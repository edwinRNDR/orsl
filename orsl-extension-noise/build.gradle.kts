import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    org.openrndr.orsl.convention.`kotlin-multiplatform`
    alias(libs.plugins.ksp.annotation.processor)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.math)
                implementation(project(":orsl-shader-generator"))
                implementation(project(":orsl-shader-generator-annotations"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.gl3.core)
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", project(":orsl-shader-generator-processor"))
}

tasks.withType<KotlinCompile<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
tasks.findByName("jsSourcesJar")!!.dependsOn("kspCommonMainKotlinMetadata")
tasks.findByName("jvmSourcesJar")!!.dependsOn("kspCommonMainKotlinMetadata")
tasks.findByName("sourcesJar")!!.dependsOn("kspCommonMainKotlinMetadata")

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
