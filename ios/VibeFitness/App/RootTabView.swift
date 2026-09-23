import SwiftUI
import UIKit

/// The real post-login shell, replacing the single placeholder screen: a bottom tab bar mirroring
/// the Android app's layout (Home / Schede / Profilo - Chat and l'Assistente follow in the next
/// porting round). Styled to match the Android `AnimatedNavBar`: dark surface, orange selection,
/// generous corner rounding via `.tabViewStyle` + a custom appearance instead of the stock white bar.
struct RootTabView: View {
    init() {
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(Color.vibeVioletSurface)
        appearance.shadowColor = UIColor.white.withAlphaComponent(0.08)

        let selected = UITabBarItemAppearance()
        selected.normal.iconColor = UIColor(Color.white.opacity(0.5))
        selected.normal.titleTextAttributes = [.foregroundColor: UIColor(Color.white.opacity(0.5))]
        selected.selected.iconColor = UIColor(Color.vibeOrange)
        selected.selected.titleTextAttributes = [.foregroundColor: UIColor(Color.vibeOrange)]
        appearance.stackedLayoutAppearance = selected
        appearance.inlineLayoutAppearance = selected
        appearance.compactInlineLayoutAppearance = selected

        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        TabView {
            HomeView()
                .tabItem { Label("Home", systemImage: "house.fill") }

            WorkoutPlansView()
                .tabItem { Label("Schede", systemImage: "dumbbell.fill") }

            ProfileView()
                .tabItem { Label("Profilo", systemImage: "person.crop.circle.fill") }
        }
        .tint(.vibeOrange)
    }
}
