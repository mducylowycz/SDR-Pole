package app.sdrpole.core.p25;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class P25CsvImporterTest {
    @TempDir Path temp;

    @Test void importsFriendlyAndQuotedColumns() throws Exception {
        var file = temp.resolve("sites.csv");
        Files.writeString(file, "System Name,Site Name,Control Channels,Latitude,Longitude,Modulation\n"
                + "\"County, Public Safety\",North Simulcast,851.1125;852.225,41.1,-87.2,LSM\n");
        var sites = new P25CsvImporter().read(file);
        assertEquals("County, Public Safety", sites.getFirst().systemName());
        assertEquals(2, sites.getFirst().controlFrequenciesHz().size());
        assertEquals(P25SystemConfig.Modulation.LSM_SIMULCAST, sites.getFirst().modulation());
        assertNotNull(sites.getFirst().location());
    }

    @Test void explainsMissingColumns() throws Exception {
        var file = temp.resolve("bad.csv");
        Files.writeString(file, "Name,Frequency\nExample,851.1\n");
        var error = assertThrows(Exception.class, () -> new P25CsvImporter().read(file));
        assertTrue(error.getMessage().contains("system"));
    }
}
