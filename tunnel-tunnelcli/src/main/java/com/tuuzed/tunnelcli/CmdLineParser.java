package com.tuuzed.tunnelcli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 命令行解析器
 * 支持的数据类型有: boolean, byte, char, short, int, long, float, double,
 * *              String,
 * *              Map<String,K>,默认使用'&'和'='分隔，例如，k1=v1&k2=v2
 * *              List<T>, 默认使用','分隔，例如，123,456
 */
public final class CmdLineParser {

    private static final String LIST_DELIMITER = ",";
    private static final String MAP_DELIMITER = "&";
    private static final String MAP_KV_DELIMITER = "=";

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

    public static <T> void printHelp(@NotNull T t, @NotNull OutputStream out, @NotNull String prefix) throws Exception {
        List<Item> items = getOptionItems(t);
        PrintStream os = (out instanceof PrintStream)
            ? (PrintStream) out
            : new PrintStream(out, true);
        int nameLength = 0;
        int longNamesLength = 0;
        int typeLength = 0;
        for (Item item : items) {
            nameLength = Math.max(item.name.length(), nameLength);
            longNamesLength = Math.max(item.longName.length(), longNamesLength);
            typeLength = Math.max(item.typeName.length(), typeLength);
        }
        for (Item item : items) {
            item.printHelp(os, nameLength, longNamesLength, typeLength, prefix);
        }
    }

    private static <T> List<Item> getOptionItems(T t) throws Exception {
        List<Item> items = new ArrayList<>();
        Class<?> clazz = t.getClass();
        final Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Option option = field.getAnnotation(Option.class);
            if (option != null) {
                items.add(new Item(field, option, field.get(t)));
            }
        }
        Collections.sort(items);
        return items;
    }

    private static class Item implements Comparable<Item> {
        Field field;
        String name;
        String longName;
        String help;
        Object def;

        int order;
        List<String> excludeEnums;

        private String typeName;


        Item(@NotNull Field field, @NotNull Option option, Object def) throws Exception {
            this.field = field;
            this.name = option.name();
            this.longName = option.longName();
            this.help = option.help();
            this.def = def;
            this.order = option.order();
            this.excludeEnums = Arrays.asList(option.excludeEnums());
            this.typeName = getTypeName();
        }


        void printHelp(@NotNull PrintStream out, int nameLength, int longNamesLength, int typeLength, @NotNull String prefix) throws Exception {
            StringBuilder helpText = new StringBuilder();
            // name
            helpText.append("-").append(name);
            for (int i = 0; i < nameLength - name.length(); i++) {
                helpText.append(" ");
            }
            // longName
            helpText.append(" (--").append(longName);
            for (int i = 0; i < longNamesLength - longName.length(); i++) {
                helpText.append(" ");
            }
            helpText.append(")");
            // typeName
            helpText.append("    :");
            helpText.append(typeName);
            for (int i = 0; i < typeLength - typeName.length(); i++) {
                helpText.append(" ");
            }
            // help
            helpText.append("    ");
            helpText.append(help);
            helpText.append(", default: ").append(def);
            out.printf("%s%s%n", prefix, helpText);
        }


        void apply(@NotNull Object obj, @NotNull Map<String, String> map) throws Exception {
            String name = "-" + this.name;
            String longName = "--" + this.longName;
            String value = null;
            if (map.containsKey(name)) value = map.get(name);
            if (map.containsKey(longName)) value = map.get(longName);
            if (value == null) return;
            Object typedVal = convertBasicType(field.getType(), value);
            if (typedVal != null) {
                field.set(obj, typedVal);
                return;
            }
            if (field.getType() == Map.class) { // map
                field.set(obj, convertMapType(field, value));
            } else if (field.getType() == List.class) { // list
                field.set(obj, convertListType(field, value));
            }
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        private String getTypeName() throws Exception {
            if (field.getType().isEnum()) {
                StringBuilder sb = new StringBuilder();
                sb.append("enum").append("[");
                Object[] enums = field.getType().getEnumConstants();
                Method name = field.getType().getMethod("name");
                boolean first = true;
                for (Object it : enums) {
                    if (!first) {
                        sb.append("|");
                    }
                    Object nameVal = name.invoke(it);
                    if (!excludeEnums.contains(nameVal)) {
                        sb.append(nameVal);
                        first = false;
                    }
                }
                sb.append("]");
                return sb.toString();
            } else if (field.getType() == List.class) {
                Type type = field.getGenericType();
                if (type instanceof ParameterizedType) {
                    Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                    Class<?> genericType = (Class<?>) actualTypeArguments[0];
                    return "list<" + genericType.getSimpleName().toLowerCase() + ">";
                } else {
                    return "list";
                }
            } else if (field.getType() == Map.class) {
                Type type = field.getGenericType();
                if (type instanceof ParameterizedType) {
                    Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                    Class<?> genericType = (Class<?>) actualTypeArguments[1];
                    return "map<string," + genericType.getSimpleName().toLowerCase() + ">";
                } else {
                    return "list";
                }
            } else {
                return field.getType().getSimpleName().toLowerCase();
            }
        }

        @NotNull
        private static Map convertMapType(
            @NotNull Field field,
            @NotNull String value
        ) throws Exception {
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                if (actualTypeArguments.length == 2 && actualTypeArguments[0] == String.class) {
                    Map values = new LinkedHashMap();
                    String[] kvs = value.split(MAP_DELIMITER);
                    for (String kv : kvs) {
                        String[] array = kv.split(MAP_KV_DELIMITER);
                        if (array.length == 2) {
                            String k = array[0];
                            String v = array[1];
                            Object tmpVal = convertBasicType((Class<?>) actualTypeArguments[1], v);
                            if (tmpVal != null) {
                                //noinspection unchecked
                                values.put(k, v);
                            }
                        }
                    }
                    return values;
                }
            }
            return Collections.emptyMap();
        }

        @NotNull
        private static List convertListType(
            @NotNull Field field,
            @NotNull String value
        ) throws Exception {
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                Class<?> genericType = (Class<?>) actualTypeArguments[0];
                String[] array = value.split(LIST_DELIMITER);

                List<Object> values = new ArrayList<>();
                for (String it : array) {
                    Object tmpVal = convertBasicType(genericType, it);
                    if (tmpVal != null) {
                        values.add(tmpVal);
                    }
                }
                return values;
            }
            return Collections.emptyList();
        }

        @Nullable
        private static Object convertBasicType(
            @NotNull Class<?> clazz,
            @NotNull String value
        ) throws Exception {
            if (clazz == boolean.class || clazz == Boolean.class) { // boolean
                return Boolean.parseBoolean(value);
            } else if (clazz == byte.class || clazz == Byte.class) { // byte
                return Byte.parseByte(value);
            } else if (clazz == char.class || clazz == Character.class) { // char
                return value.charAt(0);
            } else if (clazz == short.class || clazz == Short.class) { // short
                return Short.parseShort(value);
            } else if (clazz == int.class || clazz == Integer.class) { // int
                return Integer.parseInt(value);
            } else if (clazz == long.class || clazz == Long.class) { // long
                return Long.parseLong(value);
            } else if (clazz == float.class || clazz == Float.class) { // long
                return Float.parseFloat(value);
            } else if (clazz == double.class || clazz == Double.class) { // long
                return Double.parseDouble(value);
            } else if (clazz == String.class) { // string
                return value;
            } else if (clazz.isEnum()) { // enum
                Method valueOf = clazz.getMethod("valueOf", String.class);
                return valueOf.invoke(clazz, value);
            } else {
                return null;
            }
        }

        @Override
        public int compareTo(@NotNull Item o) {
            // 正序（小 -> 大）
            return Integer.compare(this.order, o.order);
        }
    }

}
