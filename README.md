# BaseKMP

Mẫu Kotlin Multiplatform dùng Compose cho Android và iOS. Luồng hiện có gồm splash, chọn ngôn ngữ, onboarding và Home khung.

## Định danh mặc định

- Tên Gradle project: `BaseKMP`
- Android namespace: `com.semavia.ailearn`
- Android application ID: `com.semavia.language.learning.ai`
- iOS product name/bundle ID: `BaseKMP` / `com.example.basekmp`
- Tên hiển thị Android: `BaseKMP`

Khi tạo app mới, đổi các định danh này theo app mới trước khi đăng ký Firebase, quảng cáo hoặc signing.

## Firebase (tùy chọn)

Mẫu giữ SDK và điểm tích hợp Analytics, Remote Config, Crashlytics, Realtime Database. Firebase chỉ được khởi tạo khi cấu hình đúng của app hiện diện. Không chép cấu hình từ dự án khác.

- Android: đặt `google-services.json` riêng trong `androidApp/`.
- iOS: đặt `GoogleService-Info.plist` riêng trong `iosApp/iosApp/` và bảo đảm Bundle ID khớp.

Hai file này được Git ignore để tránh gắn nhầm Firebase project khi sao chép mẫu. Cấp chúng riêng trên máy/CI của app.

## Ads và Prox SDK (Android)

Mẫu giữ lớp tích hợp Prox, nhưng không có AdMob App ID hoặc placement ID của một app cụ thể. Thêm cấu hình quảng cáo riêng trước khi bật khởi tạo ads. Mặc định startup không khởi tạo placement hay tải/hiển thị quảng cáo; `MyAdsUtils.initializeAfterConsent()` là điểm vào sau consent.

Prox SDK cần Maven repository nội bộ. Không đặt thông tin đăng nhập trong project. Nếu có quyền truy cập, cấu hình trong `~/.gradle/gradle.properties` trên máy dựng:

```properties
artifactory_contextUrl=https://<artifactory-host>/repository/maven-releases/
artifactory_user=<your-user>
artifactory_password=<your-password>
```

Repository trong Gradle chỉ chấp nhận HTTPS. Không đưa mật khẩu vào file dự án, log hoặc Git.

## Sao chép mẫu

1. Đổi Android namespace, `applicationId`, app label và package trong `androidApp/src/` và `shared/src/`.
2. Đổi iOS product name và bundle ID trong `iosApp/Configuration/Config.xcconfig`.
3. Tạo Firebase app riêng rồi đặt các file cấu hình cục bộ nếu cần Firebase.
4. Thêm AdMob App ID và cấu hình signing riêng nếu app dùng các tính năng đó.
5. Mở bản sao trong Android Studio/Xcode sau khi đổi định danh.

`local.properties`, IDE state và build cache không nằm trong mẫu; IDE sẽ tạo lại khi mở project.

## Các lệnh kiểm tra có sẵn

```sh
./gradlew :androidApp:assembleDebug
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:lintDebug
./gradlew :shared:iosSimulatorArm64Test
xcodebuild -resolvePackageDependencies -project iosApp/iosApp.xcodeproj -scheme iosApp
```
