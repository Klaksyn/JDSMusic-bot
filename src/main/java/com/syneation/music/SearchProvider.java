package com.syneation.music;

public record SearchProvider(String prefix, String name, boolean enabled, String disabledReason) {

    public static SearchProvider enabled(String prefix, String name) {
        return new SearchProvider(prefix, name, true, null);
    }

    public static SearchProvider disabled(String prefix, String name, String reason) {
        return new SearchProvider(prefix, name, false, reason);
    }

}
