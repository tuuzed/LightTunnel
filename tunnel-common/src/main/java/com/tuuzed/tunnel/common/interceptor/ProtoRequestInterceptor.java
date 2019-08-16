package com.tuuzed.tunnel.common.interceptor;

import com.tuuzed.tunnel.common.proto.ProtoException;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import org.jetbrains.annotations.NotNull;

public interface ProtoRequestInterceptor {
    ProtoRequestInterceptor DEFAULT = new ProtoRequestInterceptor() {
        @NotNull
        @Override
        public ProtoRequest handleProtoRequest(@NotNull ProtoRequest request) throws ProtoException {
            return request;
        }
    };


    @NotNull
    ProtoRequest handleProtoRequest(@NotNull ProtoRequest request) throws ProtoException;

}
