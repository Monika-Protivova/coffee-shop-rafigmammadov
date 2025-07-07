import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.time.Duration

fun main() {
    println("Starting container test...")
    
    val container = GenericContainer<GenericContainer<*>>(
        DockerImageName.parse("openjdk:21-slim")
    ).apply {
        withExposedPorts(8080)
        
        // Copy the application JAR file
        withCopyFileToContainer(
            MountableFile.forHostPath("coffee-shop-application/build/libs/coffee-shop-application-all.jar"),
            "/app/app.jar"
        )
        
        // Copy the application configuration
        withCopyFileToContainer(
            MountableFile.forHostPath("coffee-shop-application/src/main/resources/application.yaml"),
            "/app/config/application.yaml"
        )
        
        // Copy the test configuration
        withCopyFileToContainer(
            MountableFile.forHostPath("coffee-shop-integration-tests/src/test/resources/application-test.yaml"),
            "/app/config/application-test.yaml"
        )
        
        // Set the environment variable for the configuration file
        withEnv("KTOR_CONFIG_FILE", "/app/config/application-test.yaml")
        
        // Run the application
        withCommand("java", "-jar", "/app/app.jar")
        
        // Add logging
        withLogConsumer(Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("Container")))
        
        // Wait for the application to be ready
        waitingFor(
            Wait.forListeningPort()
                .withStartupTimeout(Duration.ofSeconds(60))
        )
    }
    
    try {
        println("Starting container...")
        container.start()
        println("Container started successfully!")
        println("Container logs:")
        println(container.logs())
        
        val baseUrl = "http://${container.host}:${container.getMappedPort(8080)}/api/v1"
        println("Base URL: $baseUrl")
        
        // Wait a bit more for the application to fully start
        Thread.sleep(5000)
        
        println("Final container logs:")
        println(container.logs())
        
    } catch (e: Exception) {
        println("Container failed to start: ${e.message}")
        println("Container logs:")
        println(container.logs())
        e.printStackTrace()
    } finally {
        container.stop()
    }
} 