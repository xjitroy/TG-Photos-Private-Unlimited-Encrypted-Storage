# TG Photos design direction

All 14 embedded PNGs were inspected, including two overall boards and individual login/gallery/files/vault/
settings/player concepts. They guide hierarchy and flows only. No screenshot/photo/logo was shipped as an asset.

Original Compose direction: spacious headings, quiet blue-violet accents, rounded 12/20/28dp shapes,
tonal status cards, 8dp spacing rhythm, adaptive grids and original generated vector landscapes.
Fallback light palette: primary #6356D8, background #FAF8FF; dark: primary #C8BEFF, background #111119.
Material You uses Android 12+ wallpaper colors when enabled; theme setting persists and follows System by default.

Navigation: four bottom destinations only: Photos (Cloud), Albums, Files, Private Vault.
Settings is reached through the top-right overflow. Initial screen previews login; Explore explicitly enters
the design catalogue without authenticating. Returning/back navigation never creates an account/session.

Implemented preview interactions: photo filtering/swipe illustrations, album policy example switch,
file sample search/filter/sort/selection, theme switching/dynamic colors, country picker and login-step selection.
Phone/OTP/password entry, vault unlock/biometrics, uploads, editing and destructive file actions stay disabled.
Every sample page shows a design-preview banner. Settings detail dialogs distinguish planned integrations.

Accessibility source baseline: Material default 48dp targets, labeled icon actions, content descriptions,
text scaling without fixed-height text containers, vertical scrolling, adaptive grids and RTL support.
Manual TalkBack, 200% font size, landscape/small-screen, wallpaper contrast, keyboard focus and reduced-motion
checks remain pending. No emulator screenshot or runtime rendering is claimed.

Material Expressive APIs were removed from stable 1.4; pin `1.5.0-alpha03` with explicit opt-in for the
requested expressive theme, based on the official release notes. This prerelease dependency must be compiled
and reviewed before production; it is isolated to UI modules. Pin remaining foundational versions rather than
tracking arbitrary upstream heads. Do not describe the entire dependency graph as verified until resolution succeeds.
