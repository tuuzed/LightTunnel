package com.tuuzed.tunnel.proto;

import org.jetbrains.annotations.NotNull;

public class ProtoException extends Exception {

    public ProtoException(@NotNull String message) {
        super(message);
    }

    public ProtoException(@NotNull String message, Throwable cause) {
        super(message, cause);
    }

}
