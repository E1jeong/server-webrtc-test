import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.webrtc.java)
    implementation(variantOf(libs.webrtc.java) { classifier("windows-x86_64") })

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.desktop {
    application {
        mainClass = "com.sumas.operator.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "UBioWebRtcOperator"
            packageVersion = "1.0.0"
            description = "UBio WebRTC Desktop Operator Console"
            vendor = "Union Community"
            copyright = "© 2026 Union Community. All rights reserved."

            modules("java.instrument", "java.net.http", "jdk.unsupported")

            windows {
                menuGroup = "UBio WebRTC"
                upgradeUuid = "1879308e-1779-4d69-a1b7-e23a4128f9d1"
                shortcut = true
                dirChooser = true
                perUserInstall = true
            }
        }
    }
}