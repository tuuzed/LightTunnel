@file:Suppress("unused")

package lighttunnel.common.extensions

inline fun <reified T> T.injectLogger() = lazy { org.slf4j.LoggerFactory.getLogger(T::class.java)!! }
