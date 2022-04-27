package krust

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject

/**
 * Initializes a new project after the template is cloned by renaming all relevant identifiers and
 * moving source files accordingly.
 */
@DisableCachingByDefault(because = "task changes sources")
abstract class KrustInit : DefaultTask() {

    @get:Option(
        option = "app-id",
        description = "Android application id, also used as Kotlin package name.",
    )
    @get:Input
    abstract val appId: Property<String>

    @get:Option(
        option = "app-name",
        description = "Simple, PascalCase name for the app, also used as Kotlin type name prefix.",
    )
    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val fromAppName: Property<String>

    @get:Input
    abstract val fromAppId: Property<String>

    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection

    @TaskAction
    fun replaceStringsAndMoveSourcesToNewPackage() {
        val fromId = fromAppId.get()
        val toId = appId.get()
        val fromName = fromAppName.get()
        val toName = appName.get()
        val fromDir = fromId.replace('.', '/')
        val toDir = toId.replace('.', '/')
        val fromJniId = fromId.replace('.', '_')
        val toJniId = toId.replace('.', '_')
        existingInputFiles().forEach { file ->
            file.readText()
                .replace(fromId, toId)
                .replace(fromDir, toDir)
                .replace(fromJniId, toJniId)
                .replace(fromName, toName)
                .run(file::writeText)
        }
        moveSources(fromDir, toDir, listOf(
            "app/src/androidTest/java",
            "app/src/test/java",
            "app/src/main/java",
        ))
    }

    private
    fun existingInputFiles(): Sequence<File> =
        inputFiles.files.asSequence().filter { it.isFile }

    private
    fun moveSources(fromPackagePath: String, toPackagePath: String, sourceSets: List<String>) {
        sourceSets.forEach { sourceDirPath ->
            val sourceDir = layout.projectDirectory.dir(sourceDirPath).asFile.toPath()
            val fromPackageDir = sourceDir.resolve(fromPackagePath)
            val toPackageDir = sourceDir.resolve(toPackagePath)
            moveDirectory(fromPackageDir, toPackageDir)
            deleteEmptyAncestorsOf(fromPackageDir)
        }
    }

    private
    tailrec fun deleteEmptyAncestorsOf(dir: Path) {
        when {
            Files.isDirectory(dir) -> {
                if (Files.list(dir).findAny().isEmpty) {
                    delete(dir)
                    deleteEmptyAncestorsOf(dir.parent)
                }
            }
            else -> deleteEmptyAncestorsOf(dir.parent)
        }
    }

    private
    fun moveDirectory(fromDir: Path, toDir: Path) {

        fun relPathOf(path: Path) = when {
            path != fromDir -> path.subpath(fromDir.nameCount, path.nameCount)
            else -> null
        }

        fun destPathOf(path: Path): Path =
            relPathOf(path)?.let(toDir::resolve) ?: toDir

        Files.createDirectories(toDir)
        Files.walkFileTree(fromDir, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(
                dir: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                destPathOf(dir).let {
                    if (!Files.isDirectory(it)) {
                        logger.info("$dir => $it")
                        Files.createDirectory(it)
                    }
                }
                return super.preVisitDirectory(dir, attrs)
            }

            override fun visitFile(
                file: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                destPathOf(file).let {
                    logger.info("$file => $it")
                    Files.move(file, it, StandardCopyOption.REPLACE_EXISTING)
                }
                return super.visitFile(file, attrs)
            }

            override fun postVisitDirectory(
                dir: Path,
                exc: IOException?,
            ): FileVisitResult {
                return super.postVisitDirectory(dir, exc).also {
                    delete(dir)
                }
            }
        })
    }

    private
    fun delete(dir: Path) {
        logger.info("Deleting $dir...")
        Files.delete(dir)
    }

    @get:Inject
    protected
    abstract val layout: ProjectLayout
}
