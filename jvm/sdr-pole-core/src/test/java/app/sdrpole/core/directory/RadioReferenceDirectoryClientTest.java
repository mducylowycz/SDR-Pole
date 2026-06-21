package app.sdrpole.core.directory;

import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.p25.P25SystemConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RadioReferenceDirectoryClientTest {
    @Test void parsesSoapEncodedControlChannelsAndSimulcast() throws Exception {
        var xml = """
                <Envelope><Body><return href="#sites"/>
                <multiRef id="sites"><item href="#site1"/></multiRef>
                <multiRef id="site1"><siteId>1</siteId><siteDescr>Central Simulcast</siteDescr>
                  <siteNumber>2</siteNumber><siteModulation>LSM</siteModulation><lat>41.1</lat><lon>-87.2</lon>
                  <siteFreqs href="#freqs"/></multiRef>
                <multiRef id="freqs"><item href="#f1"/><item href="#f2"/><item href="#f3"/></multiRef>
                <multiRef id="f1"><freq>851.1125</freq><use>Primary Control</use></multiRef>
                <multiRef id="f2"><freq>852.225</freq><use>Alternate Control</use></multiRef>
                <multiRef id="f3"><freq>853.3</freq><use>Voice</use></multiRef>
                </Body></Envelope>
                """;
        var sites = RadioReferenceDirectoryClient.parseSites(
                RadioReferenceDirectoryClient.parse(xml), "County Public Safety", new GeoPoint(41, -87));
        assertEquals(1, sites.size());
        assertEquals(2, sites.getFirst().controlFrequenciesHz().size());
        assertEquals(P25SystemConfig.Modulation.LSM_SIMULCAST, sites.getFirst().modulation());
        assertEquals(851_112_500L, sites.getFirst().controlFrequenciesHz().getFirst());
    }

    @Test void rejectsDoctypeToPreventExternalEntityReads() {
        assertThrows(Exception.class, () -> RadioReferenceDirectoryClient.parse(
                "<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><x>&e;</x>"));
    }

    @Test void parsesTalkgroupLabelsAndEncryptionWithoutGuessing() throws Exception {
        var xml = """
                <Envelope><Body>
                <item><tgDec>101</tgDec><tgAlpha>Fire Dispatch</tgAlpha><tgDescr>County Fire Dispatch</tgDescr><tgMode>D</tgMode><enc>0</enc></item>
                <item><tgDec>202</tgDec><tgAlpha>Police Tac</tgAlpha><tgDescr>Encrypted tactical</tgDescr><tgMode>DE</tgMode><enc>2</enc></item>
                </Body></Envelope>
                """;
        var groups = RadioReferenceDirectoryClient.parseTalkgroups(
                RadioReferenceDirectoryClient.parse(xml), "County Public Safety");
        assertEquals(2, groups.size());
        assertEquals("Fire Dispatch", groups.getFirst().alias());
        assertTrue(groups.get(1).fullyEncrypted());
    }
}
