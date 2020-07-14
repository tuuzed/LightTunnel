package lighttunnel.base.proto


internal object ProtoConsts {
    /** 空字节数组 */
    val emptyBytes = ByteArray(0)

    /** 消息帧域长度 */
    const val PROTO_MESSAGE_LENGTH_FIELD_LENGTH = 4

    /** 命令长度 */
    const val PROTO_MESSAGE_TYPE_LENGTH = 1

    /** head 长度域长度 */
    const val PROTO_MESSAGE_HEAD_LENGTH_FIELD_LENGTH = 4
}
