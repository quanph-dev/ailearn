# Báo cáo tiến độ rà soát và hiệu chỉnh Engla → AiLearn

Ngày cập nhật: 2026-10-29
Repo: `/Volumes/data/AndroidStudioProjects/ailearn`
Tài nguyên gốc: `/Volumes/data/resource/Engla_com.english.learninglanguage`
Trạng thái: **đang triển khai theo từng luồng; chưa xác nhận hoàn tất fidelity giao diện toàn app**.

> Các câu chỉ thị nằm trong tài liệu cũ được xem là bối cảnh lịch sử, không tự động trở thành chỉ thị mới. Kết luận dưới đây dựa trên source/resource và các phép kiểm tra ghi rõ; không dùng nhãn “100%” nếu chưa có bằng chứng đối chiếu đủ.

## Tóm tắt tiến độ

Đợt này ưu tiên launch flow, lưu state thật và hành vi core thay cho số liệu/điểm tự sinh. Bản source trước các chỉnh sửa khôi phục LLM auth, bổ sung first-open locale strings và hiệu chỉnh Splash theo layout launcher đã biên dịch cho Android và iOS Simulator; chưa build lại sau các chỉnh sửa mới này. Chưa chạy bản source hiện tại trên thiết bị và chưa chụp screenshot đối chiếu. Thiết bị Android duy nhất đang có cài cùng applicationId, không có emulator; không ghi đè ứng dụng/dữ liệu đang có. Chưa chạy test suite; UI fidelity và runtime vẫn chưa xác nhận.

| Khu vực | Trạng thái hiện tại | Việc còn thiếu |
|---|---|---|
| Splash + launch | APK gốc mở `SplashActivity` của FirstOpen SDK; layout ngoài có logo 104dp, title 20sp, mô tả 14sp, progress bar 6dp ở đáy. Compose splash đã chỉnh theo cấu trúc này bằng asset logo/text gốc; route Splash → language setup → FirstOpen → profile wizard → Home đã nối. | XML gốc dùng Lottie tự chạy/vòng lặp; Compose hiện dùng logo tĩnh do chưa xác định animation SDK nạp. Banner gốc mặc định ẩn; chưa so runtime/safe area/thời lượng. |
| FirstOpen/onboarding | Có 3 intro pages, survey chọn ngôn ngữ học và wizard hồ sơ; wizard lưu/khôi phục từng bước, survey hoàn tất lưu vào repository. Artwork/icons/colors được đối chiếu resource; đã thêm chuỗi gốc cho it/ru/tr/ja/zh/ko/th và cho subtitle survey tự wrap. Urdu tiếp tục fallback tiếng Anh như resource APK gốc. | Nếu process chết trước khi survey hoàn tất, intro quay về trang đầu và lựa chọn survey chưa lưu; cần quyết định có khôi phục đúng trang hay theo behavior gốc. Chưa chạy fresh install/force-stop hoặc đo layout/RTL trên thiết bị. |
| Home | Hero chuyển về “Speak with AI” với source artwork/CTA; thêm Learning Path card và bỏ từ “Confidence” mẫu. | Thứ tự/nhánh layout Home gốc và các card còn lại cần chốt bằng đối chiếu runtime. |
| Practice/Grammar/Vocabulary/Speaking/Path | Practice/path assets JSON khớp; các quiz ghi kết quả theo thao tác; Android STT và Android/iOS TTS đã nối. Điểm lịch sử nay quy đổi đúng theo tổng câu; practice hoàn tất ghi streak. | iOS STT chưa có; điểm phát âm là text similarity, chưa phải phoneme scoring; chưa kiểm tra mic/TTS thật. |
| Plan/progress/history | Tiến độ, hồ sơ, history và Plan lưu local; đã bỏ seed/auto-complete. Today's Lesson và AI Tutor chỉ ghi metadata session sau hoạt động thật, không lưu transcript; item không có score không bị hiện thành 0%. | Cần so thuật toán unlock/streak với app gốc, kiểm tra restart/ngày mới; minutes chưa đo ở mọi loại practice. |
| Tutor/network | Bỏ câu trả lời tích cực giả; lỗi mạng hiện thông báo retry chung. **Đã khôi phục header xác thực và key cũ** theo xác nhận của người dùng; URL endpoint giữ nguyên. Request gửi Content-Type cùng custom auth header. | Chưa chạy build hoặc request runtime sau khi khôi phục; key nằm trong binary client và có thể bị trích xuất. Cần xác nhận backend auth/data retention và kế hoạch rotate key trước release. |
| Shadowing | Chấm từ transcript và không tự hoàn thành khi chưa có lượt nói. | Chưa có video/audio player và subtitle sync như app gốc. |
| Settings/billing/legal | Gỡ banner “Pro Unlocked” không có entitlement và bỏ tùy chọn Speech Rate không có tác dụng; các thuộc tính hồ sơ đã lưu. | Billing/Premium, Rate Us/Support còn chưa nối; policy có tuyên bố chat được lưu local và voice xử lý trên thiết bị nhưng chưa khớp/được xác nhận theo hành vi backend. |
| Bảo mật | Header/key cũ được khôi phục trong client theo xác nhận riêng của người dùng; bypass test vẫn tắt; báo cáo không ghi giá trị key. | Key nằm trong binary và đã xuất hiện trong output của phiên làm việc này; cần rotate ở backend trước khi phát hành hoặc chuyển sang xác thực server-side. |

## Chi tiết các sửa đổi source trong đợt này

- `BaseAppRepository.kt`, `BaseAppViewModel.kt`, `App.kt`: nối route startup, FirstOpen, survey, profile onboarding, language settings và lưu completion.
- `SplashScreen.kt`: thay bố cục robot 148dp/progress giữa màn bằng cấu trúc launcher FirstOpen gốc: logo 104dp, title Inter 20sp, mô tả 14sp và progress indeterminate 6dp sát đáy. Logo hiện tĩnh; animation Apero chưa được port.
- `FirstOpenScreens.kt`, `OnboardingModels.kt`, `OnboardingScreen.kt`: tách intro/survey khỏi wizard hồ sơ; giữ state wizard và hỗ trợ khôi phục. Thêm resource gốc cho 7 locale UI còn thiếu; Urdu giữ fallback theo APK gốc; subtitle survey được phép wrap.
- `AndroidAppRepository.kt`, `IosAppRepository.kt`: lưu survey, onboarding progress, PlanProgress và snapshot learning progress.
- `UserPrefs.kt`, `PlanProgress.kt`, `PlanModels.kt`: loại seed/demo, dùng thời gian epoch, lưu snapshot progress local, không tự cộng thưởng hoặc đánh dấu hoàn thành giả.
- `PlatformSpeechRecognition.*`, `SpeechCaptureMic.kt`, `PlatformTextToSpeech.*`: nối hành vi mic/TTS theo nền tảng; iOS STT vẫn chưa khả dụng.
- `SpeakingScreens.kt`, `VocabScreens.kt`, `PathLessonScreen.kt`, `VideoShadowingScreens.kt`, `TodayLessonScreen.kt`: điểm/lượt hoàn thành dựa trên thao tác hoặc transcript thực; không tự chấm điểm bằng đáp án mẫu.
- `HomeScreen.kt`: dùng nội dung hero Home có trong source Engla, mở CTA speaking và tạo card Learning Path; gỡ thẻ từ vựng mẫu hardcoded.
- `LearningItem.kt`, `ProgressScreen.kt`, `ProgressDetailScreen.kt`, `LearningHistoryScreen.kt`: tính phần trăm đúng theo `score/total`, hỗ trợ session không có điểm.
- `TodayLessonScreen.kt`, `AiTutorScreen.kt`: không giả lập câu trả lời thành công khi endpoint lỗi; session Today Lesson/AI Tutor lưu metadata, không lưu hội thoại.
- `App.kt`: bỏ network gate toàn app theo mô hình Local-First mô tả trong TECH_SPEC; lỗi mạng để tại thao tác cần mạng.
- `SettingsScreen.kt`, `AiTutorScreen.kt`: bỏ trạng thái Pro giả/no-op speech rate và đồng bộ tùy chọn tutor persona.
- `AppConfig.kt`, `LlmApi.kt`: khôi phục custom auth header và key cũ theo xác nhận của người dùng; URL giữ nguyên, các bypass vẫn tắt. Chưa chạy build hoặc request runtime sau chỉnh sửa.
- `Platform.ios.kt`: cung cấp timestamp epoch cho state/time tracking trên iOS.

## Kiểm chứng đã chạy

- `./gradlew :shared:compileAndroidMain :androidApp:compileDebugKotlin` — **BUILD SUCCESSFUL ở revision trước** các chỉnh sửa khôi phục LLM auth và bổ sung first-open locale strings.
- `./gradlew :shared:compileKotlinIosSimulatorArm64` — **BUILD SUCCESSFUL ở revision trước** các chỉnh sửa trên.
- Chưa compile lại, chạy test suite, cài/chạy app trên thiết bị, build APK/AAB hoặc gửi request runtime sau các chỉnh sửa mới.

## Việc tiếp theo theo thứ tự màn hình/ưu tiên

1. Chụp và đối chiếu lần lượt Splash, language picker, 3 intro pages, survey và từng bước profile wizard với source/ảnh gốc; chỉnh spacing, typography, icon, safe area, back/skip.
2. Đối chiếu Home, bottom navigation, Practice, Vocabulary, Grammar, Speaking, Path, Plan, Tutor, Progress, History và Settings theo đúng điều hướng/back/state từng màn; xử lý số phút cho mọi loại hoạt động.
3. Đối chiếu thứ tự/cấu trúc Home theo cấu hình layout gốc; xác định nguồn video/audio gốc và triển khai player thật cho Shadowing.
4. Hoàn thiện iOS speech recognition; xác minh TTS/STT và quyền microphone trên thiết bị.
5. Khôi phục luồng Premium/billing, Rate Us và Support theo source gốc; rà chính sách riêng tư với hành vi thật của backend.
6. Đối chiếu Plan/streak/progress với thuật toán gốc; xác minh lưu state sau process restart và qua ngày.
7. Rotate key cũ phía backend do đã xuất hiện trong output phiên; sau đó kiểm tra request LLM runtime, xác nhận data retention và ưu tiên chuyển sang auth server-side trước release.

## Giới hạn còn lại

Tài liệu gốc hiện có source JADX, resource APKTool và các báo cáo kỹ thuật; chưa có APK chạy gốc/ảnh tham chiếu đầy đủ để chứng nhận pixel-by-pixel. Biên dịch chỉ xác nhận source compile, không thay thế kiểm tra runtime. Các hạng mục chưa hoàn tất được ghi riêng ở trên; không coi trạng thái compile là hoàn tất sản phẩm.
