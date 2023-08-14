plugins {
  id("otel.library-instrumentation")
}

dependencies {
  library("io.netty:netty-codec-http:4.1.0.Final")
  implementation(project(":instrumentation:netty:netty-4-common:library"))
  implementation(project(":instrumentation:netty:netty-common:library"))

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":instrumentation:netty:netty-4.1:testing"))
}

tasks {
  val testStableSemconv by registering(Test::class) {
    filter {
      includeTestsMatching("*ClientTest")
    }
    include("**/*ClientTest.*")

    jvmArgs("-Dotel.semconv-stability.opt-in=http")
  }

  check {
    dependsOn(testStableSemconv)
  }
}
