package com.tuuzed.tunnelcli;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TunnelManifest {

    private static final Map<String, String> manifest = new LinkedHashMap<>();

    static {
        URL url = TunnelManifest.class.getResource("/META-INF/MANIFEST.MF");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf(":");
                if (index != -1) {
                    String k = line.substring(0, index).trim();
                    String v = line.substring(index + 1).trim();
                    manifest.put(k, v);
                }
            }
        } catch (IOException e) {
            // pass
        }
    }

    @Nullable
    public static String versionCode() {
        return manifest.get("Tunnel-VersionCode");
    }

    @Nullable
    public static String versionName() {
        return manifest.get("Tunnel-VersionName");
    }

    @Nullable
    public static String lastCommitSHA() {
        return manifest.get("Tunnel-LastCommitSHA");
    }

    @Nullable
    public static String lastCommitDate() {
        return manifest.get("Tunnel-LastCommitDate");
    }

    @Nullable
    public static String buildDate() {
        return manifest.get("Tunnel-BuildDate");
    }
}
