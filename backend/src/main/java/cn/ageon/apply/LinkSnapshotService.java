package cn.ageon.apply;

/**
 * 抓取网申链接的元信息（页面标题、logo）供表单回填与卡片展示。
 * 实现类必须做 SSRF 防护：拒绝内网/回环/链路本地地址，限制重定向、超时与响应体大小。
 */
public interface LinkSnapshotService {

    /** 永不抛异常；失败时返回 {@link LinkSnapshot#failed(String)}。 */
    LinkSnapshot snapshot(String url);
}
