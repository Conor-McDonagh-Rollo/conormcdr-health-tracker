package ie.setu

import org.junit.jupiter.api.Test

class AppTest {

    @Test
    fun `main starts application and can be stopped`() {
        val previousPort = System.getProperty("PORT")
        System.setProperty("PORT", "0")

        try {
            main()
            stopApplication()
            stopApplication()
        } finally {
            if (previousPort == null) {
                System.clearProperty("PORT")
            } else {
                System.setProperty("PORT", previousPort)
            }
        }
    }
}
