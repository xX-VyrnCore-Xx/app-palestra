import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var auth: AuthViewModel
    @State private var showRegister = false

    var body: some View {
        Group {
            if auth.userId != nil {
                RootTabView()
            } else if showRegister {
                RegisterView(onNavigateToLogin: { showRegister = false })
            } else {
                LoginView(onNavigateToRegister: { showRegister = true })
            }
        }
        .animation(.easeInOut(duration: 0.3), value: auth.userId)
        .animation(.easeInOut(duration: 0.3), value: showRegister)
        .preferredColorScheme(.dark)
    }
}
