import Foundation

@MainActor
final class AppState: ObservableObject {
    @Published var channels: [RadioChannel] = []
    @Published var selectedChannelID: RadioChannel.ID?
    @Published var hackRF: HackRFDevice?
    @Published var deviceStatus = "Checking HackRF…"
    @Published var importStatus = "Loading SDRTrunk playlist…"
    @Published var isReceiving = false

    private let playlistImporter = SDRTrunkPlaylistImporter()
    private let hackRFService = HackRFService()

    var selectedChannel: RadioChannel? {
        channels.first { $0.id == selectedChannelID }
    }

    var enabledChannels: [RadioChannel] {
        channels.filter(\.enabled)
    }

    func bootstrap() async {
        importDefaultPlaylist()
        await refreshHardware()
    }

    func importDefaultPlaylist() {
        let url = FileManager.default.homeDirectoryForCurrentUser
            .appending(path: "SDRTrunk/playlist/default.xml")
        do {
            channels = try playlistImporter.importPlaylist(at: url)
            selectedChannelID = channels.first(where: \.enabled)?.id ?? channels.first?.id
            importStatus = "Imported \(channels.count) channels from SDRTrunk"
        } catch {
            importStatus = error.localizedDescription
        }
    }

    func refreshHardware() async {
        deviceStatus = "Checking HackRF…"
        do {
            let service = hackRFService
            let device = try await Task.detached { try service.discover() }.value
            hackRF = device
            deviceStatus = "Connected"
        } catch {
            hackRF = nil
            deviceStatus = error.localizedDescription
        }
    }

    func toggleReceiving() {
        // The UI/state seam is intentionally real, while IQ/DSP implementation
        // lands in the next milestone. Never claim reception before that exists.
        isReceiving = false
    }
}
