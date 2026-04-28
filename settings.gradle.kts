pluginManagement {
    repositories {
        // 阿里云 Google 镜像
        maven("https://maven.aliyun.com/repository/google")
        // 阿里云 Central 镜像
        maven("https://maven.aliyun.com/repository/central")
        // 阿里云 Gradle Plugin 镜像
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        // 兜底官方源
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云 Google 镜像
        maven("https://maven.aliyun.com/repository/google")
        // 阿里云 Central 镜像
        maven("https://maven.aliyun.com/repository/central")
        // 阿里云 JCenter 镜像
        maven("https://maven.aliyun.com/repository/jcenter")
        // 阿里云 Public 聚合镜像（central + jcenter）
        maven("https://maven.aliyun.com/repository/public")
        // 兜底官方源
        google()
        mavenCentral()
    }
}

rootProject.name = "OpenWifi"
include(":app")

