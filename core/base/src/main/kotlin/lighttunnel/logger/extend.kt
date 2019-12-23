@file:JvmName("-ExtendKt")
@file:Suppress("unused")

package lighttunnel.logger

inline fun <reified T> T.loggerDelegate() = lazy { LoggerFactory.getLogger(T::class.java) }
