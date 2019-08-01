package com.tuuzed.tunnel.common.proto;

import org.jetbrains.annotations.NotNull;

public enum Proto {
    UNKNOWN((byte) 0x00),
    TCP((byte) 0x01),
    HTTP((byte) 0x02),
    ;

    private byte value;

    Proto(byte value) {
        this.value = value;
    }

    public byte value() {
        return value;
    }

    @NotNull
    public static Proto of(byte value) {
        final Proto[] values = values();
        for (Proto type : values) {
            if (type.value == value) {
                return type;
            }
        }
        return Proto.UNKNOWN;
    }
}