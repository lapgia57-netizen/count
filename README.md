# Đếm ngày đêm — Android

Ứng dụng Kotlin + Jetpack Compose để lưu và theo dõi các mốc thời gian. Hỗ trợ đếm từ một thời điểm hoặc đếm ngược đến một thời điểm, đăng nhập email/mật khẩu, đồng bộ dữ liệu theo tài khoản bằng Firebase Realtime Database, và phân tích riêng thời lượng ban ngày (06:00–18:00) / ban đêm (18:00–06:00) cho bộ đếm xuôi.

## Mở và chạy

- Mở thư mục này bằng Android Studio.
- Dùng JDK 17 và Android SDK Platform 35.
- Chạy cấu hình `app` trên thiết bị hoặc emulator Android 8.0 (API 26) trở lên.
- Bật **Email/Password** trong Firebase Authentication và tạo Realtime Database.
- Sao chép `firebase.properties.example` thành `firebase.properties`, sau đó điền `apiKey`, `appId`, `projectId`, `databaseURL`, `storageBucket` và `messagingSenderId` từ Firebase config. `firebase.properties` đã được bỏ qua khỏi Git.
- Áp dụng [database.rules.json](./database.rules.json) trong tab Rules của Realtime Database. Mỗi tài khoản chỉ đọc/ghi được `/users/{uid}` của chính mình.

`authDomain` không được dùng bởi Firebase Android SDK.

## Build APK tự động

Workflow `.github/workflows/android.yml` build APK debug khi push lên nhánh `main`/`master`, mở pull request hoặc chạy thủ công từ GitHub Actions. Tải `app-debug.apk` trong mục Artifacts của lần chạy workflow. APK debug phù hợp để cài thử; phát hành lên Google Play cần cấu hình ký APK/AAB riêng.

Để APK từ GitHub Actions kết nối Firebase, thêm các GitHub Actions repository secrets: `FIREBASE_API_KEY`, `FIREBASE_APP_ID`, `FIREBASE_PROJECT_ID`, `FIREBASE_DATABASE_URL`, `FIREBASE_STORAGE_BUCKET`, `FIREBASE_SENDER_ID`.
