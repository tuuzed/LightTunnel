package com.tuuzed.tunnelcli;

import com.tuuzed.tunnel.proto.Proto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Options {
    @Option(name = "s", longName = "str", help = "String Option", order = 1)
    public String string = "127.0.0.1";

    @Option(name = "i", longName = "int", help = "Int Option", order = 2)
    public int intVal = 5000;


    @Option(name = "e", longName = "enum", help = "Enum Option", order = 4, excludeEnums = {"UNKNOWN"})
    public Proto proto = Proto.TCP;

    @Option(name = "l", longName = "list", help = "List Option", order = 5)
    public List<String> list = Collections.emptyList();

    @Option(name = "m", longName = "map", help = "Map Option", order = 3)
    public Map<String, String> setHeaders = Collections.emptyMap();

    @Override
    public String toString() {
        return "Options{" +
            "string='" + string + '\'' +
            ", intVal=" + intVal +
            ", setHeaders=" + setHeaders +
            ", proto=" + proto +
            ", list=" + list +
            '}';
    }
}
