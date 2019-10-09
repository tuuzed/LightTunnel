package cmdclient

import cmdcommon.CmdLineOption

class TPCCmdLine {

    @CmdLineOption(name = "c", longName = "config", help = "配置文件", order = 1000)
    var configFile = "tpc.yml"

    @CmdLineOption(name = "y", longName = "yaml", help = "配置文本", order = 2000)
    var yaml = ""

    @CmdLineOption(name = "h", longName = "help", help = "显示帮助信息", order = 3000)
    var help = false

}