import SwiftUI

@main
struct SDRPoleApp: App {
    @StateObject private var state = AppState()

    var body: some Scene {
        WindowGroup {
            NavigationSplitView {
                ChannelSidebar()
            } detail: {
                DashboardView()
            }
            .environmentObject(state)
            .frame(minWidth: 980, minHeight: 640)
            .task { await state.bootstrap() }
        }
        .windowStyle(.titleBar)
        .windowToolbarStyle(.unified)

        Settings {
            Form {
                Text("SDR-Pole is under active development.")
                Text("Configuration remains local to this Mac.")
            }
            .padding(24)
            .frame(width: 420)
        }
    }
}
