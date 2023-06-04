plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.android.tools:r8:3.0.73")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.1.202206130422-r")
}
