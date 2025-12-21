plugins {
    id("buildlogic.java-application-conventions")

}

dependencies {
    implementation(project(":lox"))
}

java {
    manifest {
        attributes("Main-Class" to "ee.tepp.craftinginterpreters.cli.Lox")
    }
}

application {
    // Define the main class for the application.
    mainClass = "ee.tepp.craftinginterpreters.cli.Lox"
}
