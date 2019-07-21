package com.tuuzed.tunnel.server.stat;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatItems {

    private static class InstanceHolder {
        private static final StatItems instance = new StatItems();
    }

    @NotNull
    public static StatItems getInstance() {
        return InstanceHolder.instance;
    }

    @NotNull
    private final Map<Integer, StatItem> items = new ConcurrentHashMap<>();

    @NotNull
    public StatItem getItem(int port) {
        StatItem item = items.get(port);
        if (item == null) {
            item = new StatItem(port);
            items.put(port, item);
        }
        return item;
    }

    public void removeAll() {
        items.clear();
    }

    public void remove(int port) {
        items.remove(port);
    }

}
