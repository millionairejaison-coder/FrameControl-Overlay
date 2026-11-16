# FrameControl Overlay (Android System Overlay App)

Just like **Crosshair Pro**, this Android application uses Android's `SYSTEM_ALERT_WINDOW` and `WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE` to draw **10 photography composition guides from FrameControl** directly on top of your screen so you can use them inside **any camera app** (Google Camera, Samsung Camera, Lightroom Mobile, Instagram, or OpenCamera) with **100% touch passthrough**.

## Included FrameControl Overlays
1. **Rule of Thirds** (with power-point intersection markers)
2. **Golden Ratio (Phi Grid 1:0.618:1)**
3. **Fibonacci Golden Spiral** (with 1-tap orientation flip)
4. **Golden Triangle** (diagonal + perpendicular intersections)
5. **Center Crosshair & Symmetry Ring**
6. **Converging Vanishing Lines & Horizon**
7. **Dynamic Diagonals**
8. **S-Curve Flow Guide**
9. **Frame Within a Frame + Safe Zones**
10. **Dutch Angle (15° Roll Grid)**

## How to Get Your `.apk` File (2 Easy Ways)
### Option A: 1-Click Cloud Build on GitHub (No Android Studio Needed)
1. Upload this unzipped folder to a new GitHub repository.
2. Go to the **Actions** tab in your repository -> **Build FrameControl Overlay APK**.
3. Download `app-debug.apk` from the **Artifacts** section and install it on your Android phone.

### Option B: Build Locally in Android Studio
1. Open this folder in **Android Studio**.
2. Wait for Gradle Sync to finish, then click **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
3. Transfer `app/build/outputs/apk/debug/app-debug.apk` to your phone and install.
