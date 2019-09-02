@file:JvmName("_Util")

package tunnel2.server.internal

/**
 * 判断端口是否在指定的端口规则内
 *
 * @param portRange 端口规则，例如：10000-21000,30000,30001,30003
 * @param port      端口
 * @return 判断结果
 */

fun hasInPortRange(portRange: String, port: Int): Boolean {
    if (port < 0 || port > 65535) {
        return false
    }
    val sliceArray = portRange.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    for (slice in sliceArray) {
        val index = slice.indexOf("-")
        if (index == -1) {
            try {
                val rulePort = Integer.parseInt(slice)
                if (rulePort == port) {
                    return true
                }
            } catch (e: Exception) {
                // pass
            }

        } else {
            val range = slice.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                val startRulePort = Integer.parseInt(range[0])
                val endRulePort = Integer.parseInt(range[1])
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