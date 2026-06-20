import Foundation

enum ChannelMode: String, Codable, CaseIterable, Sendable {
    case narrowFM = "NFM"
    case p25Phase1 = "P25 Phase 1"
    case unknown = "Unknown"
}

struct RadioChannel: Identifiable, Codable, Hashable, Sendable {
    let id: UUID
    var system: String
    var site: String
    var name: String
    var frequencyHz: UInt64
    var mode: ChannelMode
    var enabled: Bool
    var squelchDBFS: Double?

    init(
        id: UUID = UUID(),
        system: String,
        site: String,
        name: String,
        frequencyHz: UInt64,
        mode: ChannelMode,
        enabled: Bool,
        squelchDBFS: Double? = nil
    ) {
        self.id = id
        self.system = system
        self.site = site
        self.name = name
        self.frequencyHz = frequencyHz
        self.mode = mode
        self.enabled = enabled
        self.squelchDBFS = squelchDBFS
    }

    var frequencyMHz: String {
        String(format: "%.5f MHz", Double(frequencyHz) / 1_000_000)
    }
}
