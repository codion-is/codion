dependencies {
    api(project(":codion-swing-common-model"))

    api(libs.ikonli.core)
    api(libs.ikonli.swing)

    testImplementation(libs.ikonli.foundation.pack)
    testImplementation(libs.assertj.swing)
}