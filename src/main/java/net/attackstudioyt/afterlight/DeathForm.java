package net.attackstudioyt.afterlight;

public enum DeathForm {
    TRANSPARENT,
    INVISIBLE;

    public static DeathForm parse(String s) {
        if (s == null) return null;
        try {
            return DeathForm.valueOf(s.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
