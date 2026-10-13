package ru.landilf.ollink

import ru.landilf.ollink.domain.model.*
import ru.landilf.ollink.data.network.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ServerAddressTest {
    @Test fun acceptsLanAndEmulatorAddresses() {
        assertEquals("http://192.168.1.10:11434", ServerAddress.normalize(" http://192.168.1.10:11434/ "))
        assertEquals("http://10.0.2.2:11434", ServerAddress.normalize("http://10.0.2.2:11434"))
        assertEquals("https://ollama.local", ServerAddress.normalize("https://ollama.local"))
        assertEquals("http://[::1]:11434", ServerAddress.normalize("http://[::1]:11434"))
    }

    @Test fun rejectsInvalidRootsBeforeNetworkRequest() {
        listOf("", "192.168.1.10:11434", "ftp://host", "http://", "http://host:0",
            "http://host:65536", "http://host/api/chat", "http://host?key=x",
            "http://host#fragment", "http://user:password@host", "http://bad host")
            .forEach { input ->
                assertThrows("Must reject $input", IllegalArgumentException::class.java) {
                    ServerAddress.normalize(input)
                }
            }
    }
}
