package com.tuuzed.tunnel.common.util;

import org.junit.Test;

import java.util.Map;

public class HttpUtilsTest {

    @Test
    public void headersOf() {
        Map<String, String> map = HttpUtils.headersOf("A", "1",
            "B", "2",
            "C", "3"
        );
        System.out.println(map);
    }
}