plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.apache.logging.log4j")
    module.set("log4j-core")
    versions.set("[2.7,2.17.0)")
    assertInverse.set(true)
  }
}

dependencies {
  library("org.apache.logging.log4j:log4j-core:2.7")

  testInstrumentation(project(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.17:javaagent"))

  testImplementation(project(":instrumentation:log4j:log4j-context-data:log4j-context-data-common:testing"))

  latestDepTestLibrary("org.apache.logging.log4j:log4j-core:2.16.+") // see log4j-context-data-2.17 module
}

tasks {
  test {
    filter {
      excludeTestsMatching("Log4j27BaggageTest")
    }
  }

  val testAddBaggage by registering(Test::class) {
    filter {
      includeTestsMatching("Log4j27BaggageTest")
    }
    jvmArgs("-Dotel.instrumentation.log4j-context-data.add-baggage=true")
  }

  named("check") {
    dependsOn(testAddBaggage)
  }
}
