import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var auth: AuthViewModel
    @State private var email = ""
    @State private var password = ""
    var onNavigateToRegister: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Spacer()

            Text("Vibe Fitness")
                .font(.largeTitle.bold())
            Text("Bentornato. Ogni ripetizione conta.")
                .foregroundStyle(.secondary)

            VStack(spacing: 12) {
                TextField("Email", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .textFieldStyle(.roundedBorder)

                SecureField("Password", text: $password)
                    .textContentType(.password)
                    .textFieldStyle(.roundedBorder)

                if let error = auth.errorMessage {
                    Text(error)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                Button {
                    Task { await auth.signIn(email: email, password: password) }
                } label: {
                    if auth.isLoading {
                        ProgressView().frame(maxWidth: .infinity)
                    } else {
                        Text("Accedi").frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(auth.isLoading)
            }
            .padding(.horizontal, 24)

            Button("Non hai un account? Registrati", action: onNavigateToRegister)
                .font(.footnote)

            Spacer()
        }
        .padding()
    }
}
