# Semavia brand and Android ASO assets

Brand: **Semavia: Learn Languages**  
Android application ID: `com.semavia.language.learning.ai` (five package segments)

The Play, Android launcher, iOS, and splash icons use the new 3D AI robot mascot in Semavia teal, coral, and ivory. The existing talking robot used by tutor feedback keeps its original shape and animation alignment; its blue image and animation accents were recolored teal. Other shared illustrations use generated 2.5D language-learning artwork.

## Included assets

- `play-icon-512.png`: 512 × 512 PNG icon, under 1 MB
- `feature-graphic-1024x500.jpg`: 1024 × 500 JPEG, no alpha
- `Screenshots/raw/`: 10 real Samsung S24 screen captures, 1080 × 2340
- `Screenshots/final/`: 8 English ASO images, 1080 × 1920 portrait (9:16)
- `alt-text.en.md`: English alt text for the 8 ASO images
- `android-launcher/`: adaptive icon XML, 3D transparent foreground, monochrome icon, and density PNGs
- `ios-app-icon-1024.png`: 1024 × 1024 iOS icon
- `generated/`: generated visual sources, including the transparent 3D robot logo
- `aso/make_aso_posters.swift`: source used to compose the ASO images from the captured app screens
- `store-listing.en.md`: English Play listing metadata

The poster headline is intentionally compact; the app capture remains the main image. Source captures are retained separately from the store-ready 9:16 compositions.

The Play upload assets were checked against the [Google Play preview asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en): the app icon is 32-bit RGBA PNG at 512 × 512 and below 1 MB; the feature graphic is 1024 × 500 JPEG without alpha; and the 8 portrait screenshots are 1080 × 1920.

Do not put Firebase config, signing files, credentials, or an APK in the ASO archive.
