plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.android.tools:r8:8.3.37")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")
}
