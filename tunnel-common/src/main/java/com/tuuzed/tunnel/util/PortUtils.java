package com.tuuzed.tunnel.util;

import org.jetbrains.annotations.NotNull;

public final class PortUtils {

    /**
     * 判断端口是否在指定的端口规则内
     *
     * @param portRule 端口规则，例如：10000-21000,30000,30001,30003
     * @param port     端口
     * @return 判断结果
     */
    public static boolean inPortRule(@NotNull String portRule, int port) {
        if (port < 0 || port > 65535) {
            return false;
        }
        final String[] sliceArray = portRule.split(",");
        for (String slice : sliceArray) {
            int index = slice.indexOf("-");
            if (index == -1) {
                try {
                    int rulePort = Integer.parseInt(slice);
                    if (rulePort == port) {
                        return true;
                    }
                } catch (Exception e) {
                    // pass
                }
            } else {
                String[] range = slice.split("-");
                try {
                    int startRulePort = Integer.parseInt(range[0]);
                    int endRulePort = Integer.parseInt(range[1]);
                    if (port >= startRulePort && port <= endRulePort) {
                        return true;
                    }
                } catch (Exception e) {
                    // pass
                }
            }
        }
        return false;
    }
}
