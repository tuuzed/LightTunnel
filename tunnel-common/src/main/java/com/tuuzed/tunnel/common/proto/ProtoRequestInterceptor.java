package com.tuuzed.tunnel.common.proto;

import org.jetbrains.annotations.NotNull;

public interface ProtoRequestInterceptor {
    ProtoRequestInterceptor DEFAULT = new ProtoRequestInterceptor() {
        @NotNull
        @Override
        public ProtoRequest proceed(@NotNull ProtoRequest request) throws ProtoException {
            return request;
        }
    };

    @NotNull
    ProtoRequest proceed(@NotNull ProtoRequest request) throws ProtoException;
}
