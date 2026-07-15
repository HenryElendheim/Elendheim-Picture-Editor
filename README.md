# Elendheim Picture Editor

A private, non-destructive photo editor for Android. Adjust and filter your
photos, crop and rotate them with social-ready presets, then save the result to
your gallery as a brand new file. Your originals are never touched, and the app
has no internet access at all.

Part of the Elendheim suite. Dark first, native Kotlin and Jetpack Compose.

## What it does

- **Adjust and filter** with live sliders (exposure, brightness, contrast,
  saturation, warmth, tint, hue), a set of one-tap premade looks, and a vignette.
  Save any look you like as your own reusable filter.
- **Crop, rotate and flip** with aspect presets for Instagram, Stories, Reels,
  TikTok, YouTube and the classic photo ratios.
- **Export to your gallery** as a new JPEG or PNG. The original photo is left
  exactly as it was.

## Why non-destructive

An edit is a small recipe of settings, not baked-in pixels. The preview shows
that recipe live, and export replays the very same recipe onto a fresh copy of
the photo at full size. That is what makes undo, reusable filters and safe
export all fall out for free.

## Privacy

There is no `INTERNET` permission anywhere in the manifest. The app cannot phone
home. Everything happens on your device, and your filters are yours to export,
import and share as plain files.

## Accessibility

Built in from the start, not bolted on: high-contrast mode, reduce motion,
larger tap targets, a fine slider mode for precise control, value announcements
for screen readers, and nothing that depends on colour alone.

## Building

The app builds with Gradle and the Android Gradle Plugin.

```
./gradlew assembleRelease
```

The release APK lands in `app/build/outputs/apk/release/`. Continuous
integration builds the same APK on every push and attaches it to a GitHub
Release whenever a version tag (for example `v1.0`) is pushed.

## License

Released under the MIT License. See [LICENSE](LICENSE).
