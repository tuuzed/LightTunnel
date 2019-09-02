package tunnel2.t2cli

import java.io.OutputStream
import java.io.PrintStream
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.util.*

/**
 * 命令行解析器
 * 支持的数据类型有: boolean, byte, char, short, int, long, float, double,
 * *              String,
 * *              Map<String></String>,K>,默认使用'&'和'='分隔，例如，k1=v1&k2=v2
 * *              List<T>, 默认使用','分隔，例如，123,456
</T> */
object CmdLineParser {

    private const val LIST_DELIMITER = ","
    private const val MAP_DELIMITER = "&"
    private const val MAP_KV_DELIMITER = "="

    @Throws(Exception::class)
    fun <T> parse(obj: T, args: Array<String>) {
        val map = LinkedHashMap<String, String>()
        var i = 0
        while (i < args.size) {
            map[args[i]] = args[++i]
            i++
        }
        val items = getOptionItems(obj as Any)
        for (item in items) {
            item.apply(obj, map)
        }
    }

    @Throws(Exception::class)
    fun <T> printHelp(obj: T, out: OutputStream, prefix: String) {
        val items = getOptionItems(obj as Any)
        val os = if (out is PrintStream)
            out
        else
            PrintStream(out, true)
        var nameLength = 0
        var longNamesLength = 0
        var typeLength = 0
        for (item in items) {
            nameLength = item.name.length.coerceAtLeast(nameLength)
            longNamesLength = item.longName.length.coerceAtLeast(longNamesLength)
            typeLength = item.typeName.length.coerceAtLeast(typeLength)
        }
        for (item in items) {
            item.printHelp(os, nameLength, longNamesLength, typeLength, prefix)
        }
    }

    @Throws(Exception::class)
    private fun getOptionItems(obj: Any): List<Item> {
        val items = ArrayList<Item>()
        val clazz = obj.javaClass
        val fields = clazz.declaredFields
        for (field in fields) {
            field.isAccessible = true
            val option = field.getAnnotation(Option::class.java)
            if (option != null) {
                items.add(Item(field, option, field.get(obj)))
            }
        }
        items.sort()
        return items
    }

    private class Item(val field: Field, option: Option, val def: Any) : Comparable<Item> {
        val name: String = option.name
        val longName: String = option.longName
        val help: String = option.help
        val order: Int = option.order
        val excludeEnums: List<String> = listOf(*option.excludeEnums)
        val typeName: String

        init {
            this.typeName = obtainTypeName()
        }


        @Throws(Exception::class)
        internal fun printHelp(out: PrintStream, nameLength: Int, longNamesLength: Int, typeLength: Int, prefix: String) {
            val helpText = StringBuilder()
            // name
            helpText.append("-").append(name)
            for (i in 0 until nameLength - name.length) {
                helpText.append(" ")
            }
            // longName
            helpText.append(" (--").append(longName)
            for (i in 0 until longNamesLength - longName.length) {
                helpText.append(" ")
            }
            helpText.append(")")
            // typeName
            helpText.append("    :")
            helpText.append(typeName)
            for (i in 0 until typeLength - typeName.length) {
                helpText.append(" ")
            }
            // help
            helpText.append("    ")
            helpText.append(help)
            helpText.append(", default: ").append(def)
            out.printf("%s%s%n", prefix, helpText)
        }


        @Throws(Exception::class)
        internal fun <T> apply(obj: T, map: Map<String, String>) {
            val name = "-" + this.name
            val longName = "--" + this.longName
            var value: String? = null
            if (map.containsKey(name)) value = map[name]
            if (map.containsKey(longName)) value = map[longName]
            if (value == null) return
            val typedVal = convertBasicType(field.type, value)
            if (typedVal != null) {
                field.set(obj, typedVal)
                return
            }
            if (field.type == Map::class.java) { // map
                field.set(obj, convertMapType(field, value))
            } else if (field.type == List::class.java) { // list
                field.set(obj, convertListType(field, value))
            }
        }

        @Throws(Exception::class)
        private fun obtainTypeName(): String {
            when {
                field.type.isEnum -> {
                    val sb = StringBuilder()
                    sb.append("enum").append("[")
                    val enums = field.type.enumConstants
                    val name = field.type.getMethod("name")
                    var first = true
                    for (it in enums) {
                        if (!first) {
                            sb.append("|")
                        }
                        val nameVal = name.invoke(it)
                        if (!excludeEnums.contains(nameVal)) {
                            sb.append(nameVal)
                            first = false
                        }
                    }
                    sb.append("]")
                    return sb.toString()
                }
                field.type == List::class.java -> return when (val type = field.genericType) {
                    is ParameterizedType -> {
                        val actualTypeArguments = type.actualTypeArguments
                        val genericType = actualTypeArguments[0] as Class<*>
                        "list<" + genericType.simpleName.toLowerCase() + ">"
                    }
                    else -> "list"
                }
                field.type == Map::class.java -> return when (val type = field.genericType) {
                    is ParameterizedType -> {
                        val actualTypeArguments = type.actualTypeArguments
                        val genericType = actualTypeArguments[1] as Class<*>
                        "map<string," + genericType.simpleName.toLowerCase() + ">"
                    }
                    else -> "list"
                }
                else -> return field.type.simpleName.toLowerCase()
            }
        }

        @Throws(Exception::class)
        private fun convertMapType(
            field: Field,
            value: String
        ): Map<*, *> {
            val type = field.genericType
            if (type is ParameterizedType) {
                val actualTypeArguments = type.actualTypeArguments
                if (actualTypeArguments.size == 2 && actualTypeArguments[0] === String::class.java) {
                    val values = linkedMapOf<String, String>()
                    val kvs = value.split(MAP_DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (kv in kvs) {
                        val array = kv.split(MAP_KV_DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (array.size == 2) {
                            val k = array[0]
                            val v = array[1]
                            val tmpVal = convertBasicType(actualTypeArguments[1] as Class<*>, v)
                            if (tmpVal != null) {
                                values[k] = v
                            }
                        }
                    }
                    return values
                }
            }
            return emptyMap<Any, Any>()
        }

        @Throws(Exception::class)
        private fun convertListType(
            field: Field,
            value: String
        ): List<*> {
            val type = field.genericType
            if (type is ParameterizedType) {
                val actualTypeArguments = type.actualTypeArguments
                val genericType = actualTypeArguments[0] as Class<*>
                val array = value.split(LIST_DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                val values = ArrayList<Any>()
                for (it in array) {
                    val tmpVal = convertBasicType(genericType, it)
                    if (tmpVal != null) {
                        values.add(tmpVal)
                    }
                }
                return values
            }
            return emptyList<Any>()
        }

        @Throws(Exception::class)
        private fun convertBasicType(
            clazz: Class<*>,
            value: String
        ): Any? {
            if (clazz == Boolean::class.javaPrimitiveType || clazz == Boolean::class.java) { // boolean
                return java.lang.Boolean.parseBoolean(value)
            } else if (clazz == Byte::class.javaPrimitiveType || clazz == Byte::class.java) { // byte
                return java.lang.Byte.parseByte(value)
            } else if (clazz == Char::class.javaPrimitiveType || clazz == Char::class.java) { // char
                return value[0]
            } else if (clazz == Short::class.javaPrimitiveType || clazz == Short::class.java) { // short
                return java.lang.Short.parseShort(value)
            } else if (clazz == Int::class.javaPrimitiveType || clazz == Int::class.java) { // int
                return Integer.parseInt(value)
            } else if (clazz == Long::class.javaPrimitiveType || clazz == Long::class.java) { // long
                return java.lang.Long.parseLong(value)
            } else if (clazz == Float::class.javaPrimitiveType || clazz == Float::class.java) { // long
                return java.lang.Float.parseFloat(value)
            } else if (clazz == Double::class.javaPrimitiveType || clazz == Double::class.java) { // long
                return java.lang.Double.parseDouble(value)
            } else if (clazz == String::class.java) { // string
                return value
            } else if (clazz.isEnum) { // enum
                val valueOf = clazz.getMethod("valueOf", String::class.java)
                return valueOf.invoke(clazz, value)
            } else {
                return null
            }
        }

        override fun compareTo(other: Item): Int {
            // 正序（小 -> 大）
            return this.order.compareTo(other.order)
        }
    }

}
