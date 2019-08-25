package com.tuuzed.tunnelcli;

import org.junit.Test;

public class CmdLineParserTest {

    @Test
    public void parse() throws Exception {
        String[] args = new String[]{
            "--str", "String",
            "--int", "80",
            "--map", "k1=123&k2=abc",
            "--enum", "HTTP",
            "--list", "123,abc,aaa",
        };
        Options options = new Options();
        CmdLineParser.parse(options, args);
        System.out.println(options);
    }

    @Test
    public void printHelp() throws Exception {
        CmdLineParser.printHelp(new Options(), System.out);
    }
}