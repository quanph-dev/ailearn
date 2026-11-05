import AppKit
import CoreText
import Foundation

private let canvasSize = 1080
private let canvasHeight = 1920

private struct Poster {
    let filename: String
    let source: String
    let headline: String
}

private let posters = [
    Poster(filename: "01_home.png", source: "03_home.png", headline: "Build a daily\nlearning routine"),
    Poster(filename: "02_learning_path.png", source: "04_learning_path.png", headline: "Follow a guided\nlearning path"),
    Poster(filename: "03_ai_tutor.png", source: "05_ai_tutor.png", headline: "Chat with your\nAI tutor"),
    Poster(filename: "04_vocabulary.png", source: "06_vocabulary.png", headline: "Learn words\nin context"),
    Poster(filename: "05_grammar.png", source: "07_grammar.png", headline: "Practice grammar\nstep by step"),
    Poster(filename: "06_speaking.png", source: "08_speaking.png", headline: "Practice speaking\nwith voice feedback"),
    Poster(filename: "07_video_shadowing.png", source: "09_video_shadowing.png", headline: "Listen, repeat, and\nfollow along"),
    Poster(filename: "08_progress.png", source: "10_progress.png", headline: "Track your\nlearning progress")
]

private func topRect(x: CGFloat, y: CGFloat, width: CGFloat, height: CGFloat) -> NSRect {
    NSRect(x: x, y: CGFloat(canvasHeight) - y - height, width: width, height: height)
}

private func color(_ hex: UInt32, alpha: CGFloat = 1) -> NSColor {
    NSColor(
        calibratedRed: CGFloat((hex >> 16) & 0xff) / 255,
        green: CGFloat((hex >> 8) & 0xff) / 255,
        blue: CGFloat(hex & 0xff) / 255,
        alpha: alpha
    )
}

private func drawText(_ value: String, rect: NSRect, font: NSFont, color textColor: NSColor) {
    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = .left
    paragraph.lineBreakMode = .byWordWrapping
    paragraph.lineSpacing = 3
    NSAttributedString(
        string: value,
        attributes: [
            .font: font,
            .foregroundColor: textColor,
            .paragraphStyle: paragraph
        ]
    ).draw(in: rect)
}

private func build(_ poster: Poster, rawDirectory: URL, outputDirectory: URL, logoURL: URL, fontURL: URL) throws {
    _ = CTFontManagerRegisterFontsForURL(fontURL as CFURL, .process, nil)

    let baseFont = NSFont(name: "PlusJakartaSans-Regular", size: 47) ?? NSFont.systemFont(ofSize: 47, weight: .semibold)
    let headlineFont = NSFontManager.shared.convert(baseFont, toHaveTrait: .boldFontMask)
    let brandBase = NSFont(name: "PlusJakartaSans-Regular", size: 30) ?? NSFont.systemFont(ofSize: 30, weight: .bold)
    let brandFont = NSFontManager.shared.convert(brandBase, toHaveTrait: .boldFontMask)

    let canvas = NSImage(size: NSSize(width: canvasSize, height: canvasHeight))
    canvas.lockFocus()

    color(0xF0F8F5).setFill()
    NSBezierPath(rect: NSRect(x: 0, y: 0, width: canvasSize, height: canvasHeight)).fill()

    color(0xF07864, alpha: 0.07).setFill()
    NSBezierPath(ovalIn: topRect(x: 850, y: 26, width: 260, height: 260)).fill()
    color(0x0F766E, alpha: 0.05).setFill()
    NSBezierPath(ovalIn: topRect(x: -100, y: 1580, width: 340, height: 340)).fill()

    let logo = NSImage(contentsOf: logoURL)!
    logo.draw(in: topRect(x: 78, y: 36, width: 54, height: 54), from: .zero, operation: .sourceOver, fraction: 1)
    drawText("Semavia", rect: topRect(x: 150, y: 43, width: 500, height: 46), font: brandFont, color: color(0x17312E))
    drawText(poster.headline, rect: topRect(x: 80, y: 112, width: 920, height: 122), font: headlineFont, color: color(0x17312E))

    let sourceURL = rawDirectory.appendingPathComponent(poster.source)
    let screenshot = NSImage(contentsOf: sourceURL)!
    let imageRect = topRect(x: 160, y: 250, width: 760, height: 1647)
    NSBezierPath(roundedRect: imageRect, xRadius: 24, yRadius: 24).addClip()
    screenshot.draw(
        in: imageRect,
        from: NSRect(origin: .zero, size: screenshot.size),
        operation: .sourceOver,
        fraction: 1,
        respectFlipped: false,
        hints: [.interpolation: NSImageInterpolation.high]
    )

    canvas.unlockFocus()
    let imageRep = NSBitmapImageRep(data: canvas.tiffRepresentation!)!
    let png = imageRep.representation(using: .png, properties: [:])!
    try png.write(to: outputDirectory.appendingPathComponent(poster.filename), options: .atomic)
}

let root = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
let rawDirectory = root.appendingPathComponent("Screenshots/raw", isDirectory: true)
let outputDirectory = root.appendingPathComponent("Screenshots/final", isDirectory: true)
let logoURL = root.appendingPathComponent("play-icon-512.png")
let fontURL = URL(fileURLWithPath: CommandLine.arguments[2])
try FileManager.default.createDirectory(at: outputDirectory, withIntermediateDirectories: true)
for poster in posters {
    try build(poster, rawDirectory: rawDirectory, outputDirectory: outputDirectory, logoURL: logoURL, fontURL: fontURL)
    print(poster.filename)
}
