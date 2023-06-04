package lighttunnel.httpserver

fun interface AuthProvider {
    fun invoke(username: String, password: String): Boolean
}