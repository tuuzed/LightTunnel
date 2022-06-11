package krp.extensions

val String.asAddr get() = this.split(":").let { it.first() to it.last().toInt() }
