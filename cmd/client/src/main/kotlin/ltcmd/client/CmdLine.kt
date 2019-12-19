package ltcmd.client

import lighttunnel.cmd.CmdLineOption

class CmdLine {

    @CmdLineOption(name = "c", longName = "config", help = "配置文件", order = 1000)
    var iniFile = "ltc.ini"

    @CmdLineOption(name = "h", longName = "help", help = "显示帮助信息", order = 2000)
    var help = false

}