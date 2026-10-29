import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        configureFirebaseIfAvailable()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private func configureFirebaseIfAvailable() {
        guard
            let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
            let values = NSDictionary(contentsOfFile: path) as? [String: Any],
            values["BUNDLE_ID"] as? String == Bundle.main.bundleIdentifier,
            let options = FirebaseOptions(contentsOfFile: path)
        else {
            return
        }

        if FirebaseApp.app() == nil {
            FirebaseApp.configure(options: options)
        }
    }
}
