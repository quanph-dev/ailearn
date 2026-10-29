import UIKit
import SwiftUI
import Shared
import Network

final class NetworkConnectionMonitor: ObservableObject {
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.example.basekmp.network-monitor")

    init() {
        monitor.pathUpdateHandler = { path in
            DispatchQueue.main.async {
                NetworkConnectionStateKt.updateIosNetworkConnection(isOnline: path.status == .satisfied)
            }
        }
        monitor.start(queue: queue)
        NetworkConnectionStateKt.updateIosNetworkConnection(
            isOnline: monitor.currentPath.status == .satisfied
        )
        NetworkConnectionStateKt.setIosNetworkRefreshHandler(handler: { [weak self] in
            guard let self else { return KotlinUnit() }
            NetworkConnectionStateKt.updateIosNetworkConnection(
                isOnline: self.monitor.currentPath.status == .satisfied
            )
            return KotlinUnit()
        })
    }

    deinit {
        monitor.cancel()
        NetworkConnectionStateKt.setIosNetworkRefreshHandler(handler: nil)
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    @StateObject private var networkMonitor = NetworkConnectionMonitor()

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
