// kosong saja; versi plugin sudah di settings.gradle.kts
plugins {}

tasks.wrapper {
    gradleVersion = "8.9"      // supaya wrapper konsisten di 8.9
    distributionType = Wrapper.DistributionType.BIN
}
