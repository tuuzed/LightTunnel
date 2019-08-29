package com.tuuzed.tunnel;

import org.jetbrains.annotations.NotNull;

public final class TunnelVersion {

    public static final String major = "0";
    public static final String minor = "1";
    public static final String build = "x";

    @NotNull
    public static String versionName() {
        return String.format("%s.%s.%s", major, minor, build);
    }

}
