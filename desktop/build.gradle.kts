import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.programmersbox"
version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "common"
            packageVersion = "1.0.0"

            macOS {
                bundleID = "com.programmersbox.pillcounter"
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSBluetoothAlwaysUsageDescription</key>  
                        <string>Privacy - Bluetooth Always Usage Description</string>  
                        <key>NSBluetoothPeripheralUsageDescription</key>  
                        <string>Privacy - Bluetooth Peripheral Usage Description</string>  
                    """.trimIndent()
                }
            }
        }
    }
}
