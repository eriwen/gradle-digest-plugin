import com.eriwen.gradle.Digest

plugins {
    id("com.eriwen.gradle.digest") version "0.0.3"
}

val digestSha1 by tasks.creating(Digest::class) {
    source = fileTree("$projectDir/js")
    dest = file("$projectDir/build")
    algorithm = "SHA1"
}
