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

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        window = UIWindow(frame: UIScreen.main.bounds)
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
            },
            localization: Localization(
                saved: NSLocalizedString("saved", comment: ""),
                pills: { p in String.localizedStringWithFormat(NSLocalizedString("pills", comment: ""), p) },
                updateCurrentConfig: NSLocalizedString("updateCurrentConfig", comment: ""),
                saveCurrentConfig: NSLocalizedString("saveCurrentConfig", comment: ""),
                home: NSLocalizedString("home", comment: ""),
                addNewPill: NSLocalizedString("addNewPill", comment: ""),
                areYouSureYouWantToRemoveThis: NSLocalizedString("areYouSureYouWantToRemoveThis", comment: ""),
                pillWeight: { p in String(format: NSLocalizedString("pillWeight", comment: ""), p) },
                bottleWeight: { p in String(format: NSLocalizedString("bottleWeight", comment: ""), p) },
                pillWeightCalibration: NSLocalizedString("pillWeightCalibration", comment: ""),
                bottleWeightCalibration: NSLocalizedString("bottleWeightCalibration", comment: ""),
                findPillCounter: NSLocalizedString("findPillCounter", comment: ""),
                retryConnection: NSLocalizedString("retryConnection", comment: ""),
                somethingWentWrongWithTheConnection: NSLocalizedString("somethingWentWrongWithTheConnection", comment: ""),
                connected: NSLocalizedString("connected", comment: ""),
                id: { p in String(format: NSLocalizedString("id_info", comment: ""), p) },
                pillName: NSLocalizedString("pillName", comment: ""),
                save: NSLocalizedString("save", comment: ""),
                pressToStartCalibration: NSLocalizedString("pressToStartCalibration", comment: ""),
                discover: NSLocalizedString("discover", comment: ""),
                enterIpAddress: NSLocalizedString("enterIpAddress", comment: ""),
                manualIP: NSLocalizedString("manualIP", comment: ""),
                discovery: NSLocalizedString("discovery", comment: ""),
                needToConnectPillCounterToWifi: NSLocalizedString("needToConnectPillCounterToWifi", comment: ""),
                connect: NSLocalizedString("connect", comment: ""),
                pleaseWait: NSLocalizedString("pleaseWait", comment: ""),
                ssid: NSLocalizedString("ssid", comment: ""),
                password: NSLocalizedString("password", comment: ""),
                connectPillCounterToWiFi: NSLocalizedString("connectPillCounterToWiFi", comment: ""),
                refreshNetworks: NSLocalizedString("refreshNetworks", comment: ""),
                releaseToRefresh: NSLocalizedString("release_to_refresh", comment: ""),
                refreshing: NSLocalizedString("refreshing", comment: ""),
                pullToRefresh: NSLocalizedString("pull_to_refresh", comment: ""),
                close: NSLocalizedString("close", comment: ""),
                pastDevices: NSLocalizedString("pastDevices", comment: "")
            )
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
