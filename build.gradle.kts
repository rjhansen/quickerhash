import java.io.ByteArrayOutputStream

plugins {
    id("java")
    id("org.beryx.jlink") version "4.1.0"
}

group = "engineering.hansen"
version = "1.0"
val osName = System.getProperty("os.name").lowercase()

application {
    mainModule.set("QuickerHash.main")     // your module-info.java module name
    mainClass.set("engineering.hansen.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:3.7.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

when {
    osName.contains("mac") -> {
        jlink {
            options.set(listOf("--strip-debug", "--compress=zip-6", "--no-header-files", "--no-man-pages"))

            launcher {
                name = "QuickerHash"
                jvmArgs = listOf("--enable-native-access=ALL-UNNAMED", "--enable-native-access=com.formdev.flatlaf")
            }

            jpackage {
                imageName = "QuickerHash"
                installerName = "QuickerHash"
                installerType = "dmg"
                skipInstaller = false

                imageOptions = listOf(
                    "--vendor", "Robert Hansen",
                    // Note: if you're not me, either (a) substitute your
                    // own credentials or (b) omit the code signing commands.
                    "--mac-sign",
                    "--mac-signing-key-user-name", "Developer ID Application: Robert Hansen (QUCQ6M7QQ5)",
                    "--mac-signing-keychain", "/Users/rjh/Library/Keychains/login.keychain-db",
                    "--mac-package-identifier", "engineering.hansen.QuickerHash"
                )
                installerOptions = listOf(
                    "--vendor", "Robert Hansen",
                    "--app-version", "$version",
                    // Note: if you're not me, either (a) substitute your
                    // own credentials or (b) omit the code signing commands.
                    "--mac-sign",
                    "--mac-signing-key-user-name", "Developer ID Application: Robert Hansen (QUCQ6M7QQ5)",
                    "--mac-signing-keychain", "/Users/rjh/Library/Keychains/login.keychain-db",
                    "--mac-package-identifier", "engineering.hansen.QuickerHash"
                )
            }
        }

        // If you're not me, either change the credentials to refer to your
        // own, or else omit this entirely.
        tasks.register<Exec>("signDmg") {
            group = "distribution"
            description = "Codesigns the disk image"
            mustRunAfter("jpackageImage", "jpackage")

            val dmgFile = layout.buildDirectory.file("jpackage/QuickerHash-$version.dmg")
            inputs.file(dmgFile)

            // This task mutates an existing file owned by :jpackage rather than producing
            // its own output, so it's intentionally excluded from output tracking —
            // otherwise Gradle deletes the dmg as a "stale output" before this runs.
            doNotTrackState("Signs the dmg produced by :jpackage in place; does not own the file")

            commandLine(
                "codesign",
                "--force",
                "--timestamp",
                "--sign", "Developer ID Application: Robert Hansen (QUCQ6M7QQ5)",
                dmgFile.get().asFile.absolutePath
            )
        }

        tasks.register<Exec>("notarizeDmg") {
            group = "distribution"
            description = "Submits the signed dmg to Apple's notary service and waits for approval"
            dependsOn("signDmg")

            val dmgFile = layout.buildDirectory.file("jpackage/QuickerHash-$version.dmg")
            inputs.file(dmgFile)

            // Submission doesn't modify the file, but it's a network call with side
            // effects on Apple's servers, not a deterministic local build step —
            // exclude it from up-to-date tracking so it always actually runs.
            doNotTrackState("Submits to Apple's notary service; not a reproducible local build step")

            commandLine(
                "xcrun", "notarytool", "submit",
                dmgFile.get().asFile.absolutePath,
                "--keychain-profile", "QuickerHash-Notary",
                "--wait"
            )
        }

        tasks.register<Exec>("stapleDmg") {
            group = "distribution"
            description = "Staples the notarization ticket to the dmg"
            dependsOn("notarizeDmg")

            val dmgFile = layout.buildDirectory.file("jpackage/QuickerHash-$version.dmg")
            inputs.file(dmgFile)

            // Mutates the dmg in place (embeds the ticket) rather than producing
            // a new file — same reasoning as signDmg above.
            doNotTrackState("Staples the notarization ticket onto the dmg produced by :jpackage in place")

            commandLine(
                "xcrun", "stapler", "staple",
                dmgFile.get().asFile.absolutePath
            )
        }

        tasks.register<Exec>("verifyDmg") {
            group = "verification"
            description = "Confirms Gatekeeper accepts the dmg as signed and notarized"
            dependsOn("stapleDmg")
            mustRunAfter("jpackageImage", "jpackage")

            val dmgFile = layout.buildDirectory.file("jpackage/QuickerHash-$version.dmg")
            inputs.file(dmgFile)

            doNotTrackState("Runs a read-only Gatekeeper check against the dmg produced by :jpackage")

            val output = ByteArrayOutputStream()
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true

            commandLine(
                "spctl", "-a", "-vvv", "-t", "open",
                "--context", "context:primary-signature",
                dmgFile.get().asFile.absolutePath
            )

            doLast {
                val result = output.toString()
                println(result)

                if (executionResult.get().exitValue != 0) {
                    throw GradleException("Gatekeeper rejected the dmg:\n$result")
                }
                if (!result.contains("source=Notarized Developer ID")) {
                    throw GradleException(
                        "dmg passed Gatekeeper but was not reported as notarized " +
                                "(missing 'source=Notarized Developer ID'):\n$result"
                    )
                }
            }
        }

        tasks.named("jpackage") {
            finalizedBy("verifyDmg")
        }
    }
    osName.contains("linux") -> {
        jlink {
            options.set(listOf("--strip-debug", "--compress=zip-6", "--no-header-files", "--no-man-pages"))

            launcher {
                name = "QuickerHash"
                jvmArgs = listOf("--enable-native-access=ALL-UNNAMED", "--enable-native-access=com.formdev.flatlaf")
            }

            jpackage {
                imageName = "QuickerHash"
                installerName = "QuickerHash"
                installerType = "rpm"
                skipInstaller = false

                imageOptions = listOf(
                    "--vendor", "Robert Hansen",
                )
                installerOptions = listOf(
                    "--vendor", "Robert Hansen",
                    "--app-version", "$version",
                    "--linux-package-name", "quickerhash",
                    "--linux-menu-group", "Utility",
                    "--linux-shortcut",
//                    "--linux-deb-maintainer", "rob@hansen.engineering" // required for .deb packages
                )
            }
        }

        // Requires a GPG signing key already imported into the build machine's keyring,
        // and rpm configured to find it (typically via %_gpg_name / %_openpgp_sign_id
        // in ~/.rpmmacros, or the --define below).
        tasks.register<Exec>("signRpm") {
            group = "distribution"
            description = "Signs the rpm with a GPG key"

            // This task mutates an existing file owned by :jpackage rather than producing
            // its own output, so it's intentionally excluded from output tracking —
            // otherwise Gradle deletes the rpm as a "stale output" before this runs.
            doNotTrackState("Signs the rpm produced by :jpackage in place; does not own the file")

            doFirst {
                // The exact filename includes release and architecture (e.g.
                // QuickerHash-1.0.0-1.x86_64.rpm), so locate it rather than hardcoding it.
                val rpmFile = layout.buildDirectory.dir("jpackage").get().asFileTree
                    .matching { include("*.rpm") }
                    .singleFile

                commandLine(
                    "rpmsign",
                    "--addsign",
                    rpmFile.absolutePath
                )
            }
        }

        tasks.named("jpackage") {
            finalizedBy("signRpm")
        }
    }
    osName.contains("win") -> {
        jlink {
            options.set(listOf("--strip-debug", "--compress=zip-6", "--no-header-files", "--no-man-pages"))

            launcher {
                name = "QuickerHash"
                jvmArgs = listOf("--enable-native-access=ALL-UNNAMED", "--enable-native-access=com.formdev.flatlaf")
            }

            jpackage {
                imageName = "QuickerHash"
                installerName = "QuickerHash"
                installerType = "msi"
                skipInstaller = false

                imageOptions = listOf(
                    "--vendor", "Robert Hansen",
                )
                installerOptions = listOf(
                    "--vendor", "Robert Hansen",
                    "--app-version", "$version",
                    "--win-menu",
                    "--win-menu-group", "QuickerHash",
                    "--win-shortcut",
                    "--win-dir-chooser",
                    "--win-per-user-install"
                )
            }
        }
    }
    else -> throw GradleException("Unsupported operating system: $osName")
}
