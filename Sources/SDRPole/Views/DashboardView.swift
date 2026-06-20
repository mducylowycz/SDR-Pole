import SwiftUI

struct DashboardView: View {
    @EnvironmentObject private var state: AppState

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            header
            SpectrumPlaceholder()
                .frame(minHeight: 230)
            channelDetails
            Spacer(minLength: 0)
            statusBar
        }
        .padding(24)
        .background(Color(nsColor: .windowBackgroundColor))
        .navigationTitle("SDR-Pole")
        .toolbar {
            ToolbarItemGroup {
                Button {
                    Task { await state.refreshHardware() }
                } label: {
                    Label("Refresh Hardware", systemImage: "arrow.clockwise")
                }
                Button(action: state.toggleReceiving) {
                    Label("Start", systemImage: "play.fill")
                }
                .disabled(state.hackRF == nil || state.selectedChannel == nil)
                .help("The receive/DSP engine arrives in milestone 2")
            }
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image(systemName: "antenna.radiowaves.left.and.right")
                .font(.system(size: 34))
                .foregroundStyle(.cyan)
            VStack(alignment: .leading) {
                Text("SDR-Pole")
                    .font(.largeTitle.bold())
                Text("Local-first radio monitoring workbench")
                    .foregroundStyle(.secondary)
            }
            Spacer()
            statusPill
        }
    }

    private var statusPill: some View {
        HStack(spacing: 7) {
            Circle()
                .fill(state.hackRF == nil ? Color.red : Color.green)
                .frame(width: 9, height: 9)
            Text(state.deviceStatus)
                .font(.callout.weight(.medium))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(.thinMaterial, in: Capsule())
    }

    @ViewBuilder
    private var channelDetails: some View {
        if let channel = state.selectedChannel {
            GroupBox {
                Grid(alignment: .leading, horizontalSpacing: 28, verticalSpacing: 10) {
                    GridRow { Text("Channel").foregroundStyle(.secondary); Text(channel.name).bold() }
                    GridRow { Text("System").foregroundStyle(.secondary); Text("\(channel.system) · \(channel.site)") }
                    GridRow { Text("Frequency").foregroundStyle(.secondary); Text(channel.frequencyMHz).monospacedDigit() }
                    GridRow { Text("Mode").foregroundStyle(.secondary); Text(channel.mode.rawValue) }
                    GridRow { Text("Squelch").foregroundStyle(.secondary); Text(channel.squelchDBFS.map { "\($0, specifier: "%.0f") dBFS" } ?? "Automatic") }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(8)
            }
        } else {
            ContentUnavailableView("No Channel Selected", systemImage: "waveform.slash")
        }
    }

    private var statusBar: some View {
        HStack {
            Label(state.importStatus, systemImage: "tray.and.arrow.down")
            Spacer()
            if let device = state.hackRF {
                Text("\(device.boardName) · \(device.serial.suffix(8)) · FW \(device.firmware)")
                    .monospaced()
            }
        }
        .font(.caption)
        .foregroundStyle(.secondary)
    }
}
