package ie.setu.controllers

import io.javalin.http.UploadedFile
import jakarta.servlet.http.Part
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.InvocationTargetException

class HealthTrackerControllerInternalTest {

    @Test
    fun `store badge file closes stream on failure`() {
        val failingStream = object : InputStream() {
            override fun read(): Int {
                throw IOException("boom")
            }
        }

        val part = object : Part {
            override fun getInputStream(): InputStream = failingStream
            override fun getContentType(): String? = "image/png"
            override fun getName(): String = "badge"
            override fun getSubmittedFileName(): String = "badge"
            override fun getSize(): Long = 10
            override fun write(fileName: String?) {}
            override fun delete() {}
            override fun getHeader(name: String?): String? = null
            override fun getHeaders(name: String?): MutableCollection<String> = mutableListOf()
            override fun getHeaderNames(): MutableCollection<String> = mutableListOf()
        }

        val uploadedFile = UploadedFile(part)
        val method = HealthTrackerController::class.java
            .getDeclaredMethod("storeBadgeFile", UploadedFile::class.java)
        method.isAccessible = true

        val exception = assertThrows(InvocationTargetException::class.java) {
            method.invoke(HealthTrackerController, uploadedFile)
        }
        assertEquals(true, exception.cause is IOException)
    }
}
