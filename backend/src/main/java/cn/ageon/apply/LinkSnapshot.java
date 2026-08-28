package cn.ageon.apply;

/**
 * 链接元信息抓取结果。抓取失败绝不抛异常，统一以 {@code error} 描述原因。
 */
public record LinkSnapshot(String title, String logoUrl, String error) {

    public static LinkSnapshot ok(String title, String logoUrl) {
        return new LinkSnapshot(title, logoUrl, null);
    }

    public static LinkSnapshot failed(String error) {
        return new LinkSnapshot(null, null, error);
    }

    public boolean isSuccessful() {
        return error == null;
    }
}
