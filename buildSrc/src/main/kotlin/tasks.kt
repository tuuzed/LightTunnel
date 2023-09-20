import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import java.io.File

open class FatJarTask : Jar() {
    init {
        group = "lighttunnel"
    }
}

open class R8JarTask : DefaultTask() {
    init {
        group = "lighttunnel"
    }

    @TaskAction
    fun entry() {
        val inJar = File(project.buildDir, "libs/${project.name}-${project.version}.jar")
        val outJar = File(project.buildDir, "libs/${project.name}-${project.version}-r8.jar")
        val rules = project.file("r8-rules.txt")
        Compiler.r8Jar(inJar, outJar, rules)
    }
}

open class BinaryJarTask : DefaultTask() {
    init {
        group = "lighttunnel"
    }

    @TaskAction
    fun entry() {
        val stub = project.rootProject.file(".scaffold/stub.sh")
        val r8Jar = File(project.buildDir, "libs/${project.name}-${project.version}-r8.jar")
        val binaryJar = File(project.buildDir, "libs/${project.name}-${project.version}-binary.jar")
        stub.copyTo(binaryJar, overwrite = true)
        r8Jar.forEachBlock { buffer, bytesRead ->
            binaryJar.appendBytes(if (bytesRead == buffer.size) buffer else buffer.copyOfRange(0, bytesRead))
        }
        binaryJar.setExecutable(true, false)
    }
}

open class PublishTask : DefaultTask() {

    @Input
    var appName: String = project.name

    init {
        group = "lighttunnel"
    }

    @TaskAction
    fun entry() {
        val r8Jar = File(project.buildDir, "libs/${project.name}-${project.version}-r8.jar")
        val distR8Jar = project.rootProject.file("dists/${project.version}/$appName.jar")
        r8Jar.copyTo(distR8Jar, overwrite = true)

        val binaryJar = File(project.buildDir, "libs/${project.name}-${project.version}-binary.jar")
        val distBinaryFile = project.rootProject.file("dists/${project.version}/$appName")
        binaryJar.copyTo(distBinaryFile, overwrite = true)
        distBinaryFile.setExecutable(true, false)
    }

}
