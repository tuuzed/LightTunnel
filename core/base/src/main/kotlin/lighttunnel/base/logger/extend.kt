@file:JvmName("-ExtendKt")
@file:Suppress("unused")

package lighttunnel.base.logger

inline fun <reified T> T.loggerDelegate() = lazy { LoggerFactory.getLogger(T::class.java) }
