# Vibe Fitness — iOS (starter)

Native SwiftUI app, separate codebase from the Android app but talking to the **same** Supabase
backend (same tables, RLS policies, Edge Functions) — no backend changes needed for this to work.

## Why this exists, and its current scope

The Android app is 100% Kotlin/Jetpack Compose with Android-only dependencies (Room, Hilt,
WorkManager, Firebase). There is no way to "build it for iOS" without a separate app — either a
full Kotlin Multiplatform + Compose Multiplatform migration, or a native iOS app. This is the
native path: a real, buildable pipeline (XcodeGen → Swift Package Manager → Codemagic → TestFlight)
with **Login, Register and a placeholder Home screen** wired to Supabase Auth — proof that the
whole chain works end to end. Everything else the Android app does (workout tracking, PT
dashboard, chat, AI assistant, ...) is not ported yet; that's follow-up work, screen by screen.

**This code has never been compiled.** This sandbox has no macOS/Xcode, so nothing here has run
through the Swift compiler. Codemagic's first build is the first real compile — expect it to
surface a few real errors (an API signature drift in `supabase-swift`, a missing import) that need
a fix-and-repush cycle, same as any new project's first CI run.

## Project structure

```
ios/
  project.yml              XcodeGen spec — the source of truth, NOT the .xcodeproj itself
  VibeFitness/
    App/                    App entry point, Supabase client, Secrets loader, root view
    Auth/                   Login/Register views + view model
    Home/                   Placeholder post-login screen
    Resources/
      Assets.xcassets/      App icon slot (still needs a real 1024×1024 PNG) + accent color
      Secrets.plist.example Template — copy to Secrets.plist locally, never commit the real one
```

The `.xcodeproj` is **generated**, not committed (see `.gitignore`) — `xcodegen generate` builds
it fresh from `project.yml` every time, both locally and in Codemagic. This avoids hand-editing a
fragile `.pbxproj` file, which isn't realistic to keep correct by hand.

## Building locally (needs a Mac with Xcode)

```sh
brew install xcodegen
cd ios
cp VibeFitness/Resources/Secrets.plist.example VibeFitness/Resources/Secrets.plist
# edit Secrets.plist: SUPABASE_ANON_KEY -> your project's anon/publishable key
xcodegen generate
open VibeFitness.xcodeproj
```

## Building on Codemagic

`codemagic.yaml` at the repo root defines the `ios-vibefitness` workflow. One-time setup in the
Codemagic UI:

1. Connect this repository to a Codemagic app.
2. App settings → Environment variables: add `SUPABASE_URL` and `SUPABASE_ANON_KEY` (mark the key
   Secure). The workflow writes these into `Secrets.plist` at build time — nothing sensitive is
   committed to git.
3. As configured today, the workflow builds for the **iOS Simulator only** (no code signing
   needed) — enough to confirm the project compiles. To get a real device build and TestFlight
   upload:
   - Add an App Store Connect API key under Team settings → Code signing identities.
   - Uncomment the `integrations.app_store_connect` block and the archive/export steps in
     `codemagic.yaml`, and swap the "Build (Debug, simulator SDK)" step for the "Archive" +
     "Export IPA" ones (commented alongside it).
   - Add a real 1024×1024 app icon image to `Assets.xcassets/AppIcon.appiconset` — Apple's App
     Store validation rejects a build without one, even though a plain compile doesn't care.

## What's next

Port screens from the Android app one at a time, reusing this same Supabase backend: Welcome
onboarding, Home dashboard, workout tracking, PT client list, chat, and so on. Each Android
ViewModel maps reasonably well to a Swift `ObservableObject` following the same pattern as
`AuthViewModel.swift` here.
