import SwiftUI

struct RegisterView: View {
    @EnvironmentObject private var auth: AuthViewModel
    @State private var fullName = ""
    @State private var email = ""
    @State private var password = ""
    @State private var passwordVisible = false
    @State private var role = "ALLIEVO"
    @State private var appeared = false
    var onNavigateToLogin: () -> Void

    var body: some View {
        ZStack {
            VibeBackground()

            ScrollView {
                VStack(spacing: 20) {
                    Spacer().frame(height: 32)

                    Text("Crea il tuo account")
                        .font(.largeTitle.bold())
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                        .opacity(appeared ? 1 : 0)
                        .animation(.easeOut(duration: 0.5), value: appeared)

                    Picker("Sei un...", selection: $role) {
                        Text("Allievo").tag("ALLIEVO")
                        Text("Personal Trainer").tag("PT")
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 24)

                    GlassCard {
                        VStack(spacing: 14) {
                            TextField("Nome completo", text: $fullName)
                                .textFieldStyle(VibeTextFieldStyle())

                            TextField("Email", text: $email)
                                .textContentType(.emailAddress)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                                .textFieldStyle(VibeTextFieldStyle())

                            HStack {
                                Group {
                                    if passwordVisible {
                                        TextField("Password (min. 8 caratteri)", text: $password)
                                    } else {
                                        SecureField("Password (min. 8 caratteri)", text: $password)
                                    }
                                }
                                .textContentType(.newPassword)

                                Button {
                                    passwordVisible.toggle()
                                } label: {
                                    Image(systemName: passwordVisible ? "eye.slash" : "eye")
                                        .foregroundStyle(.white.opacity(0.6))
                                }
                            }
                            .textFieldStyle(VibeTextFieldStyle())

                            if let error = auth.errorMessage {
                                Text(error)
                                    .font(.footnote)
                                    .foregroundStyle(.red.opacity(0.9))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }

                            Button {
                                Task { await auth.signUp(email: email, password: password, fullName: fullName, role: role) }
                            } label: {
                                if auth.isLoading {
                                    ProgressView().tint(.white)
                                } else {
                                    Text("Registrati")
                                }
                            }
                            .buttonStyle(
                                VibeButtonStyle(
                                    isLoading: auth.isLoading,
                                    enabled: !fullName.isEmpty && !email.isEmpty && password.count >= 8
                                )
                            )
                            .disabled(auth.isLoading || fullName.isEmpty || email.isEmpty || password.count < 8)
                            .padding(.top, 4)
                        }
                        .padding(24)
                    }
                    .padding(.horizontal, 24)
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 16)
                    .animation(.easeOut(duration: 0.5).delay(0.1), value: appeared)

                    Button("Hai già un account? Accedi", action: onNavigateToLogin)
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.8))
                        .padding(.bottom, 32)
                }
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .onAppear { appeared = true }
    }
}
