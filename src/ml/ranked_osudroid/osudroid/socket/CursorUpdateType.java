package ml.ranked_osudroid.osudroid.socket;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CursorUpdateType {
    PRESS("press"), MOVE("move"), RELEASE("release");

    private String type;

    public String getAsEventName() {
        return "cursor_" + type;
    }

}
