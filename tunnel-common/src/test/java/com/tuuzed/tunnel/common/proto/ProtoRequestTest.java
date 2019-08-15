package com.tuuzed.tunnel.common.proto;

import org.junit.Test;

import java.util.Arrays;

public class ProtoRequestTest {


    @Test
    public void test() throws Exception {
        ProtoRequest http = ProtoRequest.httpBuilder("t1")
                .setLocalAddr("127.0.0.1")
                .setLocalPort(80)
                .setOption("token", "tk123456")
                .build();
        System.out.println(http);
        System.out.println(http.toBytes().length);
        System.out.println(Arrays.toString(http.toBytes()));

        ProtoRequest http2 = ProtoRequest.fromBytes(http.toBytes());
        System.out.println(http2);
        System.out.println(http2.toBytes().length);
        System.out.println(Arrays.toString(http2.toBytes()));


        ProtoRequest tcp = ProtoRequest.tcpBuilder(10080)
                .setLocalAddr("127.0.0.1")
                .setLocalPort(80)
                .setOption("token", "tk123456")
                .build();
        System.out.println(tcp);
        System.out.println(tcp.toBytes().length);
        System.out.println(Arrays.toString(tcp.toBytes()));

        ProtoRequest tcp2 = ProtoRequest.fromBytes(tcp.toBytes());
        System.out.println(tcp2);
        System.out.println(tcp2.toBytes().length);
        System.out.println(Arrays.toString(tcp2.toBytes()));

    }
}