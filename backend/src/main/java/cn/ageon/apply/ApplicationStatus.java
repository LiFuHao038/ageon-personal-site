package cn.ageon.apply;

import java.util.ArrayList;
import java.util.List;

/**
 * 投递状态机。
 *
 * <p>{@code order} 表示漏斗推进次序，{@code -1} 表示非漏斗状态（终局类）。
 * 允许沿漏斗向前跳跃（例如跳过笔试直接面试），不允许后退，也不允许在终局状态上继续流转
 * （唯一例外：拿到 offer 后仍可标记为已撤回）。
 */
public enum ApplicationStatus {
    PREPARING("准备投递", 0, false),
    APPLIED("已投递", 1, false),
    WRITTEN_TEST("笔试/测评", 2, false),
    INTERVIEW_1("一面", 3, false),
    INTERVIEW_2("二面", 4, false),
    INTERVIEW_FINAL("终面/HR面", 5, false),
    OFFER("已获得 offer", 6, true),
    REJECTED("未通过", -1, true),
    WITHDRAWN("已撤回", -1, true);

    /** 漏斗视图参与统计的状态，顺序即展示顺序。 */
    public static final List<ApplicationStatus> FUNNEL_ORDER = List.of(
            PREPARING, APPLIED, WRITTEN_TEST, INTERVIEW_1, INTERVIEW_2, INTERVIEW_FINAL, OFFER);

    /** 进入该状态即视为「企业已回应」：收到笔试或更靠后的任何进展。 */
    private static final List<ApplicationStatus> RESPONDED_OR_LATER = List.of(
            WRITTEN_TEST, INTERVIEW_1, INTERVIEW_2, INTERVIEW_FINAL, OFFER);

    private final String label;
    private final int order;
    private final boolean terminal;

    ApplicationStatus(String label, int order, boolean terminal) {
        this.label = label;
        this.order = order;
        this.terminal = terminal;
    }

    public String label() {
        return label;
    }

    public int order() {
        return order;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isFunnel() {
        return order >= 0;
    }

    public boolean countsAsResponse() {
        return RESPONDED_OR_LATER.contains(this);
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        if (target == null || target == this) {
            return false;
        }
        if (this == OFFER) {
            return target == WITHDRAWN;
        }
        if (terminal) {
            return false;
        }
        if (target == REJECTED || target == WITHDRAWN) {
            return true;
        }
        return target.isFunnel() && target.order > this.order;
    }

    /** 供前端渲染推进按钮，避免在 TypeScript 里重复实现一遍状态机。 */
    public List<ApplicationStatus> allowedTransitions() {
        List<ApplicationStatus> allowed = new ArrayList<>();
        for (ApplicationStatus candidate : values()) {
            if (canTransitionTo(candidate)) {
                allowed.add(candidate);
            }
        }
        return allowed;
    }
}
