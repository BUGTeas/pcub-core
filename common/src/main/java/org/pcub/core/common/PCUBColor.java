package org.pcub.core.common;

import org.jspecify.annotations.NonNull;

public record PCUBColor(int red, int blue, int green) {
    public static PCUBColor fromInt(int color) {
        return new PCUBColor(
                color >> 16 & 255,
                color >> 8 & 255,
                color & 255
        );
    }

    public int toInt() {
        return ((red * 255 + green) * 255 + blue);
    }

    @Override
    public @NonNull String toString() {
        return toInt() + "(" + red + "," + green + "," + green + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PCUBColor(int red1, int blue1, int green1))) {
            return false;
        }
        return red == red1 && green == green1 && blue == blue1;
    }
}
