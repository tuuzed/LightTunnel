package com.tuuzed.tunnel.cli.common.cmd;

import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;

public class CmdLineParser {

    public static <T> void parse(@NotNull T t, @NotNull String[] args) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            map.put(args[i], args[++i]);
        }
        List<Item> items = getOptionItems(t);
        for (Item item : items) {
            item.apply(t, map);
        }
    }

    public static <T> void printHelp(@NotNull T t, @NotNull OutputStream out) throws Exception {
        List<Item> items = getOptionItems(t);
        PrintStream os = (out instanceof PrintStream)
            ? (PrintStream) out
            : new PrintStream(out, true);
        for (Item item : items) {
            item.printHelp(os);
        }
    }


    private static <T> List<Item> getOptionItems(T t) throws Exception {
        List<Item> items = new ArrayList<>();
        Class<?> clazz = t.getClass();
        final Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Option option = field.getAnnotation(Option.class);
            if (option != null) {
                items.add(new Item(
                    field,
                    option.name(),
                    option.longName(),
                    field.getType(),
                    option.help(),
                    field.get(t),
                    option.order())
                );
            }
        }
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                // 倒序
                return Integer.compare(o1.order, o2.order);
            }
        });
        return items;
    }

    private static class Item {
        Field field;
        String name;
        String longName;
        String help;
        Object def;

        Class<?> type;
        int order;

        public Item(Field field, String name, String longName, Class<?> type, String help, Object def, int order) {
            this.field = field;
            this.name = name;
            this.longName = longName;
            this.type = type;
            this.help = help;
            this.def = def;
            this.order = order;
        }

        void printHelp(@NotNull PrintStream out) throws Exception {
            out.print("-");
            out.print(name);
            out.print("(--");
            out.print(longName);
            out.print(")\t: ");
            out.print("<");
            out.print(type.getSimpleName().toLowerCase());
            out.print("> ");
            out.print(help);
            out.print(", default: ");
            out.println(def);
        }

        void apply(@NotNull Object obj, @NotNull Map<String, String> map) throws Exception {
            String name = "-" + this.name;
            String longName = "--" + this.longName;
            String value = null;
            if (map.containsKey(name)) value = map.get(name);
            if (map.containsKey(longName)) value = map.get(longName);
            if (value == null) return;
            if (field.getType() == int.class || field.getType() == Integer.class) {
                field.set(obj, Integer.parseInt(value));
            } else if (field.getType() == String.class) {
                field.set(obj, value);
            }
        }
    }

}
