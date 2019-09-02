package tunnel2.t2cli

import org.apache.log4j.Level

enum class LogLevel(val level: Level) {

    ALL(Level.ALL),
    TRACE(Level.TRACE),
    DEBUG(Level.DEBUG),
    INFO(Level.INFO),
    WARN(Level.WARN),
    ERROR(Level.ERROR),
    OFF(Level.OFF)
}
