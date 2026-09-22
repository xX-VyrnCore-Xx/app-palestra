import SwiftUI

struct RegisterView: View {
    @EnvironmentObject private var auth: AuthViewModel
    @State private var fullName = ""
    @State private var email = ""
    @State private var password = ""
    @State private var role = "ALLIEVO"
    var onNavigateToLogin: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text("Crea il tuo account")
                    .font(.largeTitle.bold())
                    .padding(.top, 32)

                Picker("Sei un...", selection: $role) {
                    Text("Allievo").tag("ALLIEVO")
                    Text("Personal Trainer").tag("PT")
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 24)

                VStack(spacing: 12) {
                    TextField("Nome completo", text: $fullName)
                        .textFieldStyle(.roundedBorder)
                    TextField("Email", text: $email)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .textFieldStyle(.roundedBorder)
                    SecureField("Password (min. 8 caratteri)", text: $password)
                        .textContentType(.newPassword)
                        .textFieldStyle(.roundedBorder)

                    if let error = auth.errorMessage {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }

                    Button {
                        Task { await auth.signUp(email: email, password: password, fullName: fullName, role: role) }
                    } label: {
                        if auth.isLoading {
                            ProgressView().frame(maxWidth: .infinity)
                        } else {
                            Text("Registrati").frame(maxWidth: .infinity)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(auth.isLoading)
                }
                .padding(.horizontal, 24)

                Button("Hai già un account? Accedi", action: onNavigateToLogin)
                    .font(.footnote)
                    .padding(.bottom, 32)
            }
        }
    }
}
