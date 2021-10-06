@file:JvmName("-ConstsKt")

package lighttunnel.base.proto

/** 空字节数组 */
val emptyBytes = ByteArray(0)

/** 协议版本 */
internal const val PROTO_VERSION = 0x01.toByte()

/** 消息帧域长度 */
internal const val PROTO_MESSAGE_LENGTH_FIELD_LENGTH = 4

/** 命令长度 */
internal const val PROTO_MESSAGE_TYPE_LENGTH = 1

/** head 长度域长度 */
internal const val PROTO_MESSAGE_HEAD_LENGTH_FIELD_LENGTH = 4
