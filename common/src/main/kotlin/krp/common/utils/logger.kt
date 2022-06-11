@file:Suppress("unused")

package krp.common.utils

inline fun <reified T> T.injectLogger() = lazy { org.slf4j.LoggerFactory.getLogger(T::class.java)!! }
