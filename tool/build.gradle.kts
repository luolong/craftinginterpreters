plugins {
    id("buildlogic.java-application-conventions")
    application
}

dependencies {
    //implementation(project(":utilities"))
}

java {
    manifest {
        attributes("Main-Class" to "ee.tepp.craftinginterpreters.tool.GenerateAst")
    }
}

application {
    // Define the main class for the application.
    mainClass = "ee.tepp.craftinginterpreters.tool.GenerateAst"
}
