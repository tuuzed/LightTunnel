package com.tuuzed.tunnel.server.stat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StatItem {
    private final int port;
    private AtomicLong readBytes = new AtomicLong(0);
    private AtomicLong writeBytes = new AtomicLong(0);
    private AtomicLong readMsgs = new AtomicLong(0);
    private AtomicLong writeMsgs = new AtomicLong(0);
    private AtomicInteger channels = new AtomicInteger(0);
    private long timestamp = 0;

    StatItem(int port) {
        this.port = port;
    }

    public void incrementReadBytes(int count) {
        readBytes.addAndGet(count);
        timestamp = System.currentTimeMillis();
    }

    public void incrementWriteBytes(int count) {
        writeBytes.addAndGet(count);
        timestamp = System.currentTimeMillis();
    }

    public void incrementReadMsgs(int count) {
        readMsgs.addAndGet(count);
        timestamp = System.currentTimeMillis();
    }

    public void incrementWriteMsgs(int count) {
        writeMsgs.addAndGet(count);
        timestamp = System.currentTimeMillis();
    }

    public void incrementChannels() {
        channels.incrementAndGet();
        timestamp = System.currentTimeMillis();
    }

    public void decrementChannels() {
        timestamp = System.currentTimeMillis();
        channels.decrementAndGet();
    }

    public void resetReadStat() {
        readBytes.set(0);
        readMsgs.set(0);
    }

    public void resetWriteStat() {
        readMsgs.set(0);
        writeMsgs.set(0);
    }

    public int getPort() {
        return port;
    }

    public long getReadBytes() {
        return readBytes.get();
    }

    public long getWriteBytes() {
        return writeBytes.get();
    }

    public long getReadMsgs() {
        return readMsgs.get();
    }

    public long getWriteMsgs() {
        return writeMsgs.get();
    }

    public int getChannels() {
        return channels.get();
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "StatItem{" +
                "port=" + port +
                ", readBytes=" + readBytes +
                ", writeBytes=" + writeBytes +
                ", readMsgs=" + readMsgs +
                ", writeMsgs=" + writeMsgs +
                ", channels=" + channels +
                ", timestamp=" + timestamp +
                '}';
    }
}
