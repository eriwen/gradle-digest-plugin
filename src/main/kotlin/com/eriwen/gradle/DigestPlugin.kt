package com.eriwen.gradle

import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.lang.RuntimeException
import java.nio.file.Files

open class DigestPlugin : Plugin<Project> {
    override fun apply(p0: Project?) {}
}

open class Digest : SourceTask() {
    @Input
    var algorithm: String = DigestAlgorithm.MD5.toString()

    @OutputDirectory
    var dest: File? = null

    fun setDest(input: String) {
        this.dest = project.file(input)
    }
    fun dest(input: String) = setDest(input)
    fun dest(input: File) = {
        this.dest = input
    }

    @TaskAction
    fun run() {
        processChanges(getSource())
    }

    private fun processChanges(sourceTree: FileTree) {
        sourceTree.visit(object : FileVisitor {
            override fun visitDir(visitDetails: FileVisitDetails) {
                visitDetails.relativePath.getFile(dest!!).mkdir()
            }

            override fun visitFile(visitDetails: FileVisitDetails) {
                try {
                    val checksum = digest(visitDetails.file, DigestAlgorithm.valueOf(algorithm))
                    val destFile = visitDetails.relativePath.getFile(dest!!)
                    visitDetails.copyTo(digestedFile(destFile, checksum))
                    Files.write(checksumFile(destFile, DigestAlgorithm.valueOf(algorithm)).toPath(), checksum.toByteArray())
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        })
    }

    private fun digest(file: File, algorithm: DigestAlgorithm): String {
        val contentBytes = Files.readAllBytes(file.toPath())
        val checksum: String
        when (algorithm) {
            DigestAlgorithm.MD5 -> checksum = DigestUtils.md5Hex(contentBytes)
            DigestAlgorithm.SHA1 -> checksum = DigestUtils.sha1Hex(contentBytes)
            DigestAlgorithm.SHA256 -> checksum = DigestUtils.sha256Hex(contentBytes)
            DigestAlgorithm.SHA512 -> checksum = DigestUtils.sha512Hex(contentBytes)
        }
        return checksum
    }

    private fun checksumFile(destFile: File, algorithm: DigestAlgorithm): File {
        return project.file(destFile.absolutePath + "." + algorithm.suffix())
    }

    private fun digestedFile(file: File, digest: String): File {
        return project.file(file.parent + File.separator + digest + "-" + file.name)
    }

    private fun deleteOutputsFor(file: File) {
        file.delete()
        File(file.absolutePath + "." + DigestAlgorithm.valueOf(algorithm).suffix()).delete()
    }
}

enum class DigestAlgorithm {
    MD5, SHA1, SHA256, SHA512;

    fun suffix(): String = toString().toLowerCase();
}
