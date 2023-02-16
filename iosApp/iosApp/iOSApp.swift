import UIKit
import common
import GCXMulticastDNSKit
import RxGCXMulticastDNSKit
import RxSwift
import RxBlocking
import RxCocoa
import Bonjour

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    var window: UIWindow?
    
    let disposeBag = DisposeBag()

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        window = UIWindow(frame: UIScreen.main.bounds)
        print("Hello World")
        let mainViewController = PlatformKt.MainViewController(
            actionOnDiscover: { action in
                Task {
                    var ipArray: Array<IpInfo> = []
                    let client = NetServiceManager()
                    client.log = { print("NetService:", $0) }
                    
                    // start discovery
                    let stream = client.discoverServices(of: .http, in: .local)
                    Task {
                        try await Task.sleep(seconds: 20)
                        stream.stop()
                    }
                    
                    // store scanned services
                    var services = Set<Service>()
                    for try await service in stream {
                        services.insert(service)
                    }
                    
                    // resolve each
                    for service in services {
                        do {
                            let addresses = try await client.resolve(service, timeout: 10.0)
                            if let hostName = await client.hostName(for: service) {
                                print("Host Name:", hostName)
                            }
                            addresses.forEach {
                                print(service.name, $0)
                            }
                            ipArray.append(IpInfo(name: service.name, host: addresses.first?.address.rawValue ?? ""))
                            if let txtRecord = await client.txtRecord(for: service) {
                                print("TXT Record:", txtRecord)
                            }
                        } catch {
                            
                        }
                    }
                    action(ipArray)
                }
            }
        )
        window?.rootViewController = mainViewController
        window?.makeKeyAndVisible()
        return true
    }
}

extension Task where Success == Never, Failure == Never {
    static func sleep(seconds: Double) async throws {
        let duration = UInt64(seconds * 1_000_000_000)
        try await Task.sleep(nanoseconds: duration)
    }
}
