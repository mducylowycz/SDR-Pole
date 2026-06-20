import Foundation

enum PlaylistImportError: LocalizedError {
    case unreadable(URL)
    case malformedXML(String)

    var errorDescription: String? {
        switch self {
        case .unreadable(let url): "Could not read playlist at \(url.path)"
        case .malformedXML(let message): "Invalid SDRTrunk playlist: \(message)"
        }
    }
}

final class SDRTrunkPlaylistImporter: NSObject, XMLParserDelegate {
    private var channels: [RadioChannel] = []
    private var current: ChannelDraft?
    private var parserError: Error?

    func importPlaylist(at url: URL) throws -> [RadioChannel] {
        guard let parser = XMLParser(contentsOf: url) else {
            throw PlaylistImportError.unreadable(url)
        }
        channels = []
        current = nil
        parserError = nil
        parser.delegate = self
        guard parser.parse() else {
            throw PlaylistImportError.malformedXML(
                parser.parserError?.localizedDescription ?? parserError?.localizedDescription ?? "Unknown error"
            )
        }
        return channels
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String] = [:]
    ) {
        switch elementName {
        case "channel":
            current = ChannelDraft(
                system: attributeDict["system"] ?? "Unknown",
                site: attributeDict["site"] ?? "",
                name: attributeDict["name"] ?? "Unnamed",
                enabled: attributeDict["enabled"]?.lowercased() == "true"
            )
        case "source_configuration":
            current?.frequencyHz = UInt64(attributeDict["frequency"] ?? "")
        case "decode_configuration":
            let type = attributeDict["type"] ?? ""
            current?.mode = switch type {
            case "decodeConfigNBFM": .narrowFM
            case "decodeConfigP25Phase1": .p25Phase1
            default: .unknown
            }
            current?.squelchDBFS = Double(attributeDict["squelch"] ?? "")
        default:
            break
        }
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?
    ) {
        guard elementName == "channel", let draft = current, let frequency = draft.frequencyHz else { return }
        channels.append(RadioChannel(
            system: draft.system,
            site: draft.site,
            name: draft.name,
            frequencyHz: frequency,
            mode: draft.mode,
            enabled: draft.enabled,
            squelchDBFS: draft.squelchDBFS
        ))
        current = nil
    }

    func parser(_ parser: XMLParser, parseErrorOccurred parseError: Error) {
        parserError = parseError
    }
}

private struct ChannelDraft {
    var system: String
    var site: String
    var name: String
    var enabled: Bool
    var frequencyHz: UInt64?
    var mode: ChannelMode = .unknown
    var squelchDBFS: Double?
}
