package ie.setu.config

import kong.unirest.core.Unirest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class JavalinConfigTest {

    @Test
    fun `uploads directory is created when missing`() {
        val uploadsDir = Path.of("uploads")
        val backupDir = Path.of("uploads-backup-${UUID.randomUUID()}")
        val hadUploads = Files.exists(uploadsDir)

        if (hadUploads) {
            Files.move(uploadsDir, backupDir)
        }

        try {
            JavalinConfig()
            assertEquals(true, Files.exists(uploadsDir))
        } finally {
            if (hadUploads) {
                uploadsDir.toFile().deleteRecursively()
                Files.move(backupDir, uploadsDir)
            }
        }
    }

    @Test
    fun `app property exposes configured javalin`() {
        val config = JavalinConfig()
        assertNotNull(config.app)
    }

    @Test
    fun `config handles 404 and exception responses`() {
        val config = JavalinConfig()
        val app = config.getJavalinService()
        app.get("/boom") { throw RuntimeException("boom") }

        app.start(0)
        try {
            val base = "http://localhost:${app.port()}"
            val notFound = Unirest.get("$base/not-here").asString()
            assertEquals(404, notFound.status)
            val errorResponse = Unirest.get("$base/boom").asString()
            assertEquals(500, errorResponse.status)
        } finally {
            app.stop()
        }
    }

    @Test
    fun `start uses PORT system property when set`() {
        val previousPort = System.getProperty("PORT")
        System.setProperty("PORT", "0")

        try {
            val app = JavalinConfig().startJavalinService()
            app.stop()
        } finally {
            if (previousPort == null) {
                System.clearProperty("PORT")
            } else {
                System.setProperty("PORT", previousPort)
            }
        }
    }
}
