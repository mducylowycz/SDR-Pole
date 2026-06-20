package app.sdrpole.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class JmbeInstallerTest {
    @Test void currentPlatformHasPinnedCreatorArtifact() {
        var artifact = JmbeInstaller.currentArtifact();
        assertTrue(artifact.fileName().startsWith("jmbe-creator-"));
        assertEquals(64, artifact.sha256().length());
        assertEquals("https", artifact.uri().getScheme());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SDR_POLE_NETWORK_TEST", matches = "true")
    void downloadsBuildsValidatesAndActivatesJmbe(@TempDir Path directory) throws Exception {
        var libraries = new DecoderLibraryManager();
        var jar = new JmbeInstaller(libraries, directory).install((message, progress) -> {});
        assertTrue(Files.isRegularFile(jar));
        assertEquals(jar.toAbsolutePath().normalize(), libraries.jmbePath().orElseThrow());
    }
}
