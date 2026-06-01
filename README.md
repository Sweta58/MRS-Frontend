# Medicine Recognition System

An Android application that scans medicine labels, prescriptions, and drug packaging to extract text using on-device OCR — no internet connection required for scanning.

## Features

- **Instant OCR** — point your camera at any medicine label and extract all printed text in seconds
- **Image cropping** — crop precisely to the area you want before scanning
- **Copy to clipboard** — copy the extracted text with one tap
- **Offline** — text recognition runs fully on-device via ML Kit

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |
| OCR Engine | Google ML Kit Text Recognition v16 |
| Image Cropper | android-image-cropper (vanniktech fork) v4.6 |
| UI | Material Design 3 |



## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+
- Android device or emulator running API 24+

### Build & Run

```bash
git clone https://github.com/Sweta58/MRS-Frontend.git
cd MRS-Frontend
./gradlew assembleDebug
```

Install on a connected device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and click **Run**.

### Permissions

The app requests the following permissions at runtime:

| Permission | Reason |
|---|---|
| `CAMERA` | Capture medicine label photos |
| `READ_MEDIA_IMAGES` | Select images from gallery (Android 13+) |

## Project Structure

```
app/src/main/
├── java/com/sweta/mrs/
│   └── MainActivity.java       # Single-activity app entry point
├── res/
│   ├── layout/activity_main.xml
│   ├── drawable/               # Vector icons and gradient background
│   └── values/                 # Colors, strings, themes (Material3 teal)
└── AndroidManifest.xml
```

## Roadmap

- [ ] Backend API integration for medicine identification
- [ ] Scan history
- [ ] Medicine detail screen (dosage, side effects, interactions)
- [ ] Multi-language label support

## License

This project is part of the Medicine Recognition System (MRS) academic project.
