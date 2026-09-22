import SwiftUI

/// Brand palette mirrored from the Android app (`ui/theme/Color.kt`), so both apps read as the
/// same product. Orange is the primary identity color, violet is the dark-surface foundation.
extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }

    static let vibeOrangeDeep = Color(hex: 0xE855_10)
    static let vibeOrange = Color(hex: 0xF76B_15) // brand logo orange
    static let vibeOrangeLight = Color(hex: 0xFF8A_3D)
    static let vibeOrangeSoft = Color(hex: 0xFFD3_BC)

    static let vibeVioletDark = Color(hex: 0x1410_1C)
    static let vibeVioletSurface = Color(hex: 0x1E18_28)
    static let vibeVioletCard = Color(hex: 0x2A23_38)
}

/// Full-bleed brand background: a dark violet base with two soft orange glows, used behind
/// every auth/onboarding screen so the app reads as branded from the very first frame.
struct VibeBackground: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [.vibeVioletDark, .vibeVioletSurface],
                startPoint: .top,
                endPoint: .bottom
            )

            Circle()
                .fill(Color.vibeOrange.opacity(0.35))
                .frame(width: 320, height: 320)
                .blur(radius: 90)
                .offset(x: -120, y: -260)

            Circle()
                .fill(Color.vibeOrangeLight.opacity(0.25))
                .frame(width: 260, height: 260)
                .blur(radius: 80)
                .offset(x: 140, y: 320)
        }
        .ignoresSafeArea()
    }
}

/// Frosted-glass card, the SwiftUI equivalent of the Android `GlassCard` component: translucent
/// material, soft border, generous corner radius.
struct GlassCard<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        content
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .strokeBorder(Color.white.opacity(0.15), lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.25), radius: 20, x: 0, y: 10)
    }
}

/// Primary gradient button style, matching the Android `GradientButton`: orange gradient fill,
/// pill shape, dims and disables while loading.
struct VibeButtonStyle: ButtonStyle {
    var isLoading: Bool = false
    var enabled: Bool = true

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                LinearGradient(
                    colors: [.vibeOrange, .vibeOrangeDeep],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .opacity(enabled ? (configuration.isPressed ? 0.85 : 1) : 0.5)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

/// Two-option ALLIEVO/PT selector, styled to match the dark brand background - the native
/// `.segmented` picker renders on a light system material and stands out against `VibeBackground`.
struct RoleToggle: View {
    @Binding var role: String

    var body: some View {
        HStack(spacing: 4) {
            option(label: "Allievo", value: "ALLIEVO")
            option(label: "Personal Trainer", value: "PT")
        }
        .padding(4)
        .background(Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    @ViewBuilder
    private func option(label: String, value: String) -> some View {
        let isSelected = role == value
        Button {
            withAnimation(.easeOut(duration: 0.2)) { role = value }
        } label: {
            Text(label)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(isSelected ? .white : .white.opacity(0.6))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(
                    Group {
                        if isSelected {
                            LinearGradient(colors: [.vibeOrange, .vibeOrangeDeep], startPoint: .leading, endPoint: .trailing)
                        } else {
                            Color.clear
                        }
                    },
                    in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                )
        }
    }
}

/// Text field styling shared by Login/Register, matching the Android `AuthTextField` look:
/// rounded, filled, brand-tinted focus ring.
struct VibeTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(Color.white.opacity(0.12), lineWidth: 1)
            )
            .foregroundStyle(.white)
    }
}
