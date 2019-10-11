package com.tuuzed.lighttunnel.cmd

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
        val map = linkedMapOf<String, String>()
        var i = 0
        while (i < args.size) {
            map[args[i]] = args[++i]
            i++
        }
        getOptionItems(obj as Any).forEach { it.apply(obj, map) }
    }

    @Throws(Exception::class)
    fun <T> printHelp(obj: T, out: OutputStream, prefix: String) {
        val items = getOptionItems(obj as Any)
        val outTarget = if (out is PrintStream) out else PrintStream(out, true)
        var nameLength = 0
        var longNamesLength = 0
        var typeLength = 0
        items.forEach {
            nameLength = it.name.length.coerceAtLeast(nameLength)
            longNamesLength = it.longName.length.coerceAtLeast(longNamesLength)
            typeLength = it.typeName.length.coerceAtLeast(typeLength)
        }
        items.forEach { it.printHelp(outTarget, nameLength, longNamesLength, typeLength, prefix) }
    }

    @Throws(Exception::class)
    private fun getOptionItems(obj: Any): List<Item> {
        val items = ArrayList<Item>()
        val clazz = obj.javaClass
        val fields = clazz.declaredFields
        for (field in fields) {
            field.isAccessible = true
            val option = field.getAnnotation(CmdLineOption::class.java)
            if (option != null) {
                items.add(Item(field, option, field.get(obj)))
            }
        }
        items.sort()
        return items
    }

    private class Item(val field: Field, option: CmdLineOption, val def: Any) : Comparable<Item> {
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
        fun printHelp(out: PrintStream, nameLength: Int, longNamesLength: Int, typeLength: Int, prefix: String) {
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
        fun <T> apply(obj: T, map: Map<String, String>) {
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
        private fun convertListType(field: Field, value: String): List<*> {
            val type = field.genericType
            return if (type is ParameterizedType) {
                val actualTypeArguments = type.actualTypeArguments
                val genericType = actualTypeArguments[0] as Class<*>
                value.split(LIST_DELIMITER.toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .mapNotNull { convertBasicType(genericType, it) }
            } else {
                emptyList<Any>()
            }
        }

        @Throws(Exception::class)
        private fun convertBasicType(clazz: Class<*>, value: String): Any? {
            return when (clazz) {
                Boolean::class.javaPrimitiveType, Boolean::class.java -> value.toBoolean()
                Byte::class.javaPrimitiveType, Byte::class.java -> value.toByte()
                Char::class.javaPrimitiveType, Char::class.java -> value[0]
                Short::class.javaPrimitiveType, Short::class.java -> value.toShort()
                Int::class.javaPrimitiveType, Int::class.java -> value.toInt()
                Long::class.javaPrimitiveType, Long::class.java -> value.toLong()
                Float::class.javaPrimitiveType, Float::class.java -> value.toFloat()
                Double::class.javaPrimitiveType, Double::class.java -> value.toDouble()
                String::class.java -> value
                else -> {
                    if (clazz.isEnum) clazz.getMethod("valueOf", String::class.java).invoke(clazz, value)
                    else null
                }
            }
        }

        // 正序（小 -> 大）
        override fun compareTo(other: Item): Int = this.order.compareTo(other.order)
    }

}
