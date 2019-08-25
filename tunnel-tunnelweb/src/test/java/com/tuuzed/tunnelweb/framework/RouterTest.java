package com.tuuzed.tunnelweb.framework;

import org.junit.Test;

public class RouterTest {


    @Test
    public void testUri() {
        String uri = "/login";
        String uriWithQuery = "/login/?k=v";

        System.out.println(uri.indexOf('?'));
        System.out.println(uriWithQuery.indexOf('?'));
        System.out.println(uriWithQuery.substring(0, uriWithQuery.indexOf('?')));
        System.out.println(uriWithQuery.substring(uriWithQuery.indexOf('?') + 1));

    }


}