import SwiftUI

struct ChannelSidebar: View {
    @EnvironmentObject private var state: AppState

    var body: some View {
        List(selection: $state.selectedChannelID) {
            ForEach(groupedSystems, id: \.0) { system, channels in
                Section(system) {
                    ForEach(channels) { channel in
                        VStack(alignment: .leading, spacing: 3) {
                            HStack {
                                Circle()
                                    .fill(channel.enabled ? Color.green : Color.gray)
                                    .frame(width: 7, height: 7)
                                Text(channel.name)
                                    .lineLimit(1)
                            }
                            Text("\(channel.frequencyMHz) · \(channel.mode.rawValue)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .tag(channel.id)
                    }
                }
            }
        }
        .navigationTitle("Channels")
        .frame(minWidth: 270)
    }

    private var groupedSystems: [(String, [RadioChannel])] {
        Dictionary(grouping: state.channels, by: \.system)
            .sorted { $0.key.localizedStandardCompare($1.key) == .orderedAscending }
    }
}
