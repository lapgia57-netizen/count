# Đếm ngày đêm — Android

Ứng dụng Kotlin + Jetpack Compose để lưu và theo dõi các mốc thời gian. Hỗ trợ đếm từ một thời điểm hoặc đếm ngược đến một thời điểm, đăng nhập email/mật khẩu (không hỗ trợ đăng ký trong ứng dụng), đồng bộ dữ liệu theo tài khoản bằng Firebase Realtime Database, và phân tích riêng thời lượng ban ngày (06:00–18:00) / ban đêm (18:00–06:00) cho bộ đếm xuôi.

## Mở và chạy

- Mở thư mục này bằng Android Studio.
- Dùng JDK 17 và Android SDK Platform 35.
- Chạy cấu hình `app` trên thiết bị hoặc emulator Android 8.0 (API 26) trở lên.
- Bật **Email/Password** trong Firebase Authentication, tạo tài khoản người dùng trong Firebase Console và tạo Realtime Database. Người dùng đăng nhập bằng tài khoản đã tạo sẵn.
- Cấu hình Firebase của dự án đã được khai báo mặc định trong `app/build.gradle.kts`, nên có thể mở app và build trực tiếp mà không cần tạo file cấu hình hay GitHub Secrets. Nếu cần dùng cấu hình khác, có thể ghi đè bằng `firebase.properties` (mẫu ở `firebase.properties.example`), Gradle properties hoặc biến môi trường.
- Áp dụng [database.rules.json](./database.rules.json) trong tab Rules của Realtime Database. Mỗi tài khoản chỉ đọc/ghi được `/users/{uid}` của chính mình.

`authDomain` không được dùng bởi Firebase Android SDK.

## Build APK tự động

Workflow `.github/workflows/android.yml` build APK debug khi push lên nhánh `main`/`master`, mở pull request hoặc chạy thủ công từ GitHub Actions. Tải `app-debug.apk` trong mục Artifacts của lần chạy workflow. APK debug phù hợp để cài thử; phát hành lên Google Play cần cấu hình ký APK/AAB riêng.

Workflow build APK dùng cấu hình Firebase mặc định trong mã nguồn và không yêu cầu GitHub Actions secrets. Firebase client config được đóng gói trong APK; bảo vệ dữ liệu bằng Realtime Database Rules, không dựa vào việc giấu API key.
