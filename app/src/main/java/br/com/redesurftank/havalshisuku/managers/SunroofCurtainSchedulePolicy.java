package br.com.redesurftank.havalshisuku.managers;

/** Resolve uma única ação da agenda, mantendo fechamento prioritário em sobreposição. */
public final class SunroofCurtainSchedulePolicy {
    private SunroofCurtainSchedulePolicy() {}

    public enum Action {
        NONE,
        OPEN,
        CLOSE
    }

    public static Action actionFor(boolean inOpen, boolean openHandled,
                                   boolean inClose, boolean closeHandled) {
        if (inClose) return closeHandled ? Action.NONE : Action.CLOSE;
        if (inOpen && !openHandled) return Action.OPEN;
        return Action.NONE;
    }
}
