package cn.ageon.apply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网申链接元信息抓取：基于 java.net.http，手动处理重定向并逐跳做 SSRF 防护。
 *
 * <p>任何情况下都不抛异常，失败统一返回 {@link LinkSnapshot#failed(String)}。
 * 防护要点：
 * <ul>
 *     <li>协议仅限 http/https；</li>
 *     <li>DNS 解析后拒绝回环/任意地址/链路本地/站点本地（10./172.16-31./192.168./169.254.）与多播；</li>
 *     <li>手动跟随重定向 ≤ 3 跳，每一跳都重新做地址校验；</li>
 *     <li>连接 5s / 请求 8s 超时，正文上限 512KiB；</li>
 *     <li>仅解析 text/html。</li>
 * </ul>
 */
@Component
public class HttpLinkSnapshotService implements LinkSnapshotService {
    private static final Logger log = LoggerFactory.getLogger(HttpLinkSnapshotService.class);

    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_BODY_BYTES = 512 * 1024;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_LOGO_LENGTH = 500;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private static final Pattern TAG_OPEN_PATTERN = Pattern.compile("(?is)<(meta|link)\\b[^>]*>");
    private static final Pattern ATTR_PATTERN =
            Pattern.compile("(?i)([a-zA-Z][-a-zA-Z0-9_:]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    private static final Pattern TITLE_TAG_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");

    private final HttpClient httpClient;
    private final Semaphore semaphore = new Semaphore(4);

    public HttpLinkSnapshotService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public LinkSnapshot snapshot(String rawUrl) {
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(2, TimeUnit.SECONDS);
            if (!acquired) {
                return LinkSnapshot.failed("抓取服务繁忙，请稍后再试");
            }
            return doSnapshot(rawUrl);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return LinkSnapshot.failed("抓取被中断");
        } catch (Exception exception) {
            // 兜底：任何未预期异常都转为失败结果，保证调用方永不收到异常
            log.debug("link snapshot unexpected failure", exception);
            return LinkSnapshot.failed("抓取失败");
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    private LinkSnapshot doSnapshot(String rawUrl) {
        String normalized = normalizeUrl(rawUrl);
        if (normalized == null) {
            return LinkSnapshot.failed("链接格式不合法");
        }
        URI currentUri;
        try {
            currentUri = new URI(normalized);
        } catch (URISyntaxException exception) {
            return LinkSnapshot.failed("链接格式不合法");
        }

        String html = null;
        URI pageUri = currentUri;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            LinkSnapshot blockResult = rejectUnsafe(currentUri);
            if (blockResult != null) {
                return blockResult;
            }
            HttpResponse<InputStream> response;
            try {
                HttpRequest request = HttpRequest.newBuilder(currentUri)
                        .timeout(REQUEST_TIMEOUT)
                        .header("User-Agent", "Mozilla/5.0 (compatible; ageon-link-snapshot/1.0)")
                        .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                        .GET()
                        .build();
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (IOException exception) {
                return LinkSnapshot.failed("无法连接该页面");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return LinkSnapshot.failed("抓取被中断");
            }

            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                Optional<String> location = response.headers().firstValue("Location");
                if (location.isEmpty()) {
                    return LinkSnapshot.failed("重定向缺少 Location");
                }
                closeQuietly(response.body());
                try {
                    currentUri = currentUri.resolve(location.get().trim());
                } catch (IllegalArgumentException exception) {
                    return LinkSnapshot.failed("重定向地址不合法");
                }
                continue;
            }
            if (status < 200 || status >= 300) {
                closeQuietly(response.body());
                return LinkSnapshot.failed("页面返回状态码 " + status);
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
                closeQuietly(response.body());
                return LinkSnapshot.failed("该链接不是 HTML 页面");
            }
            Charset charset = parseCharset(contentType);
            try {
                html = readBody(response.body(), charset);
            } catch (IOException exception) {
                return LinkSnapshot.failed("读取页面内容失败");
            }
            pageUri = currentUri;
            break;
        }

        if (html == null) {
            return LinkSnapshot.failed("重定向次数过多");
        }
        return LinkSnapshot.ok(extractTitle(html), extractLogo(html, pageUri));
    }

    private String readBody(InputStream body, Charset charset) throws IOException {
        try (body) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while ((read = body.read(chunk)) != -1) {
                total += read;
                if (total > MAX_BODY_BYTES) {
                    throw new IOException("body too large");
                }
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), charset);
        }
    }

    /** 返回 null 表示安全；否则返回对应的失败结果。 */
    private LinkSnapshot rejectUnsafe(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return LinkSnapshot.failed("仅支持 http/https 链接");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return LinkSnapshot.failed("链接缺少主机名");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException exception) {
            return LinkSnapshot.failed("无法解析域名");
        }
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress()
                    || address.isAnyLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                return LinkSnapshot.failed("拒绝访问内网或保留地址");
            }
        }
        return null;
    }

    private static String normalizeUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith("http://")
                && !trimmed.toLowerCase(Locale.ROOT).startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        return trimmed;
    }

    private static String extractTitle(String html) {
        String ogTitle = metaContent(html, "og:title");
        if (ogTitle != null) {
            return truncate(cleanText(ogTitle));
        }
        String twitterTitle = metaContent(html, "twitter:title");
        if (twitterTitle != null) {
            return truncate(cleanText(twitterTitle));
        }
        Matcher matcher = TITLE_TAG_PATTERN.matcher(html);
        if (matcher.find()) {
            return truncate(cleanText(matcher.group(1)));
        }
        return null;
    }

    private static String extractLogo(String html, URI pageUri) {
        String ogImage = metaContent(html, "og:image");
        String resolved = firstNonNull(ogImage, linkHref(html, "apple-touch-icon"), linkHref(html, "icon"));
        if (resolved == null || resolved.isBlank()) {
            return null;
        }
        return truncate(absoluteUrl(resolved, pageUri), MAX_LOGO_LENGTH);
    }

    private static String metaContent(String html, String key) {
        Matcher tagMatcher = TAG_OPEN_PATTERN.matcher(html);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group();
            if (!tag.toLowerCase(Locale.ROOT).startsWith("<meta")) {
                continue;
            }
            Map<String, String> attributes = parseAttributes(tag);
            String property = firstNonNull(attributes.get("property"), attributes.get("name"));
            String content = attributes.get("content");
            if (property != null && property.equalsIgnoreCase(key) && content != null && !content.isBlank()) {
                return content;
            }
        }
        return null;
    }

    private static String linkHref(String html, String rel) {
        Matcher tagMatcher = TAG_OPEN_PATTERN.matcher(html);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group();
            if (!tag.toLowerCase(Locale.ROOT).startsWith("<link")) {
                continue;
            }
            Map<String, String> attributes = parseAttributes(tag);
            String tagRel = attributes.get("rel");
            String href = attributes.get("href");
            if (tagRel != null && href != null && !href.isBlank() && tagRel.toLowerCase(Locale.ROOT).contains(rel)) {
                return href;
            }
        }
        return null;
    }

    private static Map<String, String> parseAttributes(String tag) {
        Map<String, String> attributes = new HashMap<>();
        Matcher attrMatcher = ATTR_PATTERN.matcher(tag);
        while (attrMatcher.find()) {
            String name = attrMatcher.group(1).toLowerCase(Locale.ROOT);
            String value = attrMatcher.group(2) != null ? attrMatcher.group(2) : attrMatcher.group(3);
            attributes.put(name, value);
        }
        return attributes;
    }

    private static String absoluteUrl(String href, URI pageUri) {
        try {
            return pageUri.resolve(href.trim()).toString();
        } catch (IllegalArgumentException exception) {
            return href.trim();
        }
    }

    private static Charset parseCharset(String contentType) {
        Matcher matcher = Pattern.compile("(?i)charset\\s*=\\s*([\\w-]+)").matcher(contentType);
        if (matcher.find()) {
            try {
                return Charset.forName(matcher.group(1));
            } catch (Exception ignored) {
                return StandardCharsets.UTF_8;
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static String cleanText(String value) {
        String stripped = value.replaceAll("<[^>]+>", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return stripped.isEmpty() ? null : stripped;
    }

    private static String truncate(String value) {
        return truncate(value, MAX_TITLE_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String firstNonNull(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String firstNonNull(String first, String second, String third) {
        return firstNonNull(first, firstNonNull(second, third));
    }

    private static void closeQuietly(InputStream body) {
        if (body == null) {
            return;
        }
        try {
            body.close();
        } catch (IOException ignored) {
            // 忽略
        }
    }
}