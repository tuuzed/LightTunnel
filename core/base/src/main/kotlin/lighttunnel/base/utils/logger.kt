@file:JvmName("-LoggerKt")
@file:Suppress("unused")

package lighttunnel.base.utils

inline fun <reified T> T.loggerDelegate() = lazy { org.slf4j.LoggerFactory.getLogger(T::class.java)!! }
