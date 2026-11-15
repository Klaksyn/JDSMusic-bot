package com.syneation.helper;

import java.awt.*;

public enum Colors {
    PURPLE(new Color(148, 0, 211)),
    RED(new Color(255, 0, 0)),
    BLUE(new Color(0, 0, 255));

    private final Color color;

    Colors(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}
