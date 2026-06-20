import Foundation

enum HackRFServiceError: LocalizedError {
    case toolMissing
    case commandFailed(String)
    case noDevice

    var errorDescription: String? {
        switch self {
        case .toolMissing: "hackrf_info was not found. Install the HackRF tools first."
        case .commandFailed(let output): "HackRF probe failed: \(output)"
        case .noDevice: "No HackRF device is connected."
        }
    }
}

struct HackRFService: Sendable {
    let executableURL = URL(fileURLWithPath: "/usr/local/bin/hackrf_info")

    func discover() throws -> HackRFDevice {
        guard FileManager.default.isExecutableFile(atPath: executableURL.path) else {
            throw HackRFServiceError.toolMissing
        }

        let process = Process()
        let pipe = Pipe()
        process.executableURL = executableURL
        process.standardOutput = pipe
        process.standardError = pipe
        try process.run()
        process.waitUntilExit()

        let data = pipe.fileHandleForReading.readDataToEndOfFile()
        let output = String(decoding: data, as: UTF8.self)
        guard process.terminationStatus == 0 else {
            if output.localizedCaseInsensitiveContains("No HackRF boards found") {
                throw HackRFServiceError.noDevice
            }
            throw HackRFServiceError.commandFailed(output.trimmingCharacters(in: .whitespacesAndNewlines))
        }

        let fields = Self.parse(output)
        guard let serial = fields.serial, !serial.isEmpty else { throw HackRFServiceError.noDevice }
        return HackRFDevice(
            serial: serial,
            boardName: fields.boardName ?? "HackRF",
            firmware: fields.firmware ?? "Unknown"
        )
    }

    static func parse(_ output: String) -> (serial: String?, boardName: String?, firmware: String?) {
        var serial: String?
        var boardName: String?
        var firmware: String?
        for rawLine in output.split(separator: "\n") {
            let line = rawLine.trimmingCharacters(in: .whitespaces)
            if line.hasPrefix("Serial number:") {
                serial = value(afterColonIn: line)
            } else if line.hasPrefix("Board ID Number:") {
                boardName = value(afterColonIn: line)
            } else if line.hasPrefix("Firmware Version:") {
                firmware = value(afterColonIn: line)
            }
        }
        return (serial, boardName, firmware)
    }

    private static func value(afterColonIn line: String) -> String? {
        guard let index = line.firstIndex(of: ":") else { return nil }
        return String(line[line.index(after: index)...]).trimmingCharacters(in: .whitespaces)
    }
}
