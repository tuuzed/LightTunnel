package lighttunnel.cmd

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class CmdLineOption(
    val name: String,
    val longName: String = "",
    val help: String = "",
    // 排序，值越小排在越前面
    val order: Int = 0,
    // 排除的枚举对象
    val excludeEnums: Array<String> = []
)
