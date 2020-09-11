package io.timeywimey.jocker.model;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public enum WaitCondition {

    NOT_RUNNING("not-running"),
    NEXT_EXIT("next-exit"),
    REMOVED("removed");


    private final String value;

    WaitCondition(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
