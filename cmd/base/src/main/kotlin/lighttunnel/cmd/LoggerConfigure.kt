package lighttunnel.cmd

import org.apache.log4j.*


/*
 %p     输出日志信息的优先级，即DEBUG，INFO，WARN，ERROR，FATAL。
 %d     输出日志时间点的日期或时间，默认格式为ISO8601，也可以在其后指定格式，如：%d{yyyy/MM/dd HH:mm:ss,SSS}。
 %r     输出自应用程序启动到输出该log信息耗费的毫秒数。
 %t     输出产生该日志事件的线程名。
 %l     输出日志事件的发生位置，相当于%c.%M(%F:%L)的组合，包括类全名、方法、文件名以及在代码中的行数。例如：test.TestLog4j.main(TestLog4j.java:10)
 %c     输出日志信息所属的类目，通常就是所在类的全名。
 %M     输出产生日志信息的方法名。
 %F     输出日志消息产生时所在的文件名称。
 %L     输出代码中的行号。
 %m     输出代码中指定的具体日志信息。
 %n     输出一个回车换行符，Windows平台为"/r/n"，Unix平台为"/n"。
 %x     输出和当前线程相关联的NDC(嵌套诊断环境)，尤其用到像java servlets这样的多客户多线程的应用中。
 %%     输出一个"%"字符。

 在%与格式字符之间加上修饰符来控制其最小长度、最大长度、和文本的对齐方式
 %20c   指定输出category的名称，最小的长度是20，如果category的名称长度小于20的话，默认的情况下右对齐。
 %-20c  "-"号表示左对齐。
 %.30c  指定输出category的名称，最大的长度是30，如果category的名称长度大于30的话，就会将左边多出的字符截掉，但小于30的话也不会补空格。
 */
class LoggerConfigure private constructor(
    private val level: Level = Level.INFO,
    conversionPattern: String = defaultConsolePattern,
    // console
    console: Boolean = true,
    target: String = SYSTEM_ERR,
    // file
    file: String = "log4j.log",
    maxFileSize: Long = 1024 * 1024L,
    maxBackupIndex: Int = 2,
    append: Boolean = true,
    vararg names: String
) {

    companion object {

        private const val SYSTEM_OUT = "System.out"
        private const val SYSTEM_ERR = "System.err"
        private const val defaultConsolePattern = "%-d{HH:mm:ss} [%5p] %l: %m%n"
        private const val defaultFilePattern = "%-d{yyyy-MM-dd HH:mm:ss} [ %t:%r ] - [ %p ] %m%n"

        @JvmStatic
        fun configConsole(
            level: Level = Level.INFO,
            conversionPattern: String = defaultConsolePattern,
            systemErr: Boolean = true,
            vararg names: String
        ) = LoggerConfigure(
            level = level,
            conversionPattern = conversionPattern,
            console = true,
            target = if (systemErr) SYSTEM_ERR else SYSTEM_OUT,
            names = names
        ).apply()

        @JvmStatic
        fun configFile(
            level: Level = Level.INFO,
            conversionPattern: String = defaultFilePattern,
            file: String = "log4j.log",
            maxFileSize: Long = 1024 * 1024L,
            maxBackupIndex: Int = 2,
            append: Boolean = true,
            vararg names: String
        ) = LoggerConfigure(
            level = level,
            conversionPattern = conversionPattern,
            console = false,
            file = file,
            maxFileSize = maxFileSize,
            maxBackupIndex = maxBackupIndex,
            append = append,
            names = names
        ).apply()
    }

    private val loggers: List<Logger>
    private val appender: Appender

    init {
        appender =
            if (console) createConsoleAppender(level, conversionPattern, target)
            else createRollingFileAppender(level, conversionPattern, file, maxFileSize, maxBackupIndex, append)
        loggers = if (names.isEmpty()) {
            listOf(Logger.getRootLogger())
        } else {
            names.map { Logger.getLogger(it) }
        }
    }

    private fun apply() {
        loggers.forEach {
            it.addAppender(appender)
            it.level = level
        }
    }

    private fun createConsoleAppender(
        level: Level,
        conversionPattern: String,
        target: String
    ): Appender {
        val appender = ConsoleAppender()
        val layout = PatternLayout()
        layout.conversionPattern = conversionPattern
        appender.layout = layout
        appender.threshold = level
        appender.target = target
        appender.activateOptions()
        return appender
    }

    private fun createRollingFileAppender(
        level: Level,
        conversionPattern: String,
        file: String,
        maxFileSize: Long,
        maxBackupIndex: Int,
        append: Boolean
    ): Appender {
        require(maxFileSize > 0) { "maxFileSize <= 0" }
        require(maxBackupIndex > 0) { "maxBackupIndex <= 0" }
        val appender = RollingFileAppender()
        val layout = PatternLayout()
        layout.conversionPattern = conversionPattern
        appender.layout = layout
        appender.threshold = level
        appender.encoding = "utf-8"
        appender.file = file
        appender.append = append
        appender.maximumFileSize = maxFileSize
        appender.maxBackupIndex = maxBackupIndex
        appender.activateOptions()
        return appender
    }


}
