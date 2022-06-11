import com.android.tools.r8.R8
import java.io.File

object Compiler {

    fun r8Jar(inJar: File, outJar: File, rules: File) {
        val argv = arrayOf(
            "--release",
            "--classfile",
            "--output", outJar.toString(),
            "--pg-conf", rules.toString(),
            "--lib", System.getProperties()["java.home"].toString(),
            inJar.toString(),
        )
        R8.main(argv)
    }

}
