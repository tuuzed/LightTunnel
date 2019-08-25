package com.tuuzed.tunnel.log4j;

import org.apache.log4j.*;
import org.jetbrains.annotations.NotNull;

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
public class Log4jInitializer {

    private Appender appender;
    private Logger logger;
    private Level level;

    private Log4jInitializer(@NotNull Logger logger, @NotNull Appender appender, @NotNull Level level) {
        this.logger = logger;
        this.appender = appender;
        this.level = level;
    }

    public static void initializeThirdLibrary(@NotNull Level level) {
        String[] names = new String[]{
            "io.netty",
            "org.yaml.snakeyaml",
            "org.slf4j",
            "org.apache.log4j",
        };
        for (String name : names) {
            Log4jInitializer.builder()
                .setLogger(LogManager.getLogger(name))
                .setLevel(level)
                .initialize();
        }
    }

    public void initialize() {
        logger.addAppender(appender);
        logger.setLevel(level);
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("Duplicates")
    public static class Builder {
        private static final String SYSTEM_OUT = "System.out";
        private static final String SYSTEM_ERR = "System.err";
        private static final String defaultConsolePattern = " %d{HH:mm:ss} [%5p] %l: %m%n";
        private static final String defaultFilePattern = "%-d{yyyy-MM-dd HH:mm:ss} [ %t:%r ] - [ %p ] %m%n";


        // =========================== common =========================== //
        private Logger logger = null;
        private boolean console = true;
        private Level level = Level.INFO;
        // =========================== PatternLayout =========================== //
        private String conversionPattern = null;
        // =========================== ConsoleAppender =========================== //
        private String target = SYSTEM_OUT;
        // =========================== RollingFileAppender =========================== //
        private String file;
        private long maxFileSize = 1024 * 1024; // 1MB
        private int maxBackupIndex = 2;
        private boolean append = true;


        Builder() {
        }

        @NotNull
        public Builder setLogger(@NotNull Logger logger) {
            this.logger = logger;
            return this;
        }

        @NotNull
        public Builder setConsole(boolean console) {
            this.console = console;
            return this;
        }

        @NotNull
        public Builder setLevel(@NotNull Level level) {
            this.level = level;
            return this;
        }

        @NotNull
        public Builder setConversionPattern(@NotNull String conversionPattern) {
            this.conversionPattern = conversionPattern;
            return this;
        }

        @NotNull
        public Builder setTarget(boolean errorStream) {
            if (errorStream) {
                this.target = SYSTEM_ERR;
            } else {
                this.target = SYSTEM_OUT;
            }
            return this;
        }

        @NotNull
        public Builder setFile(@NotNull String file) {
            this.file = file;
            return this;
        }

        @NotNull
        public Builder setMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
            return this;
        }

        @NotNull
        public Builder setMaxBackupIndex(int maxBackupIndex) {
            this.maxBackupIndex = maxBackupIndex;
            return this;
        }

        @NotNull
        public Builder setAppend(boolean append) {
            this.append = append;
            return this;
        }

        @NotNull
        public Log4jInitializer build() {
            if (logger == null) {
                logger = Logger.getRootLogger();
            }
            if (console) {
                return new Log4jInitializer(logger, createConsoleAppender(), level);
            } else {
                return new Log4jInitializer(logger, createRollingFileAppender(), level);
            }
        }

        public void initialize() {
            build().initialize();
        }


        @NotNull
        private Appender createRollingFileAppender() {
            if (maxFileSize <= 0) throw new IllegalArgumentException("maxFileSize <= 0");
            if (maxBackupIndex <= 0) throw new IllegalArgumentException("maxBackupIndex <= 0");
            RollingFileAppender appender = new RollingFileAppender();
            PatternLayout layout = new PatternLayout();
            layout.setConversionPattern((conversionPattern == null) ? defaultFilePattern : conversionPattern);
            appender.setLayout(layout);
            appender.setThreshold(level);
            appender.setEncoding("utf-8");
            appender.setFile(file);
            appender.setAppend(append);
            appender.setMaximumFileSize(maxFileSize);
            appender.setMaxBackupIndex(maxBackupIndex);
            appender.activateOptions();
            return appender;
        }

        @NotNull
        private Appender createConsoleAppender() {
            ConsoleAppender appender = new ConsoleAppender();
            PatternLayout layout = new PatternLayout();
            layout.setConversionPattern((conversionPattern == null) ? defaultConsolePattern : conversionPattern);
            appender.setLayout(layout);
            appender.setThreshold(level);
            appender.setTarget(target);
            appender.activateOptions();
            return appender;
        }
    }

}
