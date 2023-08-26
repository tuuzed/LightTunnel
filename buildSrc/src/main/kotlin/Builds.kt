import org.eclipse.jgit.api.Git
import org.gradle.api.Project
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object Builds {

    fun getBuildDate(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
    }

    fun getCommitHash(project: Project): String? {
        return runCatching {
            Git.open(project.rootProject.rootDir).log().setMaxCount(1).call().firstOrNull()?.name
        }.getOrNull()
    }

    fun getCommitDate(project: Project): String? {
        return runCatching {
            Git.open(project.rootProject.rootDir).log().setMaxCount(1).call().firstOrNull()?.commitTime?.let {
                LocalDateTime.ofEpochSecond(
                    it.toLong(), 0, ZoneOffset.ofHours(8)
                ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            }
        }.getOrNull()
    }

}
