# Báo cáo tiến độ rà soát và hiệu chỉnh Engla → AiLearn

Ngày cập nhật: 2026-10-30
Repo: `/Volumes/data/AndroidStudioProjects/ailearn`
Tài nguyên gốc: `/Volumes/data/resource/Engla_com.english.learninglanguage`
Trạng thái: **đang triển khai theo từng luồng; chưa xác nhận hoàn tất fidelity giao diện toàn app**.

> Các câu chỉ thị nằm trong tài liệu cũ được xem là bối cảnh lịch sử, không tự động trở thành chỉ thị mới. Kết luận dưới đây dựa trên source/resource và các phép kiểm tra ghi rõ; không dùng nhãn “100%” nếu chưa có bằng chứng đối chiếu đủ.

## Tóm tắt tiến độ

Đợt rà soát ngày 30/10 đã sửa các cụm Plan, Practice, Path, Shadowing, Home và loại bỏ entry point Premium. Splash progress/intro Lottie đã được port. Lượt `:shared:testAndroidHostTest :androidApp:assembleDebug` thành công với 16 host tests, 0 lỗi/skip. Trên `c9bcc000` (Android 15), APK mới mở được My Path → Day 1, Lesson → node Goodbye, Shadowing list/filter/player và Settings → Language; sau kiểm tra app đang foreground ở Home. Hộp giải thích quyền mic đã được làm lại; quyền mic vẫn chưa cấp. Không hoàn tất bài hoặc thay đổi tiến độ học.

| Khu vực | Trạng thái hiện tại | Việc còn thiếu |
|---|---|---|
| Splash + launch | APK gốc mở `SplashActivity` của FirstOpen SDK; Compose nối Splash → language picker → 3 intro → survey → profile wizard → Home. Splash có progress bar 40% rộng, xanh, đảo chiều chu kỳ 1,5 giây; intro Android dùng 3 Lottie gốc và ảnh tĩnh làm fallback. | Splash logo/intro/safe area/thời lượng vẫn cần screenshot/runtime trên fresh install; iOS intro giữ ảnh tĩnh. |
| FirstOpen/onboarding | Có 3 intro pages, survey chọn ngôn ngữ học và wizard hồ sơ; wizard lưu/khôi phục từng bước, survey hoàn tất lưu vào repository. Artwork/icons/colors được đối chiếu resource; đã thêm chuỗi gốc cho it/ru/tr/ja/zh/ko/th và cho subtitle survey tự wrap. Urdu tiếp tục fallback tiếng Anh như resource APK gốc. | Nếu process chết trước khi survey hoàn tất, intro quay về trang đầu và lựa chọn survey chưa lưu; cần quyết định có khôi phục đúng trang hay theo behavior gốc. Chưa chạy fresh install/force-stop hoặc đo layout/RTL trên thiết bị. |
| Home | Hero theo asset/copy gốc; active-plan card hiển thị stage/day/progress và Start/Continue/View path. Home đã mở lại trên APK hiện tại; ảnh runtime có thẻ kế hoạch và Shadowing. | Chưa đối chiếu pixel/layout toàn màn với ảnh gốc; cần kiểm tra trạng thái catalog rỗng/offline. |
| Practice/Grammar/Vocabulary/Speaking/Path | 36 JSON khớp byte với APK; 93 lesson ID có nội dung. Path có connector fill/highlight/pop; Speaking có Skip không chấm điểm; feedback được reset khi sang câu. Trên APK mới, node `Goodbye` mở đúng câu hỏi “Which word do you say when you leave?” và lựa chọn `Goodbye`. | Chưa chạy hoàn tất bài/animation completion; score nói vẫn là text similarity, không phải acoustic/phoneme. Chưa đối chiếu toàn bộ màn với screenshot gốc. |
| Plan/progress/history | Day index 1 và phạm vi 1–30 được sửa; Plan dùng Firebase catalog/bank và cache app-private; activity chỉ lưu sau completion. My Path → My 30-day plan → Start Day 1 đã mở trên thiết bị, hiển thị dữ liệu sẵn có 3/4 hoạt động. Progress/History title/filter đã sửa; Android streak dùng timezone thiết bị. | Chưa hoàn tất activity; chưa kiểm tra nhiều goal/offline, TrustedClock, phút/ngày và manifest MD5. |
| Tutor/network | Today Lesson xử lý hết giờ/back confirmation/retry copy; custom microphone rationale tiếng Việt đã kiểm tra trên thiết bị, Continue mở system permission. | Mic vẫn `granted=false`, chưa test STT; chưa gửi request API runtime. Mood tag chưa port; chat history/runtime privacy cần đối chiếu Legal. |
| Shadowing | Catalog/list, thumbnail, Daily life/Travel filters và player 9 câu đã mở trên thiết bị; logcat có MediaCodec audio/video decode. Transcript, dịch và chip từ hiện diện. | Chưa xác nhận frame/audio sync hoặc highlight từng từ; nguồn chỉ có timecode cấp câu nên highlight từ còn nội suy. iOS chưa ưu tiên. |
| Settings/billing/legal | Đã gỡ diamond/tab buttons, Settings ProBanner, PAYWALL route và đoạn Premium/subscription trong Legal; route premium cũ bị bỏ qua. | Billing/entitlement nội bộ và chuỗi dịch cũ còn trong code nhưng không được nối vào UI; Rate Us/Support vẫn cần rà riêng. |
| Bảo mật | Header/key cũ được khôi phục trong client theo xác nhận riêng của người dùng; bypass test vẫn tắt; báo cáo không ghi giá trị key. | Key nằm trong binary và đã xuất hiện trong output của phiên làm việc này; cần rotate ở backend trước khi phát hành hoặc chuyển sang xác thực server-side. |

## Chi tiết các sửa đổi source trong đợt này

- `BaseAppRepository.kt`, `BaseAppViewModel.kt`, `App.kt`: nối route startup, FirstOpen, survey, profile onboarding, language settings và lưu completion.
- `SplashScreen.kt`, `FirstOpenIntroArtwork.*`: Splash dùng progress bar 40% rộng, xanh, chạy đảo chiều 1,5 giây; intro Android phát 3 Lottie gốc với ảnh tĩnh fallback. Logo Splash vẫn tĩnh; cần screenshot/runtime xác nhận bố cục trên máy thật.
- `FirstOpenScreens.kt`, `OnboardingModels.kt`, `OnboardingScreen.kt`: tách intro/survey khỏi wizard hồ sơ; giữ state wizard và hỗ trợ khôi phục. Thêm resource gốc cho 7 locale UI còn thiếu; Urdu giữ fallback theo APK gốc; subtitle survey được phép wrap.
- `AndroidAppRepository.kt`, `IosAppRepository.kt`: lưu survey, onboarding progress, PlanProgress và snapshot learning progress.
- `UserPrefs.kt`, `PlanProgress.kt`, `PlanModels.kt`: loại seed/demo, dùng thời gian epoch, lưu snapshot progress local, không tự cộng thưởng hoặc đánh dấu hoàn thành giả.
- `PlatformSpeechRecognition.*`, `SpeechCaptureMic.kt`, `PlatformTextToSpeech.*`: nối hành vi mic/TTS theo nền tảng; iOS STT vẫn chưa khả dụng.
- `SpeakingScreens.kt`, `VocabScreens.kt`, `PathLessonScreen.kt`, `VideoShadowingScreens.kt`, `TodayLessonScreen.kt`: điểm/lượt hoàn thành dựa trên thao tác hoặc transcript thực; không tự chấm điểm bằng đáp án mẫu.
- `HomeScreen.kt`: dùng nội dung hero Home có trong source Engla, mở CTA speaking và tạo card Learning Path; gỡ thẻ từ vựng mẫu hardcoded.
- `LearningItem.kt`, `ProgressScreen.kt`, `ProgressDetailScreen.kt`, `LearningHistoryScreen.kt`: tính phần trăm đúng theo `score/total`, hỗ trợ session không có điểm.
- `TodayLessonScreen.kt`, `Translations.kt`, `AiTutorScreen.kt`: Today Lesson hiện trả thông báo retry đã dịch, không phát TTS thành công giả khi API trả null; session lưu metadata nhưng không persist hội thoại.
- `App.kt`: bỏ network gate toàn app theo mô hình Local-First mô tả trong TECH_SPEC; lỗi mạng để tại thao tác cần mạng.
- `App.kt`, `TodayLessonScreen.kt`: system Back trong Today Lesson/Plan Talk chuyển thành yêu cầu đóng để màn hiện hộp xác nhận và hoàn tất lưu tiến độ khi người dùng chọn Complete.
- `ShadowingVideos.kt`, `ShadowingRemoteCache.*`, `VideoPlayer.*`: lấy catalog/script từ Firebase, cache JSON app-private, stream MP4 qua Media3 với LRU cache; transcript partial và karaoke highlight dùng timecode cấp câu.
- `Platform.kt`, `UserPrefs.kt`: streak Android tính ngày theo timezone thiết bị; TrustedClock chưa được port và iOS vẫn dùng UTC.
- `SettingsScreen.kt`, `AiTutorScreen.kt`: bỏ trạng thái Pro giả/no-op speech rate và đồng bộ tùy chọn tutor persona.
- `AppConfig.kt`, `LlmApi.kt`: khôi phục custom auth header và key cũ theo xác nhận của người dùng; URL giữ nguyên, các bypass vẫn tắt. Build Android/iOS sau chỉnh sửa thành công; chưa gửi request API runtime.
- `Platform.ios.kt`: cung cấp timestamp epoch cho state/time tracking trên iOS.
- `BaseAppRepository.kt`, `App.kt`: resolver chọn StartupLanguage → FirstOpenIntro → FirstOpenSurvey → Onboarding → Home; render route language/intro/survey ở root.
- `GoalPlan.kt`, `PlanModels.kt`: truyền ngày tuyệt đối vào chọn video, kiểm tra phạm vi ngày và tránh truy cập index âm ở Plan.
- `LearningPathScreen.kt`: khởi tạo level bằng `PathCourse.startLevel(UserPrefs)` để dùng placement CEFR khi người học chưa chọn level path thủ công.
- `GrammarScreens.kt`, `VocabScreens.kt`: reset feedback/selection khi quay lại Grammar và giữ điểm theo index câu; Vocabulary khôi phục score của đúng thẻ và đóng translation khi quay lại.
- `SharedCommonTest.kt`: thêm regression test Day 1/invalid day; chỉnh test locale theo 16 ngôn ngữ ứng dụng hỗ trợ.

## Kiểm chứng đã chạy ngày 30/10

- `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug` — **BUILD SUCCESSFUL** ngày 30/10 sau các sửa đổi hiện tại; 16 host tests, 0 failure, 0 skipped. Gồm startup/locale, Day 1/index bounds, Premium/Practice/Shadowing/Plan translations, word highlight và Plan activity set. Debug APK 187,313,860 bytes. `git diff --check` sạch.
- `./gradlew :shared:compileKotlinIosSimulatorArm64` — **BUILD SUCCESSFUL** sau các expect/actual mới; chỉ compile shared iOS simulator target, không build/cài full iOS app.
- Debug APK tại `androidApp/build/outputs/apk/debug/androidApp-debug.apk` đã cài bằng `adb install -r`; thiết bị trả `Success`, `pm path` xác nhận package/versionCode 100. `am start -W` trả `Status: ok`; screenshot cho thấy màn Language, MainActivity resumed và process còn sống. Không gỡ app hoặc xóa data.
- Đối chiếu path resources: 36/36 JSON cùng hash với APK gốc; 93 lesson ID khớp; question schema không có answer index sai.
- Trên thiết bị `c9bcc000` (Android 15), tái hiện crash của binary trước cập nhật: Home → My Path → My 30-day plan → Start Day 1; Logcat: `IndexOutOfBoundsException`, index `-1`, `GoalPlan.videoFor` → `PlanFlow.dayPlan`. Bản source mới đã build/cài và mở được màn Language; **chưa đi tới My Learn Path để xác nhận crash fix runtime**.
- Tab Lesson mở được trên app cài; chạm node “Goodbye” mở content mang nhãn “Asking for Help”. Dữ liệu source hiện tại gắn `beginner-u1-l2` với Goodbye; runtime mismatch chưa được giải thích và cần tái kiểm tra trên bản mới.

## Việc tiếp theo theo thứ tự màn hình/ưu tiên

1. Tạo môi trường Android test không chứa dữ liệu cá nhân hoặc dùng thiết bị test riêng; kiểm tra crash Day 1, Home active plan và toàn bộ activity trên build mới.
2. Chụp/đối chiếu Splash, language picker, 3 intro, survey và profile wizard; xác nhận Lottie/progress motion, safe area, back/skip và trạng thái khôi phục.
3. Tiếp tục đi từng tab Practice, Vocabulary, Grammar, Speaking, Path, Plan, Tutor, Progress, History và Settings để đối chiếu action/state/animation; xác minh mismatch node Goodbye.
4. Đo sync Shadowing trên thiết bị; nếu cần highlight từng từ chính xác, bổ sung forced alignment/per-word timestamps thay cho nội suy theo timecode cấp câu. Script/clip hiện stream trực tiếp và cache theo Media3; kiểm tra offline sau khi phát hết clip.
5. Đối chiếu Plan XP/streak và thay đổi ngày/giờ; xác minh cache/offline với nhiều goal/bank.
6. Rà riêng Rate Us/Support, legal/privacy và auth/LLM theo hành vi thật. Giữ UI Premium ngoài luồng sản phẩm; không đưa credential thật vào báo cáo hoặc source.

## Giới hạn còn lại

APK hiện tại đã được cập nhật bằng `adb install -r`; dữ liệu được giữ nguyên và app đang mở tại Home. Day 1, Lesson/Goodbye và Shadowing cơ bản đã mở được trên thiết bị. Vẫn cần chạy first-run trên môi trường sạch, kiểm tra STT sau khi cấp quyền, completion animation và sync Shadowing; không kết luận fidelity tổng thể hoàn tất.

## Cập nhật kiểm chứng Android theo màn — 2026-10-30

> Mục này bổ sung và thay thế trạng thái runtime cũ ở trên cho APK vừa cài; không xóa dữ liệu ứng dụng.

- **Dialog microphone:** hộp Material mặc định bị lệch màu/font so với Engla và phần mô tả rơi về tiếng Anh do key dịch không khớp. Đã thay bằng `MicrophonePermissionDialog` dùng bề mặt trắng, icon mic xanh, chữ tương phản rõ, nút chính màu xanh và bản dịch tiếng Việt. Cả hộp hướng dẫn mở Settings dùng chung thiết kế. Ảnh: [`ailearn-mic-dialog-final-design.png`](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-mic-dialog-final-design.png).
- **Permission flow:** bổ sung `LocalActivityResultRegistryOwner` ở `MainActivity` để `rememberLauncherForActivityResult` hoạt động trong composition hiện tại. Trên `c9bcc000`, Today Lesson mở không crash; chạm mic hiện dialog tiếng Việt; Hủy đóng dialog; Tiếp tục mở system permission sheet. Đã đóng sheet bằng Back, không cấp quyền; `dumpsys package` vẫn báo `RECORD_AUDIO: granted=false`. Chưa kiểm tra transcript STT vì chưa cấp quyền.
- **My Path / Plan:** `My Path → My 30-day plan → Start Day 1` vào được Day 1 activities; màn hiển thị trạng thái đã có sẵn `3/4`, không phát sinh thêm hoạt động hoàn thành. Không còn tái hiện crash `GoalPlan.videoFor` trên APK mới.
- **Learning Path:** tab Lesson mở Unit 1; node `Goodbye` mở đúng câu hỏi từ vựng “Which word do you say when you leave?” với lựa chọn `Goodbye`. Không chọn đáp án, không thay đổi tiến độ; không có FATAL/IndexOutOfBounds trong logcat. Ảnh: [`ailearn-learningpath-goodbye.png`](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-learningpath-goodbye.png).
- **Shadowing:** danh sách remote tải được, nhãn category tiếng Việt, thumbnail và duration hiển thị; bộ lọc `Daily life` và `Travel` trả đúng nhóm. Player tải nội dung 9 câu, có transcript, bản dịch và chip từ; logcat có MediaCodec video/audio decode và không có crash. Ảnh: [`ailearn-shadowing-daily-filter.png`](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-daily-filter.png), [`ailearn-shadowing-player-latest.png`](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-player-latest.png). Chưa xác nhận đồng bộ từng từ chính xác: script chỉ có timecode cấp câu nên progress từ vẫn nội suy.
- **Language / voice:** Settings → Language mở được danh sách ngôn ngữ và đánh dấu Vietnamese đang chọn. Onboarding source có bước chọn Male/Female/Random/Robot và ghi `UserPrefs.tutorVoice`; Android TTS áp dụng lựa chọn gần nhất trong các voice đã cài. Không chạy lại onboarding trên thiết bị vì không xóa/reset dữ liệu; chưa xác minh từng giọng TTS bằng phát âm thực tế.
- **Hint:** đã gỡ badge `AD` khỏi nút Hint trong Today Lesson.
- **Kiểm chứng build:** `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug` — **BUILD SUCCESSFUL**, 16 tests, 0 failure/error/skip; `git diff --check` sạch. APK debug đã cài bằng `adb install -r`; app đang foreground ở Home. Không uninstall, clear data hoặc cấp quyền mic.

### Việc còn cần xác minh

- First-run Language/Onboarding UI chưa được chạy lại trên fresh install; bằng chứng hiện tại gồm source và Settings → Language trên cài đặt đã hoàn tất onboarding.
- Chưa cấp quyền mic nên chưa kiểm tra STT/realtime transcript tại mọi entry point.
- Chưa xác nhận frame advancement/audio-video alignment và tô từ đúng theo âm thanh Shadowing; cần timecode theo từ hoặc forced alignment để kết luận chính xác.
- Voice selector đã được kiểm tra source và lưu flow; cần chạy thực tế từng voice trên thiết bị sau khi có môi trường onboarding test riêng.


## Kiểm chứng tích hợp bổ sung — Android 2026-10-30

- Bản debug tích hợp mới nhất được cài lại trên Xiaomi Redmi Note 13 `c9bcc000` (Android 15, 1080×2400); app vẫn ở foreground. `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug --console=plain` thành công; 23 test, 0 failure/error/skip. `git diff --check` sạch; logcat mẫu không có FATAL/ANR.
- **Learning Path / kiểm tra:** Đã đối chiếu màn lộ trình với tài nguyên app gốc; chỉnh căn giữa node đơn, khoảng cách đầu màn, phản hồi node khóa, bố cục câu hỏi/feedback và dialog thoát có bản dịch. Trên máy thật, trả lời sai giữ feedback đúng câu, Continue chuyển sang câu kế không lóe toast; dialog thoát Stay/Leave hoạt động. Hoàn tất bài 15 câu với 11 đúng và 4 câu nói được Skip; hai node và các đoạn nối đổi sang trạng thái hoàn thành màu xanh.
- **Bảo toàn dữ liệu:** lượt hoàn thành bài chỉ dùng để kiểm tra hiệu ứng path; DataStore được khôi phục và so sánh byte chính xác (SHA-256 trước/sau giống nhau). Home sau đó vẫn hiện dữ liệu ban đầu 3/4 hoạt động, 9 phút.
- **Giọng nói / nhân vật:** Today Lesson mở được trên thiết bị. Dialog giải thích mic tiếng Việt → system permission sheet → trạng thái Listening… đều chạy; đã thu hồi quyền ngay sau thử (`RECORD_AUDIO granted=false`). Robot hiện đúng trong lesson. Chưa phát âm kiểm tra từng voice hoặc đọc lời thật để xác nhận độ chính xác transcript; AI Tutor persona picker không được kiểm tra trực tiếp trên runtime trong lượt này.
- **Thống kê:** Progress mở được và render; hiện hiển thị 0 ở các bộ đếm nhóm và streak 1 theo dữ liệu đã lưu sẵn. Hạch toán ngày theo timezone thiết bị/đếm phút, điểm và skip có 7 regression tests riêng đạt; không tạo thêm hoạt động trong dữ liệu người dùng. Chưa xác nhận giá trị thống kê sau một session thật vì đã hoàn tác dữ liệu test.
- Đã lấy lại màn app và kiểm tra activity top-resumed sau thử mic; không thấy FATAL/ANR trong logcat mẫu. Chỉ xác minh Android ở lượt này; chưa chạy lại iOS.

## Rà soát và cập nhật My Path — Android, 2026-10-30

### Đối chiếu với app gốc

- PlanScreenKt của bản gốc hiển thị đường đi ziczac dọc có connector nét đứt, đánh dấu rõ trạng thái từng ngày, mở danh sách tại chặng hiện tại và đổi CTA thành Continue khi ngày hiện tại đã có hoạt động.
- Trước chỉnh sửa, clone xếp ba ngày thành một hàng ngang, luôn cuộn từ Stage 1, CTA vẫn ghi Start dù Day 1 đã có 3/4 hoạt động; thanh Material có chấm rời ở cuối.
- Core gốc mở mọi ngày đã tới hạn theo lịch cài đặt. Clone lại chặn hoàn tất nếu ngày đó khác ngày chưa hoàn tất đầu tiên, dù ngày đã được mở khóa.

### Đã sửa

- PlanScreen.kt chuyển các node thành đường ziczac dọc có connector cong nét đứt, robot ở ngày hiện tại, biểu tượng khóa/ngày hoàn tất và tóm tắt phút hoặc hoạt động; tự cuộn tới stage chứa ngày hiện tại.
- Thêm thanh tiến độ bo tròn có fill gradient, nhãn số ngày; CTA giờ hiển thị Tiếp tục khi ngày hiện tại đã bắt đầu. Home không còn nút mở ngày đã khóa; màn Day Activities chặn route trực tiếp vào ngày chưa tới hạn.
- PlanFlow.finish và PlanProgress.completeDay cho phép hoàn tất bất kỳ ngày nào đã mở khóa theo lịch, như app gốc, vẫn giữ thứ tự activity bên trong từng ngày.
- PlanProgress.kt tính ngày theo timezone thiết bị và Android first-install calendar day thay vì chia epoch UTC; giá trị first-install được khởi tạo ở MainActivity. iOS fallback về ngày bắt đầu plan, chưa kiểm chứng trên iOS.

### Kiểm chứng

- ./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug --console=plain — BUILD SUCCESSFUL; 24 tests, 0 failure/error/skip. Regression test mới xác nhận ngày 2 có thể hoàn tất khi ngày 1 còn dang dở nhưng ngày 2 đã mở theo lịch.
- git diff --check sạch.
- APK cài lại bằng adb install -r trên Xiaomi Redmi Note 13 c9bcc000 (Android 15, 1080×2400); giữ nguyên app data. My Path hiển thị đúng 3/4 hoạt động và CTA Tiếp tục Ngày 1; mở Day 1 vẫn giữ 3/4. Không hoàn tất activity mới. Activity app ở foreground; không thấy FATAL/ANR trong 3.000 dòng logcat gần nhất; RECORD_AUDIO vẫn granted=false.
- Ảnh: [My Path](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-my-path-final.png), [Day 1 activities](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-my-path-day-final.png).

### Còn mở

Không làm giả ngày trên thiết bị để đi tới stage sau; điều kiện ngày mở khóa được xác nhận bằng regression test. Chưa build/chạy iOS. Ảnh này xác nhận bố cục và trạng thái Android, chưa phải pixel-diff toàn màn với app gốc.

## My Path locked-node smoke — Android, 2026-10-30

Trên c9bcc000, chạm node Day 2 đang khóa không điều hướng sang Day Activities và không đổi tiến độ; My Path vẫn hiển thị Day 1 với 3/4 hoạt động.

## My Path screenshot A/B recheck — Android, 2026-10-30

The user's screenshot comparison was correct: the previous visual review did not compare the complete screen against the original package. It over-weighted the zigzag core path and treated the small generic robot face as sufficient. The original screen has a blue-tinted background, “My Learning Path” header, paired Goal/Assistant cards, a large gradient Stage banner, a layered day node, a full 3D robot with a speech bubble/clouds, and a fixed duration/level/start bar. The old clone missed several of those assets and regions.

`PlanScreen.kt` has now been updated to use that composition and the existing original artwork (`plan_james_body`, `plan_james_arm`, `plan_cel_body`, stage suitcase/plane): paired goal/assistant cards, progress row, gradient stage card, 3D-style path nodes and dashed connectors, floating sleep/wake robot, cloud accents, and a fixed minutes/level/Start-or-Continue footer. The premium diamond action is still omitted as requested. Tapping the Assistant card now updates both the saved voice and visible card immediately.

Evidence: original reference [ailearn-my-path-original-reference.png]({original}), updated device capture [ailearn-my-path-final.png]({current}), and A/B artifact [MY_PATH_SCREENSHOT_COMPARISON.md]({comparison}). The clone now preserves its saved Travel goal, James voice, and Day 1 3/4 progress; it does not mimic the original account's Everyday Conversation goal or 0/4 progress. Day 2 remains locked because this install is one calendar day newer than the original install. The screen layout is much closer, but these user-state differences mean the captures are not a pixel-identical data comparison.

Verification: `:shared:testAndroidHostTest` and `:androidApp:assembleDebug` succeeded (24 tests, 0 failures/errors/skips); `git diff --check` is clean. The rebuilt APK was installed with `adb install -r` on `c9bcc000` without clearing data. My Path opened; the assistant card toggled James → Emma → James, and the robot bubble toggled to “Ready to learn?” and back. The app remained foregrounded; no FATAL/ANR was found in the sampled recent logcat. No lesson was started and progress was not changed.


## Plan setup và thanh tiến độ — cập nhật 2026-10-30

- Tái hiện trạng thái `Plan content unavailable` trên cài đặt hiện có và đối chiếu với catalog gốc: questionnaire cũ lưu goal ID `conversation`, trong khi catalog dùng `everyday` (cùng dạng sai lệch với `career`/`work_career`, `study`/`school_study`, `exams`/`language_exams`, `other`/`custom`). Đã thêm canonical mapping ở `GoalPlan`, để màn Path đọc được plan đã lưu mà không xóa dữ liệu; lựa chọn mới được lưu bằng catalog ID.
- Màn tạo Path tải danh sách goal từ `PlanCatalogRepository`, chỉ cho chọn goal có nội dung cho ngôn ngữ học, có trạng thái tải/thử lại, nhập ghi chú cho goal tùy chỉnh, và dùng ảnh khảo sát gốc cho robot/name/level/time/assistant. Nếu goal hiện tại không phân giải được, Path ẩn CTA bắt đầu và cho đổi goal.
- Thay progress indicator Material có chấm cuối bằng `AppLinearProgressBar` ở các màn setup, Plan, speaking và quiz. Track dùng màu gốc `#EFEFF1`, giữ nguyên màu fill xanh hiện có theo từng màn; chấm cuối không còn hiện trên thiết bị.
- Thêm 2 regression tests cho ánh xạ ID legacy và ID được lưu sau khi tạo plan. `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug --console=plain` thành công: 26 tests, 0 failure/error/skip; `git diff --check` sạch.
- APK debug cài bằng `adb install -r` trên `c9bcc000` (Android 15). Đã mở My Path: hiển thị Everyday Conversation thay cho `Plan content unavailable`; kiểm tra questionnaire Start → Name → Goal, xác nhận robot/ảnh gốc và track xám không có chấm. UI goal hiển thị “Why do you want to learn English?”. Không hoàn tất questionnaire nên không tạo/ghi đè plan.
- DataStore sau khi mở app chỉ khác bộ đếm mở app; đã khôi phục byte-for-byte từ bản sao trước kiểm tra, SHA-256 trước/sau đều `12be4821f93273dc6bb204849b4f2093bb87b766bd3c65609b1104aa639cd4bd`. App vẫn ở foreground; logcat 500 dòng gần nhất không có FATAL/ANR.


Ảnh thiết bị: [My Path + track tiến độ](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-my-path-progress-track-2026-10-30.png), [màn chọn goal + progress](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-plan-setup-goal-progress-2026-10-30.png).


## My Path: spacing nhãn và bóng robot — 2026-10-30

- Dịch cụm lộ trình xuống 16dp để nhãn `START HERE` có khoảng cách với Stage banner; node ngày, robot, speech bubble và mây đi cùng cụm để giữ bố cục.
- Thêm bóng oval xanh xám mờ bên dưới robot, theo ảnh gốc; kiểm tra lại trên `c9bcc000`, bóng nằm dưới hình robot chứ không còn bị thân ảnh che thành một chấm nhỏ.
- Build/test Android thành công: 26 test, 0 lỗi; app được cài lại và đang ở My Path. DataStore được khôi phục chính xác sau khi test; SHA-256 trước/sau `7da72a4512cd53810d036f95205674b190c9df32d87de910c03c5ce345724b63`.
- Ảnh runtime: [My Path — khoảng cách START HERE và bóng robot](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-my-path-start-here-robot-shadow-2026-10-30.png).

## Video Shadowing: từ mới và player — Android 2026-10-30

- Đối chiếu player gốc với bản Android: nội dung gốc dùng phụ đề karaoke, chip xanh riêng cho từ vựng mới, ba điều khiển tốc độ/phát lại/ngôn ngữ, vùng “We heard”, mic và CTA lưu toàn bộ từ mới. Bằng chứng ảnh gốc: [player gốc](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-original-player-2026-10-30.png).
- Sửa nhận diện từ mới theo từng token: lấy dạng từ theo ngôn ngữ học, lowercase và bỏ dấu câu ở biên rồi so khớp chính xác với catalog của video. Chip từ mới dùng nền xanh nhạt/viền xanh và chữ màu chính để không bị màu karaoke xanh lấn át; từ bị chấm sai đổi sang nền/viền/chữ đỏ. Từ thường không có chip và vẫn theo màu karaoke. Đã xác nhận “unfortunately” được nhận là từ mới trong câu đầu.
- Nút nói dùng icon mic/stop của Material; tap bật/tắt, nhấn giữ bắt đầu thu và nhả để dừng. UI nhắc thao tác này rõ hơn. Hàng tốc độ/phát lại/ngôn ngữ thu gọn và căn trái; scrubber có núm tròn; CTA “Lưu tất cả” lưu danh sách từ chưa lưu và đổi trạng thái sau khi lưu.
- Thiết bị Xiaomi Redmi Note 13 c9bcc000: debug APK cài thành công; kiểm tra ENG ↔ VIE và câu dịch, chạm video chuyển từ câu 1 sang câu 2, “Lưu tất cả 10 từ mới” chuyển thành “Đã lưu tất cả 10 từ · Xem”. Sau kiểm tra đã khôi phục DataStore chính xác; SHA-256 trước/sau 33ae409505ff0a38dabacc731cb142832ca89f25afe50942dcfb76a7f0cac99a. App để mở tại Video Shadowing; logcat mẫu không có FATAL/ANR.
- ./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug --console=plain: BUILD SUCCESSFUL; 26 test, 0 lỗi. git diff --check sạch. Ảnh hiện tại: [player Android sau chỉnh sửa](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-current-player-final-2026-10-30.png).
- Giới hạn còn lại: chưa cấp quyền mic nên chưa xác minh nhận dạng giọng nói trên thiết bị; tiến độ tô karaoke trong một câu vẫn được nội suy từ mốc thời gian cấp câu, chưa có forced alignment theo từng từ.

## Shadowing core: dừng âm thanh video khi thu giọng nói — Android 2026-10-30

- Finding: luồng bắt đầu nhận dạng giọng nói chưa dừng video trước khi mở capture; âm thanh mẫu có thể lọt vào mic và làm nhiễu transcript/chấm phát âm.
- Đã thêm callback trước khi start recognition: player gọi videoController.pause() trước khi mở mic. Khi nhả mic/kết thúc nhận dạng, video tiếp tục ở trạng thái dừng; người học chủ động bấm Replay/Play để nghe lại, tránh audio tự chạy đè lên phần nói hoặc feedback.
- Chỉ sửa source trong lượt này; chưa chạy build hoặc kiểm tra thao tác thu trên thiết bị sau thay đổi này.

## Shadowing core + scrubber follow-up — Android, 2026-10-30

- Kiểm tra mic thật trên Xiaomi Redmi Note 13 (`c9bcc000`, Android 15). Quyền `RECORD_AUDIO` đã được cấp sẵn; không đổi quyền trong lượt này. Khi bắt đầu thu, video dừng tại 0:04 và điều khiển tua/next bị khóa; nhận dạng hiển thị “hi I bought this up yesterday”. Người dùng xác nhận kết quả chấp nhận được; từ `shirt` được Android SpeechRecognizer nhận thành `up`, nên kết quả nhận dạng vẫn phụ thuộc engine/thiết bị. Ảnh: [transcript thật](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-stt-result-2026-10-30.png).
- Đã đi từ Home card → catalog → player; tap card mở đúng video. Lưu 10 từ, mở Saved Words và xác nhận nghĩa tiếng Việt cùng nguồn “Trả lại hàng” ([ảnh Saved Words](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-saved-words-2026-10-30.png)). Chạy tới câu 10, chạm video hiện completion dialog; nhấn Done trở về catalog. DataStore cho thấy chỉ thêm một entry `Shadowing Practice` và một `returning_an_item`; callback completion không ghi trùng.
- Sửa scrubber: track Material3 mặc định vẽ stop-indicator ở cuối và dày hơn ảnh gốc. Tắt stop-indicator, đặt track 4dp bo tròn, bỏ gap núm-track. Ảnh runtime: [scrubber sau sửa](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-seekbar-fixed-2026-10-30.png).
- `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug --console=plain`: BUILD SUCCESSFUL; 26 tests, 0 failure/error/skip. `git diff --check` sạch. APK cài lại bằng `adb install -r`; `MainActivity` ở foreground; logcat mẫu không có FATAL/ANR.
- Dữ liệu test đã khôi phục byte-for-byte, SHA-256 trước/sau `49dcc0aefff3d0fe8616f32caafce027f4a4b06711273179dc0737296df038c8`; app đang mở tại player Shadowing. Karaoke từng từ vẫn nội suy từ timestamp cấp câu, chưa có forced alignment.
