@file:JvmName("-IpKt")

package krp.extensions

import java.net.Inet4Address
import java.net.NetworkInterface

val localIpV4: String?
    get() = runCatching {
        val allNetInterfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        for (netInterface in allNetInterfaces) {
            netInterface ?: continue
            if (netInterface.isLoopback || netInterface.isVirtual || !netInterface.isUp) continue
            val addresses = netInterface.inetAddresses ?: continue
            for (address in addresses) {
                address ?: continue
                if (address is Inet4Address) {
                    return address.hostAddress
                }
            }
        }
        return null
    }.getOrNull()
