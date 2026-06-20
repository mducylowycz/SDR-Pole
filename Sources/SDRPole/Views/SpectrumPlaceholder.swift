import SwiftUI

struct SpectrumPlaceholder: View {
    var body: some View {
        Canvas { context, size in
            let background = Path(CGRect(origin: .zero, size: size))
            context.fill(background, with: .linearGradient(
                Gradient(colors: [Color.black, Color(red: 0.03, green: 0.08, blue: 0.13)]),
                startPoint: .zero,
                endPoint: CGPoint(x: 0, y: size.height)
            ))

            var grid = Path()
            for column in 0...10 {
                let x = size.width * CGFloat(column) / 10
                grid.move(to: CGPoint(x: x, y: 0))
                grid.addLine(to: CGPoint(x: x, y: size.height))
            }
            for row in 0...6 {
                let y = size.height * CGFloat(row) / 6
                grid.move(to: CGPoint(x: 0, y: y))
                grid.addLine(to: CGPoint(x: size.width, y: y))
            }
            context.stroke(grid, with: .color(.cyan.opacity(0.12)), lineWidth: 1)

            var trace = Path()
            trace.move(to: CGPoint(x: 0, y: size.height * 0.72))
            for step in 1...120 {
                let fraction = CGFloat(step) / 120
                let peak = exp(-pow((fraction - 0.5) * 20, 2))
                let ripple = sin(fraction * 55) * 3
                let y = size.height * 0.72 - peak * size.height * 0.45 + ripple
                trace.addLine(to: CGPoint(x: fraction * size.width, y: y))
            }
            context.stroke(trace, with: .color(.cyan.opacity(0.5)), lineWidth: 1.5)
        }
        .overlay(alignment: .topLeading) {
            Text("LIVE SPECTRUM · engine milestone 2")
                .font(.caption2.monospaced())
                .foregroundStyle(.cyan.opacity(0.7))
                .padding(10)
        }
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
