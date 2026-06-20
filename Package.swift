// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "SDR-Pole",
    platforms: [.macOS(.v14)],
    products: [
        .executable(name: "SDR-Pole", targets: ["SDRPole"])
    ],
    targets: [
        .executableTarget(name: "SDRPole")
    ]
)
