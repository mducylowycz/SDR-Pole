import Foundation

struct HackRFDevice: Equatable, Sendable {
    var serial: String
    var boardName: String
    var firmware: String
}
