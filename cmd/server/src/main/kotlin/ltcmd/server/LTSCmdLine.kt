package ltcmd.server

import lighttunnel.cmd.CmdLineOption

data class LTSCmdLine(

    @CmdLineOption(name = "c", longName = "config", help = "配置文件", order = 1000)
    var configFile: String = "tps.yml",

    @CmdLineOption(name = "y", longName = "yaml", help = "配置文本", order = 2000)
    var yaml: String = "",

    @CmdLineOption(name = "h", longName = "help", help = "显示帮助信息", order = 3000)
    var help: Boolean = false

)