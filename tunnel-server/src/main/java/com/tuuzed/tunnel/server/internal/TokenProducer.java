package com.tuuzed.tunnel.server.internal;

public final class TokenProducer {

    private volatile long tokenProducer = 0;

    public synchronized long nextToken() {
        return ++tokenProducer;
    }

}
