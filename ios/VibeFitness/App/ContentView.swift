import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var auth: AuthViewModel
    @State private var showRegister = false

    var body: some View {
        Group {
            if auth.userId != nil {
                HomeView()
            } else if showRegister {
                RegisterView(onNavigateToLogin: { showRegister = false })
            } else {
                LoginView(onNavigateToRegister: { showRegister = true })
            }
        }
    }
}
