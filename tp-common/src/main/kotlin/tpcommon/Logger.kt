package tpcommon

@Suppress("unused")
inline fun <reified T : Any> T.logger() = lazy { LoggerFactory.getLogger(T::class.java) }
