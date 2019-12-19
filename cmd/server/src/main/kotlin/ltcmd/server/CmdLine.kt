package ltcmd.server

import lighttunnel.cmd.CmdLineOption

data class CmdLine(

    @CmdLineOption(name = "c", longName = "config", help = "配置文件", order = 1000)
    var iniFile: String = "lts.ini",

    @CmdLineOption(name = "h", longName = "help", help = "显示帮助信息", order = 3000)
    var help: Boolean = false

)