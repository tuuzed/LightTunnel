package lighttunnel.base.utils

import java.net.ServerSocket
import java.util.*


object PortUtils {
    private val random = Random()

    /**
     * 判断端口是否在指定的端口规则内
     *
     * @param portRange 端口规则，例如：10000-21000,30000,30001,30003
     * @param port      端口
     * @return 判断结果
     */
    fun hasInPortRange(portRange: String, port: Int): Boolean {
        if (port !in 0..65535) {
            return false
        }
        val rules = portRange.split(",".toRegex()).dropLastWhile { it.isEmpty() }
        for (rule in rules) {
            val index = rule.indexOf("-")
            if (index == -1) {
                try {
                    if (rule.toInt() == port) {
                        return true
                    }
                } catch (e: Exception) {
                    // pass
                }
            } else {
                val range = rule.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                try {
                    val startRulePort = range[0].toInt()
                    val endRulePort = range[1].toInt()
                    if (port in startRulePort..endRulePort) {
                        return true
                    }
                } catch (e: Exception) {
                    // pass
                }
            }
        }
        return false
    }

    /**
     * 获取指定的端口规则可用端口
     *
     * @param portRange 端口规则，例如：10000-21000,30000,30001,30003
     * @return 端口号
     */
    @Synchronized
    fun getAvailableTcpPort(portRange: String): Int {
        while (true) {
            val port = random.nextInt(65535 - 1024) + 1024
            if (hasInPortRange(portRange, port)) {
                if (isAvailablePort(port)) {
                    return port
                }
            }
        }
    }

    @Synchronized
    fun isAvailablePort(port: Int): Boolean {
        return kotlin.runCatching { ServerSocket(port).use {} }.isSuccess
    }
}
