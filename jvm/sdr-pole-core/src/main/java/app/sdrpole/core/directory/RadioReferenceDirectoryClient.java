package app.sdrpole.core.directory;

import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.p25.P25SystemConfig;
import app.sdrpole.core.p25.P25Talkgroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Official RadioReference SOAP v18 adapter with U.S. Census coordinate-to-county resolution. */
public final class RadioReferenceDirectoryClient {
    private static final URI SOAP_ENDPOINT = URI.create("https://api.radioreference.com/soap2/");
    private static final String SOAP_NS = "http://api.radioreference.com/soap2";
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public RadioReferenceDirectoryClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    RadioReferenceDirectoryClient(HttpClient http) { this.http = http; }

    public DirectoryUpdate fetch(GeoPoint location, RadioReferenceCredentials credentials) throws IOException, InterruptedException {
        var area = censusArea(location);
        var country = soap("getCountryInfo", Map.of("coid", "1"), credentials);
        int stateId = records(country, "stateCode").stream()
                .filter(record -> area.stateCode().equalsIgnoreCase(text(record, "stateCode")))
                .mapToInt(record -> integer(record, "stid")).findFirst()
                .orElseThrow(() -> new IOException("RadioReference does not list state " + area.stateCode()));
        var state = soap("getStateInfo", Map.of("stid", Integer.toString(stateId)), credentials);
        int countyId = records(state, "countyName").stream()
                .filter(record -> sameCounty(area.countyName(), text(record, "countyName")))
                .mapToInt(record -> integer(record, "ctid")).findFirst()
                .orElseThrow(() -> new IOException("RadioReference could not match " + area.countyName()));
        var county = soap("getCountyInfo", Map.of("ctid", Integer.toString(countyId)), credentials);

        var systems = new LinkedHashMap<Integer, String>();
        for (var record : records(county, "sName")) {
            int sid = integer(record, "sid");
            if (sid > 0) systems.put(sid, text(record, "sName"));
        }
        // Statewide systems are relevant to every county and must not require manual discovery.
        for (var record : records(state, "sName")) {
            int sid = integer(record, "sid");
            if (sid > 0) systems.putIfAbsent(sid, text(record, "sName"));
        }

        var types = soap("getTrsType", Map.of("id", "0"), credentials);
        var typeNames = new HashMap<Integer, String>();
        for (var record : records(types, "sTypeDescr")) typeNames.put(integer(record, "sType"), text(record, "sTypeDescr"));

        var sites = new ArrayList<P25SystemConfig>();
        var talkgroups = new ArrayList<P25Talkgroup>();
        for (var entry : systems.entrySet()) {
            var details = soap("getTrsDetails", Map.of("sid", Integer.toString(entry.getKey())), credentials);
            var detailRecords = records(details, "sType");
            if (detailRecords.isEmpty()) continue;
            var type = typeNames.getOrDefault(integer(detailRecords.getFirst(), "sType"), "");
            if (!type.toLowerCase(Locale.ROOT).contains("project 25") && !type.toLowerCase(Locale.ROOT).contains("p25")) continue;
            var siteDocument = soap("getTrsSites", Map.of("sid", Integer.toString(entry.getKey())), credentials);
            sites.addAll(parseSites(siteDocument, entry.getValue(), location));
            var talkgroupDocument = soap("getTrsTalkgroups", Map.of(
                    "sid", Integer.toString(entry.getKey()), "tgCid", "0", "tgTag", "0", "tgDec", "0"), credentials);
            talkgroups.addAll(parseTalkgroups(talkgroupDocument, entry.getValue()));
        }
        sites.sort(java.util.Comparator.comparingDouble(site -> site.location() == null
                ? Double.MAX_VALUE : location.distanceKmTo(site.location())));
        return new DirectoryUpdate("RadioReference", area.countyName() + ", " + area.stateCode(), Instant.now(), sites, talkgroups);
    }

    private Area censusArea(GeoPoint point) throws IOException, InterruptedException {
        var query = "https://geocoding.geo.census.gov/geocoder/geographies/coordinates?x="
                + encode(Double.toString(point.longitude())) + "&y=" + encode(Double.toString(point.latitude()))
                + "&benchmark=Public_AR_Current&vintage=Current_Current&format=json";
        var request = HttpRequest.newBuilder(URI.create(query)).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "SDR-Pole/0.2 (+https://github.com/mducylowycz/SDR-Pole)").GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("U.S. Census location lookup returned HTTP " + response.statusCode());
        JsonNode geographies = json.readTree(response.body()).path("result").path("geographies");
        JsonNode states = geographies.path("States");
        JsonNode counties = geographies.path("Counties");
        if (!states.isArray() || states.isEmpty() || !counties.isArray() || counties.isEmpty())
            throw new IOException("This network directory currently supports locations in the United States");
        return new Area(counties.get(0).path("NAME").asText(), states.get(0).path("STUSAB").asText());
    }

    private Document soap(String operation, Map<String, String> arguments,
                          RadioReferenceCredentials credentials) throws IOException, InterruptedException {
        var body = new StringBuilder("<rr:").append(operation).append('>');
        arguments.forEach((name, value) -> body.append('<').append(name).append('>').append(xml(value))
                .append("</").append(name).append('>'));
        body.append("<authInfo><username>").append(xml(credentials.username())).append("</username><password>")
                .append(xml(credentials.password())).append("</password><appKey>").append(xml(credentials.appKey()))
                .append("</appKey><version>latest</version><style>rpc</style></authInfo></rr:").append(operation).append('>');
        var envelope = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:rr="http://api.radioreference.com/soap2">
                  <soapenv:Body>%s</soapenv:Body>
                </soapenv:Envelope>
                """.formatted(body);
        var request = HttpRequest.newBuilder(SOAP_ENDPOINT).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", SOAP_NS + "#" + operation)
                .POST(HttpRequest.BodyPublishers.ofString(envelope)).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        var document = parse(response.body());
        var faults = document.getElementsByTagNameNS("*", "Fault");
        if (faults.getLength() > 0) {
            var message = records(document, "faultstring");
            throw new IOException(message.isEmpty() ? "RadioReference rejected the request" : text(message.getFirst(), "faultstring"));
        }
        if (response.statusCode() != 200) throw new IOException("RadioReference returned HTTP " + response.statusCode());
        return document;
    }

    static List<P25SystemConfig> parseSites(Document document, String systemName, GeoPoint center) {
        var result = new ArrayList<P25SystemConfig>();
        for (var site : records(document, "siteDescr")) {
            var frequencies = new ArrayList<Long>();
            Element frequencyContainer = resolvedChild(document, site, "siteFreqs");
            if (frequencyContainer != null) {
                for (var item : resolvedItems(document, frequencyContainer)) {
                    var use = text(item, "use").toLowerCase(Locale.ROOT);
                    if (!use.contains("control") && !use.equals("c") && !use.equals("a")
                            && !use.contains("primary") && !use.contains("alternate")) continue;
                    double mhz = decimal(item, "freq");
                    if (mhz > 0) frequencies.add(Math.round(mhz * 1_000_000));
                }
            }
            if (frequencies.isEmpty()) continue;
            GeoPoint location = null;
            double lat = decimal(site, "lat"), lon = decimal(site, "lon");
            if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180 && (lat != 0 || lon != 0)) location = new GeoPoint(lat, lon);
            if (location != null && center.distanceKmTo(location) > 300) continue;
            var modulation = text(site, "siteModulation").toLowerCase(Locale.ROOT);
            boolean simulcast = modulation.contains("simulcast") || modulation.contains("lsm") || modulation.contains("cqpsk");
            var name = text(site, "siteDescr");
            if (name.isBlank()) name = "Site " + text(site, "siteNumber");
            result.add(new P25SystemConfig(systemName, name, frequencies.stream().distinct().toList(),
                    simulcast ? P25SystemConfig.Modulation.LSM_SIMULCAST : P25SystemConfig.Modulation.C4FM, 4, location));
        }
        return result;
    }

    static List<P25Talkgroup> parseTalkgroups(Document document, String systemName) {
        var result = new ArrayList<P25Talkgroup>();
        for (var record : records(document, "tgDec")) {
            int id = integer(record, "tgDec");
            if (id < 0 || id > 65_535) continue;
            result.add(new P25Talkgroup(systemName, id, text(record, "tgAlpha"), text(record, "tgDescr"),
                    text(record, "tgMode"), Math.max(0, Math.min(2, integer(record, "enc")))));
        }
        return result.stream().collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toMap(P25Talkgroup::id, item -> item, (left, right) -> left, LinkedHashMap::new),
                map -> List.copyOf(map.values())));
    }

    static Document parse(String xml) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception problem) { throw new IOException("Directory returned invalid XML", problem); }
    }

    private static List<Element> records(Document document, String requiredField) {
        var result = new ArrayList<Element>();
        var all = document.getElementsByTagName("*");
        for (int index = 0; index < all.getLength(); index++) {
            if (all.item(index) instanceof Element element && directChild(element, requiredField) != null) result.add(element);
        }
        return result;
    }

    private static Element resolvedChild(Document document, Element parent, String name) {
        var child = directChild(parent, name);
        if (child == null) return null;
        var href = child.getAttribute("href");
        return href.isBlank() ? child : findById(document, href.substring(1));
    }

    private static List<Element> resolvedItems(Document document, Element container) {
        var result = new ArrayList<Element>();
        for (Node child = container.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element element)) continue;
            var href = element.getAttribute("href");
            result.add(href.isBlank() ? element : findById(document, href.substring(1)));
        }
        return result.stream().filter(java.util.Objects::nonNull).toList();
    }

    private static Element findById(Document document, String id) {
        var all = document.getElementsByTagName("*");
        for (int index = 0; index < all.getLength(); index++)
            if (all.item(index) instanceof Element element && id.equals(element.getAttribute("id"))) return element;
        return null;
    }

    private static Element directChild(Element parent, String name) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Element element && (name.equals(element.getLocalName()) || name.equals(element.getNodeName()))) return element;
        return null;
    }

    private static String text(Element parent, String name) {
        var child = directChild(parent, name);
        if (child == null && (name.equals(parent.getLocalName()) || name.equals(parent.getNodeName()))) child = parent;
        return child == null ? "" : child.getTextContent().trim();
    }
    private static int integer(Element parent, String name) { try { return Integer.parseInt(text(parent, name)); } catch (Exception ignored) { return 0; } }
    private static double decimal(Element parent, String name) { try { return Double.parseDouble(text(parent, name)); } catch (Exception ignored) { return 0; } }
    private static boolean sameCounty(String left, String right) { return normalizeCounty(left).equals(normalizeCounty(right)); }
    private static String normalizeCounty(String value) { return value.toLowerCase(Locale.ROOT).replace(" county", "").replace(" parish", "").replaceAll("[^a-z0-9]", ""); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String xml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"); }
    private record Area(String countyName, String stateCode) {}
}
