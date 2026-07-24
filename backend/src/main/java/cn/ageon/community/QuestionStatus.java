package cn.ageon.community;

public enum QuestionStatus {
    WAITING("待回复"),
    DISCUSSING("讨论中"),
    ANSWERED("已回复");

    private final String label;

    QuestionStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
