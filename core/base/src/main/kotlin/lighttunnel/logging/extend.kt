@file:JvmName("-ExtendKt")
@file:Suppress("unused")

package lighttunnel.logging

inline fun <reified T> T.loggerDelegate() = lazy { LoggerFactory.getLogger(T::class.java) }
