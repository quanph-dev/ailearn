# Báo cáo rà soát UI và core: Engla → AiLearn

Ngày cập nhật: 2026-10-30
Trạng thái: đã có sửa source theo từng luồng; phần snapshot bên dưới ghi nhận phát hiện trước khi sửa. Cập nhật mới nhất ở ngay sau phần này.

## Cập nhật mới nhất: triển khai theo luồng và đối chiếu UI

Các findings trong những mục snapshot phía dưới được ghi trước đợt sửa source; bảng này là trạng thái hiện tại và thay thế các nhận định cũ tương ứng.

| Hạng mục | Đã hiệu chỉnh trong source | Còn phải xác minh hoặc làm tiếp |
|---|---|---|
| Splash/startup | APK gốc khai báo `SplashActivity` làm launcher; Compose nối Splash → language → 3 intro → survey → profile → Home. Đã port progress bar 40% rộng, đảo chiều 1,5 giây và Lottie Android gốc cho 3 intro, ảnh tĩnh làm fallback. | Survey selection/page chưa persist trước khi hoàn tất; chưa đối chiếu fresh install/safe area bằng screenshot. iOS intro giữ ảnh tĩnh theo ưu tiên Android. |
| FirstOpen survey/profile | Artwork/icons/colors survey được đối chiếu resource; state wizard persist; survey target bỏ qua bước Learn Language; CEFR badge/label không lặp. Đã thêm chuỗi APK gốc cho it/ru/tr/ja/zh/ko/th; Urdu fallback giống APK gốc; subtitle survey cho phép wrap. | Chưa so spacing/icon/font/RTL bằng screenshot thiết bị; chưa force-stop để xác nhận. |
| Home | Hero copy/art/CTA theo source LessonCard; active-plan card có stage/day/progress và các CTA. Home mở lại được trên APK mới, thẻ Path và Shadowing hiện đúng trong screenshot runtime. | Chưa đối chiếu pixel/layout toàn màn với ảnh gốc; cần test catalog rỗng/offline. |
| Local-first/offline | Đã bỏ root-wide OfflineRobot gate theo TECH_SPEC; local screens tiếp tục mở offline, lỗi chỉ hiện ở request AI. | Cần kiểm tra offline trên thiết bị; app gốc có một OfflineRobot screen nhưng không đủ bằng chứng cho root gate toàn cục. |
| Practice/Grammar/Vocab/Speaking/Path | 36 path JSON khớp APK hash, 93 IDs có nội dung. Skip/feedback/connector đã sửa trong source. Trên thiết bị, Goodbye mở đúng câu hỏi “Which word do you say when you leave?” và lựa chọn Goodbye. | Chưa chạy hết lesson hoặc completion animation; text similarity không tương đương phoneme scoring; RTL node layout vẫn cần rà. |
| Plan/Progress/History | Plan dùng Firebase catalog/bank/cache; activity hub chỉ ghi nhận sau completion. My Path → Day 1 đã mở trên thiết bị, trạng thái 3/4 được giữ nguyên. Progress/History title/filter đã chỉnh; Android streak dùng timezone thiết bị. | Chưa hoàn tất activity hoặc kiểm tra nhiều goal/offline; TrustedClock, iOS UTC, phút/ngày và manifest MD5 còn mở. |
| Tutor/LLM | Today Lesson có retry/back/complete flows; custom rationale microphone tiếng Việt đã xác minh trên thiết bị, Continue mở system permission sheet. | Mic chưa cấp nên STT chưa xác minh; mood tag chưa port; chat history/privacy và API runtime cần audit riêng. |
| Shadowing | Catalog/list, thumbnail, Daily life/Travel filter và player mở trên thiết bị; logcat có MediaCodec decode; transcript/câu dịch/chip từ hiển thị. | Chưa xác nhận sync frame/audio và per-word alignment; timecode nguồn chỉ ở cấp câu. iOS chưa ưu tiên. |
| Settings/billing | Premium diamond/header, Settings ProBanner, PAYWALL route và copy subscription trong Legal đã gỡ khỏi UI. | Billing/entitlement nội bộ/translation cũ còn nhưng không được nối vào luồng UI. Rate Us/Support vẫn là việc rà riêng. |
| Privacy/security | Header/key cũ được khôi phục trong app theo xác nhận riêng; bypass test vẫn tắt; báo cáo không chứa giá trị key. | Key có thể trích xuất từ binary và đã xuất hiện trong output phiên làm việc; cần rotate phía backend trước release hoặc chuyển sang server-side auth. Policy nói chat history lưu local và voice chỉ xử lý trên thiết bị trong khi transcript không được lưu và platform speech processing chưa xác nhận. |
| Android/iOS compile | Android build + 16 host tests thành công, 0 fail/skip; APK cài và mở trên thiết bị. My Path Day 1, Learning Path Goodbye, Shadowing và permission dialog đã kiểm tra theo phạm vi ghi bên dưới. `:shared:compileKotlinIosSimulatorArm64` thành công; `git diff --check` sạch. | Full iOS app build/runtime chưa chạy; Android startup first-run và kiểm tra full-app parity còn thiếu. |

**Các sửa UI phát hiện được trong bước đối chiếu mới:** Home `LessonCard` gốc dùng “Speak with AI”, “Build confidence every day”, asset robot/nền, tỷ lệ card theo chiều rộng và nút mic “Start speaking”; source đã đổi theo. Settings gốc không có nhãn “Pro Unlocked” hay “Speech Rate”; banner entitlements giả và điều khiển rate no-op đã gỡ. Card từ “Confidence” hardcoded đã gỡ. Survey gốc dùng title/subtitle/option #222222, primary #4C8DF6, card #F8F8F8, checkbox vuông bo góc, skip nền #F3F3F4 và footer #FAFAFA. Source Compose trước sửa dùng checkbox tròn, viền selected card và một số màu/text từ theme chung; đã chỉnh về resource values ở `FirstOpenScreens.kt`. CEFR trước đây vừa ghi “A1 Beginner” vừa render badge A1; label đã sửa giống source gốc. First-open strings đã được bổ sung từ APK cho 7 locale còn thiếu; APK gốc không có bộ `fo_*` Urdu nên Urdu vẫn dùng fallback tiếng Anh. Subtitle survey không còn bị ép một dòng. Đối chiếu thêm manifest/XML xác nhận launcher gốc là SplashActivity; Compose splash được đổi theo kích thước/copy/layout, nhưng Lottie Apero vẫn là logo tĩnh và cần runtime đối chiếu.



**Cập nhật triển khai 30/10 sau rà soát theo cụm:** gỡ các entry point Premium khỏi tabs/Settings/Legal và bỏ route Paywall; Home có thẻ active plan đọc progress reactive; Path completion connector/node animation được port từ behavior decompile; Practice speech có Skip không ghi điểm và wrong-answer banner không còn lóe khi đổi câu. Plan dùng Firebase catalog/bank thật, cache app-private, route đầy đủ activity và bỏ fallback từ Path; chỉ các activity có nội dung mới tham gia điều kiện hoàn tất ngày. Android Shadowing dùng Media3 ExoPlayer theo TECH_SPEC, nhận partial STT và hiện karaoke transcript; timecode hiện vẫn nội suy giữa mốc cấp câu. Không thay đổi API auth/header trong lượt này.

**Ranh giới xác minh:** đây là audit source/resource và biên dịch; chưa có APK chạy gốc hoặc ảnh tham chiếu đủ để chứng nhận pixel-by-pixel. Không ghi phần UI là hoàn tất cho tới khi kiểm tra màn hình thực tế từng bước.

## Rà soát bổ sung ngày 30/10/2026

### Crash và phân biệt hai luồng “Path”

- **Crash được tái hiện:** Home → shortcut **My Path** → **My 30-day plan** → **Start Day 1** làm app cài trên `c9bcc000` thoát. Logcat: `IndexOutOfBoundsException: Index -1 out of bounds for length 3`; stack đi qua `GoalPlan.videoFor` → `PlanFlow.dayPlan`. `PlanFlow` đã tính `dayInStage` (0-based) rồi truyền vào `videoFor`, nơi lại chuyển ngày tuyệt đối sang day-in-stage; Day 1 thành index -1. Source sửa bằng cách truyền `day` tuyệt đối, trả null với ngày ngoài 1–30 và dùng `getOrNull`. Regression test đã thêm.
- **Đây là luồng khác với tab Lesson:** tab Lesson mở `LearningPathScreen` (36 unit). Trên app cài, node đang hiện “Goodbye” được báo là mở content “Asking for Help”. Source resource `beginner-u1-l2` có title Goodbye và câu hỏi tương ứng; chưa xác nhận mismatch còn xảy ra trên source build mới vì không cài đè package đang chứa data.
- Catalog/path static check: 36/36 JSON có SHA-256 giống APK gốc; 93 lesson IDs khớp; không tìm thấy option/answer index sai. Điều này xác nhận asset/data tĩnh, không xác nhận flow runtime hay screen content mapping trên binary mới.

### Các parity gap UI/interaction còn mở

- Splash hiện dùng progress Material indeterminate; original `IndeterminateLineView` chạy segment xanh 40% trong 1.5s. FirstOpen pages dùng WebP tĩnh, trong khi APK có ba Lottie onboarding animation chạy lặp.
- Path completion: source gốc có fill animation, moving endpoint dot và pop của node hoàn thành; hiện connector đổi màu tĩnh và node hiện tại chỉ pulse. RTL hiện mirror connector nhưng không mirror vị trí node. Locked node vẫn có click semantics dù handler không làm gì và mô tả không nói trạng thái khóa.
- Path lesson source hiện đã đưa nút Back về exit confirmation theo decompile; trước sửa nó lùi câu và giữ state đáp án cũ. Grammar Back hiện reset selection/feedback, score lưu theo index câu để không cộng trùng khi quay lại. Vocabulary Back khôi phục điểm phát âm theo thẻ và ẩn lại translation. Speaking CTA bật Continue trước transcript; nhánh `Can't speak now? Skip` trong decompile chưa đủ rõ để chốt parity.
- Plan Day 1 crash đã sửa trong source, nhưng session còn không có quiz bank phù hợp: `PlanCatalogRepository.bank()` gọi `PathLessonContent.load(stage.id, CEFR)`, trong khi JSON path chỉ có `beginner/intermediate/advanced` và lesson IDs `*-u*-l*`, không có stage IDs như `travel_01`/CEFR file. Fallback dùng cùng loader nên không có nội dung; session sẽ hiện “Couldn't load your plan”. `PlanFlow.finish` chỉ được gọi với `QUIZ`; VIDEO/SPEAKING/TALK không được hoàn tất qua call sites hiện tại, trong khi ngày chỉ hoàn thành khi mọi activity được ghi.
- APK gốc không chứa plan stage-bank assets; `GoalPlanCatalog` gốc tải catalog/bank riêng qua RemoteConfig URL, cache theo stage version và language. TECH_SPEC liệt kê remote manifest/packages/checksum. Bản KMP hiện không dùng nguồn này, nên không được tạo bank bằng cách tái dùng path lessons; cần khôi phục loader/cache hoặc cung cấp dữ liệu plan thực.
- `PlanProgress.todayDay()` dùng `currentTimeMillis()` của thiết bị để tính ngày; TECH_SPEC yêu cầu TrustedClock và chống thay đổi giờ. Cần đối chiếu clock source/rollback handling trước khi xem ngày unlock là đáng tin.
- Home source thiếu nhánh `PlanHeroCard` cho người đã có plan; shortcut “My Path” mở setup/roadmap nhưng Home không còn lối tắt đến ngày học như source gốc. Nút diamond ở Home có callback rỗng. Đây là lệch flow/action, không chỉ visual.
- Practice dataset 155/155 JSON khớp byte APK; score hiện tại khớp công thức decompile nhưng đo text similarity, không phải acoustic/phoneme. Android speech recognition/TTS có implementation; iOS STT trả unavailable. Vocab deck toàn skip không lưu session; Speaking có thể skip mọi câu và summary không hiện tổng số thẻ.
- Today's Lesson khi LLM trả null hiện đã thay câu “Good effort!”/TTS bằng retry message đã dịch cho các locale, không lộ lỗi kỹ thuật. Decompiled gốc có fallback tích cực tương tự, nên đây là thay đổi theo yêu cầu xử lý lỗi, không phải kết luận parity UI. Countdown ở source dừng tại 0 nhưng chưa gọi completion; cần đối chiếu gốc.
- Android Shadowing có 27 MP4 bundled, `MediaPlayer`, và playback theo đoạn timecode; iOS `VideoPlayerController` là no-op. Source hiện tải catalog/script local, không khớp TECH_SPEC remote catalog/CDN cache. Báo cáo cũ kết luận “không có player” cần giới hạn theo platform/revision.
- Home diamond premium hiển thị nhưng callback bị thay bằng `{}`; Learning Path tab không truyền callback nên diamond bị ẩn. Điều này lệch với báo cáo execution/parity mô tả premium control trên tất cả tabs; cần quyết định đây là thay đổi sản phẩm hay phải khôi phục action parity.
- Survey subtitle hiện wrap; source XML gốc là một dòng marquee. FirstOpen localized strings còn thiếu ở các locale gốc `af,bn,fil,kn,ml,mr,ms,nl,or,ta,te`; cần kiểm tra alias `in`/`id` trước khi sửa đồng loạt.

### Đối chiếu độ tin cậy của báo cáo

- `full_app_parity_verification_report.md` nói đã xác minh mọi tab trên Xiaomi `c9bcc000` Android 15; `EXECUTION_AND_VERIFICATION_REPORT.md` cùng serial lại ghi Android 14. Cần chốt bản ghi thiết bị theo log hiện tại (Android 15).
- Execution report đánh dấu path 100% và mọi tính năng 100%; crash Plan Day 1 và runtime node/content mismatch là bằng chứng các kết luận đó cần được hạ xuống “đã xác minh ở thời điểm/binary được ghi” cho tới khi chạy lại build mới. Báo cáo ghi 36 file JSON; phép đếm/hash hiện tại xác nhận phần asset đó chính xác.
- Host tests trước cập nhật có 5 test fail vì route resolver, số locale, regional Portuguese và test gọi `ja` là unsupported dù Japanese nằm trong 16 mục. Resolver/route handlers đã được sửa; test được làm rõ theo danh mục locale thật. Thêm kiểm tra 15 bản dịch retry; hiện có 10 tests pass.

### Kiểm chứng mới nhất

- `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug` — **BUILD SUCCESSFUL**, 16 tests, 0 failure, 0 skipped; `git diff --check` sạch.
- `:shared:compileKotlinIosSimulatorArm64` từng thành công ở lượt trước; chưa rerun sau các expect/actual mới trong lượt Android này.
- Thiết bị `c9bcc000` có `com.example.ailearn` versionCode 100 trước khi cập nhật. Crash được tái hiện trên binary cũ; APK mới cài `-r` thành công và giữ data. Tại snapshot cũ app đang ở Language; runtime follow-up cuối báo cáo đã mở Day 1, node Goodbye, Shadowing và mic dialog trên APK mới; app kết thúc ở Home.

---

## Phạm vi và cách đọc tài liệu

- Đối chiếu source Compose hiện tại trong repo với tài nguyên `apktool/`, source JADX `jadx/` và `TECH_SPEC_REPRODUCTION_REPORT.md` trong `/Volumes/data/resource/Engla_com.english.learninglanguage/`.
- Đọc `MIGRATION_PROGRESS.md` như tài liệu tiến độ/nhận định cần kiểm chứng. Các câu như “100%”, “đã hoàn thành” và các bước đề xuất trong tài liệu không được coi là chỉ thị sửa repo.
- Không tìm thấy APK/AAB gốc trong thư mục tài nguyên. Vì vậy kết luận dựa trên code, layout/resource và đối chiếu asset; chưa xác nhận pixel trên thiết bị chạy.
- Snapshot ban đầu không chạy build/test và không sửa code. Các sửa đổi và compile thực hiện sau đó được ghi ở mục cập nhật mới nhất phía trên. So sánh JSON dùng SHA-256.

## Tóm tắt snapshot ban đầu trước khi sửa

Đã xác nhận sai lệch lớn ngay từ luồng first-open/onboarding và một số core engine. Báo cáo migration hiện tại ghi “giống 100%” và “không dùng mock/sample data”, nhưng các nhận định này không khớp với code đang có. Tài nguyên bài tập đã được sao chép chính xác; phần giao diện và state/engine cần sửa đáng kể.

## Snapshot ban đầu: 1. Luồng khởi động và onboarding

| Màn/luồng | Bản gốc | Bản hiện tại | Kết quả rà soát |
|---|---|---|---|
| Splash Compose | Source gốc `ui/splash/SplashScreenKt.java` gọi `R.drawable.splash_robot`, ảnh 148dp bo góc 34dp, title “Engla: Learn Language with AI” và progress bar 180×6dp; source còn gọi `AppBanner`. | `ui/splash/SplashScreen.kt` dùng cùng ảnh `splash_robot.png`, title và kích thước tương ứng. SHA-256 ảnh hiện tại trùng ảnh gốc `splash_robot.png`. Không thấy nhánh `AppBanner` tương ứng. | **Khớp asset và phần bố cục chính của Splash Compose; phần banner/ads không được port.** Chưa xác nhận banner có hiện theo trạng thái tài khoản hay pixel/runtime; `splash_owl.png` và subtitle `fo_splash_*` thuộc splash/FirstOpen SDK riêng, không phải Compose Splash này. `SplashScreen.kt:40-103`; JADX `ui/splash/SplashScreenKt.java:169-174,203-214`. |
| First-open intro | Bản gốc FirstOpen cấu hình 3 trang giới thiệu có artwork/title/description và thao tác swipe trước survey; có `fo_img_onboarding_1..3.webp` và animation riêng. | Nhánh khởi động hiện tại trong `App()` không gọi FirstOpen SDK/intro route; sau splash đi thẳng tới form hồ sơ 5 bước. `bg_onboard1..3.webp` có trong drawable nhưng không được tham chiếu bởi Kotlin; ảnh gốc `fo_img_onboarding_*` không có trong Compose resources. | **Ba trang first-open bị bỏ khỏi luồng thật.** Đây là nguyên nhân UI sai ngay sau splash; không nhầm với form hồ sơ onboarding 5 bước bên dưới. `App.kt:154-182`; strings gốc `fo_onboarding_*`; `AppApplication.java:297-351`. |
| Chọn ngôn ngữ first-open | Bản gốc FirstOpen nhận ngôn ngữ app, đồng thời map sang native language, rồi survey lưu ngôn ngữ muốn học. `Languages.native` có 16 lựa chọn. | Root `App()` không render `BaseRoutes.StartupLanguage`; `BaseLanguages` 10 mục chỉ được dùng trong private language screen cho Settings. `ui/settings/LanguageScreen.kt` 16 mục không có call site. | **First-open language flow chưa được nối.** `App.kt` chỉ xét route `LanguageSettings`; `BaseAppViewModel`/DataStore language flow không điều khiển startup UI. `App.kt:154-182, 296-308`; `BaseAppRepository.kt:37-47`; gốc `FirstOpenNextAction.java`. |
| Form hồ sơ 5 bước | Bản gốc có `OnboardingScreen`/`OnboardingViewModel` cho learn language, native language, level, daily minutes, tutor voice; ViewModel lưu từng bước vào onboarding progress. | Form 5 bước tương tự đang được gọi trực tiếp ở first-open. Chọn lựa ghi vào `UserPrefs` trong memory; màn dùng `remember`, không phục hồi `OnboardingState`. Khi hoàn tất, gọi `advanceOnboarding()` nhưng ViewModel chỉ chấp nhận route `Onboarding`; startup resolver trả `StartupLanguage`/`Home`, còn root không chuyển route sang `Onboarding`. | **Form có nhưng đặt/nối sai flow và không bền.** Trên cài mới, `completeOnboarding()` của DataStore không được gọi qua nhánh này; `UserPrefs.onboardingDone` mặc định false khi process mới nên wizard hiện lại. Đây là suy luận control-flow từ các guard/call-site đã đọc. `ui/onboarding/OnboardingScreen.kt:70-81, 228-245`; `OnboardingModels.kt:19-117`; `BaseAppViewModel.kt:42-55, 111-132, 160`; `App.kt:154-182`. |

## Snapshot ban đầu: 2. Core và dữ liệu đã kiểm tra

| Khu vực | Bằng chứng hiện tại | Mức độ / ảnh hưởng |
|---|---|---|
| Seed dữ liệu Home/Progress | `UserPrefs` khởi tạo streak=5, 45 phút, 128 từ, 3 lịch sử học, bài Path/shadowing và từ đã lưu mẫu. | **P0/P1 – dữ liệu demo hiện như dữ liệu người dùng thật.** Home/Progress sai ngay lần mở mới. `UserPrefs.kt:30-70`. |
| Timestamp và lưu state | Nhiều thao tác ghi timestamp cố định `1730190000000L`; `UserPrefs` giữ state trong các `mutableStateOf`/list/map. `AndroidAppRepository` chỉ lưu language/onboarding flags bằng DataStore. | **P1 – tiến độ không phản ánh thời gian thật và phần lớn state học tập không bền qua process restart.** Ví dụ `UserPrefs.kt:82-123, 158-200`; `AndroidAppRepository.kt:16-58`. |
| Streak/Plan | `recordStreakDay()` chỉ tăng số; `PlanProgress.todayDay()` luôn trả 1, XP/diamonds khởi tạo 150/50, dữ liệu Plan giữ trong memory. `BYPASS_PAYWALL=true` làm mọi ngày mở khóa. | **P1 – không khớp streak theo ngày/TrustedClock và lịch 30 ngày; plan đang ở chế độ test.** `UserPrefs.kt:110-112`; `PlanProgress.kt:43-109`; `AppConfig.kt`. |
| Micro/chấm phát âm | Manifest không có `RECORD_AUDIO`; `HoldToSpeakMic` chỉ đổi boolean; callback Speaking gọi `PronunciationScorer.score(sentence, sentence)`. | **P0 – không thu âm/STT; khi nhấn dừng, đầu vào chấm điểm bằng chính đáp án nên ra 100%, bất kể người dùng nói gì.** `AndroidManifest.xml:2-40`; `Atoms.kt:173-215`; `SpeakingScreens.kt:326-338`. |
| TTS | Nút nghe ở Speaking/Vocabulary/Path/Shadowing có callback dạng no-op (`/* TTS playback trigger */`, `/* Audio pronunciation */`). | **P1 – nút có vẻ hoạt động nhưng không phát âm.** `VocabScreens.kt`, `SpeakingScreens.kt`, `PathLessonScreen.kt`, `VideoShadowingScreens.kt`. |
| Video Shadowing | Player hiện là khung gradient + emoji/tên người nói; không có `ExoPlayer`/Media3 hoặc URL video trong player. Tốc độ chỉ đổi state, chu kỳ hiện tại 1.0→0.75→0.5, trong khi tài liệu mô tả 0.75/1.0/1.25. | **P0/P1 – đây là mô phỏng hội thoại, chưa phải video player/shadowing gốc.** `VideoShadowingScreens.kt:448-498, 508-521`; dependency/engine chưa thấy trong source hiện tại. |
| Offline/local-first | `App()` hiển thị `OfflineRobotScreen` trước splash/form/Home khi mất mạng; JSON practice/path nằm trong bundle. | **P1 – chặn cả nội dung học offline, trái mô tả local-first trong TECH_SPEC.** `App.kt:121-163`; TECH_SPEC `:48-50`. |
| Lock-screen learning/notifications | Manifest hiện chỉ khai báo `MainActivity`; không có `LockScreenNotiActivity`, `RECORD_AUDIO`, exact alarm, receiver/service tương ứng. | **P1 – phần lock-screen card/scheduler trong TECH_SPEC chưa được port.** `AndroidManifest.xml:4-40`. |
| LLM credential | Credential được ghi rõ trong tài liệu nguồn và cũng được đặt trong `AppConfig.kt`, rồi gửi qua header trong `LlmApi.kt`. | **P0 bảo mật – secret nằm trong client mobile và đã xuất hiện trong tài liệu giải nén.** Không lặp lại giá trị trong báo cáo; cần thu hồi/xoay khóa và chuyển xác thực/quyền sang backend trước khi dùng thật. `AppConfig.kt`, `LlmApi.kt`. |

### Phần dữ liệu đã đối chiếu đạt

- `apktool/assets/practice/`: 155 JSON gốc; KMP: 155; **155/155 giống SHA-256**.
- `apktool/assets/path/`: 36 JSON gốc; KMP: 36; **36/36 giống SHA-256**. `TECH_SPEC_REPRODUCTION_REPORT.md` mô tả path data cho 12 ngôn ngữ; con số 12 là số ngôn ngữ, còn 36 file tương ứng 3 cấp độ × 12 ngôn ngữ.
- Danh sách 60 title trong `PracticeScreen.kt` khớp nguyên văn **60/60** với danh sách trong source JADX `PracticeScreenKt.java`.
- So sánh 76 drawable hiện có với resource gốc: **63 file trùng SHA-256**; 13 file lệch theo basename gồm 3 `bg_onboard*` và 10 ảnh cờ. `bg_onboard*` hiện không được UI tham chiếu; hash khác không tự chứng minh cờ nhìn khác. `splash_robot.png` đang dùng **trùng SHA-256** với resource gốc; `splash_owl.png` là asset của nhánh FirstOpen riêng.

## Snapshot ban đầu: 2.1. Rà soát theo từng màn hình/đích điều hướng

| Màn/đích | Bằng chứng ở source hiện tại | Kết quả đối chiếu |
|---|---|---|
| Home | `HomeScreen` dựng thứ tự Welcome + số liệu, hero “Ready for Today's Lesson?”, bốn ô practice, thẻ 7 ngày và từ “Confidence” cố định. Callback `onOpenPlan` nhận vào nhưng không được dùng. | **Sai cấu trúc/copy so với Home gốc.** Source gốc có nhánh `PlanHeroCard`, `NewFeatures`, Shadowing và More practice; các chuỗi header/hero/từ cố định hiện tại không có trong danh sách chuỗi Home gốc đã trích. Chưa so pixel trên thiết bị. `ui/home/HomeScreen.kt:56-309`; JADX `ui/home/HomeScreenKt.java`, `HomeLayoutBKt.java`. |
| Bottom navigation | Hai enum đều có thứ tự Home, Practice, Learning/Lesson, Progress, Settings; tab Lesson hiện mở `LearningPathScreen`. | **Khớp cấu trúc tab ở mức source.** Chưa xác nhận kích thước/icon/selected state khi chạy. `ui/components/AppBottomNav.kt:38-47`; `ui/main/MainScreen.kt:84-123`; JADX `ui/components/MainTab.java`. |
| Practice | Danh sách 60 tên và hai bộ JSON practice/path khớp source/tệp gốc theo các phép đối chiếu ở trên. | **Nội dung tĩnh đã xác minh khớp.** Bộ lọc, thứ tự, thao tác và layout vẫn cần đối chiếu runtime. `ui/practice/PracticeScreen.kt`. |
| Vocabulary | Flashcard lấy dữ liệu theo state; nút nghe là no-op; nhấn mic không thu âm, rồi so `currentWord.word` với chính nó để tính điểm. | **Sai engine tương tác; điểm phát âm giả 100%.** `ui/vocab/VocabScreens.kt:273-275, 376-390`; `components/Atoms.kt:173-215`. |
| Grammar | Quiz lấy câu hỏi qua `StaticContent.grammar`, đối chiếu `answerIndex`, tăng điểm theo đáp án chọn. | **Core chấm đáp án có đường xử lý thật ở source.** Chưa kết luận giao diện/câu hỏi có khớp đầy đủ bản gốc; cần kiểm kê theo bộ dữ liệu và ảnh màn. `ui/grammar/GrammarScreens.kt:154-170, 281-326`. |
| Speaking | Mic chỉ đổi cờ UI; khi thả mic, scorer so câu mẫu với chính câu mẫu; nút nghe không có playback. | **Sai engine và kết quả.** `ui/speaking/SpeakingScreens.kt:274-276, 326-338`; manifest chưa khai báo quyền mic. |
| Shadowing | Player là scene card gradient/tên người nói; tốc độ thay state UI, không có player; nút nghe cũng no-op. | **Chưa phải video shadowing.** `ui/shadowing/VideoShadowingScreens.kt:448-498, 508-521, 583`; không thấy Media3/ExoPlayer trong dependency/source hiện tại. |
| Learning Path | Danh sách 36 unit và toàn bộ tên lesson đã đối chiếu với `PathCourse.java`; bài học tải `PathLessonContent` từ JSON; 36 JSON path khớp byte. Nút nghe no-op, câu hỏi speaking vẫn dùng mic giả. | **Danh mục và tệp bài học khớp; engine audio và progress chưa đúng.** So sánh tuple (level, unit, title, lesson list) cho kết quả 36/36 giống. `data/PathCourse.kt`; `data/PathLessonContent.kt`; `ui/learningpath/PathLessonScreen.kt:82-88, 221-223, 242-358`. |
| Plan | `PlanProgress` giữ state trong memory, mặc định travel/Learner, XP=150, diamonds=50; `todayDay()` luôn 1; BYPASS mở mọi ngày. | **Test state, không phải progress thật/TrustedClock.** `data/PlanProgress.kt:43-109`; `AppConfig.kt`. |
| AI Tutor / Today's Lesson | Chat dùng `TutorAi`/`LlmApi`; tin nhắn chỉ giữ trong state của màn. LLM client gửi request trực tiếp từ app và giữ credential trong source. | **Phụ thuộc mạng và có lỗi bảo mật P0 đã nêu; lịch sử chat không bền qua lần mở màn/process.** `ui/tutor/AiTutorScreen.kt:92-141`; `data/LlmApi.kt` (không chép giá trị credential vào báo cáo). |
| Progress / History | Progress đọc trực tiếp streak, minutes, words và history từ `UserPrefs`; History thao tác trên list state đó. | **Hiển thị số liệu seed và chưa lưu bền; số liệu không đại diện lịch sử học thật.** `data/UserPrefs.kt:30-58`; `ui/progress/ProgressScreen.kt:58-61`; `LearningHistoryScreen.kt:52-54`. |
| Settings | Banner “Pro Unlocked” được vẽ cố định trong khi cờ bypass đang bật. Level/daily goal/voice chỉ cập nhật `UserPrefs` trong memory; speech rate chỉ đổi state cục bộ, không nối TTS. Rate Us và Report Issue chỉ đóng dialog, không gửi review/report. | **Chưa phải hành vi production; các tuỳ chọn chưa bền hoặc chưa nối dịch vụ.** `ui/settings/SettingsScreen.kt:101-148, 156-231, 365-379`; `AppConfig.kt`. |
| Language settings | Root dùng private `LanguageScreen` dựa trên `BaseLanguages` (10 mục); composable ở `ui/settings/LanguageScreen.kt` có 16 locale nhưng không có call site. Mở Settings gọi `BaseAppViewModel.openLanguageSettings()`, chỉ đổi route nếu VM đang ở Home. | **Screen 16 mục là code không dùng; screen 10 mục phụ thuộc route. Startup flow hiện không đưa VM tới Home nên đường đổi ngôn ngữ cũng chưa được nối đúng.** `App.kt:145-182, 296-308`; `BaseAppViewModel.kt:135-148`; `ui/settings/LanguageScreen.kt:39-49`. |
| Legal | Privacy Policy nói profile, chat history, practice scores và history được lưu trên thiết bị; AI Tutor lại gửi message history trong request lên LLM endpoint. | **Chưa công bố rõ việc gửi nội dung chat tới dịch vụ mạng; cần sửa nội dung privacy trước phát hành.** `ui/settings/LegalScreen.kt:25-40`; `ui/tutor/AiTutorScreen.kt:116-141`; `data/LlmApi.kt:63-92`. |
| Offline | Màn robot có tương tác animation, nhưng root hiện chặn toàn app khi offline thay vì chỉ khóa tính năng cần mạng. | **Local lessons không dùng được offline dù JSON đã đóng gói.** `App.kt:152-215, 250-265`; `ui/offline/OfflineRobotScreen.kt`. |
| Điều hướng route | `Routes.kt` khai báo 29 route (comment ghi 28); không có NavHost/controller và các helper `Routes.*` không có call site; sub-screen dùng `ActiveSubScreen` 19 nhánh. Root first-open lại điều khiển bằng `UserPrefs.onboardingDone`, không dùng các route `StartupLanguage`/`Onboarding` của VM. | **Hai cơ chế điều hướng rời nhau.** Một số Plan route constants chưa có nhánh riêng; cần lập mapping route gốc → screen Compose và nối startup/back/state restore. `navigation/Routes.kt`; `App.kt:145-182, 709-729, 737-910`; `BaseAppViewModel.kt:111-160`. |

Ghi chú cho các hàng có kết luận “khớp ở mức source”: chưa có APK gốc hoặc ảnh tham chiếu runtime trong thư mục tài nguyên, nên đây chưa phải xác nhận pixel-by-pixel. Các chuỗi và lời tự mô tả trong code (ví dụ “100% migrated”) là claim của source cần kiểm chứng, không phải bằng chứng tự thân.

## Snapshot ban đầu: 2.2. Đối chiếu chỉnh sửa UI/flow mới và báo cáo người dùng cung cấp

Đã đọc `/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/MIGRATION_PROGRESS.md`. Bản này và `/Volumes/data/AndroidStudioProjects/ailearn/MIGRATION_PROGRESS.md` cùng 107 dòng, cùng SHA-256. Các câu chỉ thị được trích dẫn trong tài liệu (bao gồm yêu cầu không dùng sample) là yêu cầu lịch sử, không phải bằng chứng đã hoàn thành hay lệnh mới trong lượt này; phạm vi hiện tại là rà soát thay đổi và ghi kết quả vào audit.

- **Điểm đã sửa đúng trong bản cập nhật này:** `splash_robot.png` khớp byte với resource gốc và source Splash Compose gốc cũng dùng robot. `AppColors`/`Theme` đã chuyển về light palette và dùng semantic tokens nhiều hơn; bottom navigation có divider 1dp. `MainActivity` bật edge-to-edge, làm status bar trong suốt và chỉ ẩn navigation bar; cách ẩn navigation bar khớp `MainActivity.java` gốc. Mô tả “ẩn cả status bar” trong báo cáo tiến độ không đúng với code hiện tại hoặc host gốc.
- **Onboarding vẫn thiếu một phần của flow gốc:** màn khảo sát 5 bước là một màn có trong app gốc, nhưng flow FirstOpen của SDK (chọn ngôn ngữ giao diện/native, 3 trang intro và survey) là nhánh riêng chạy trước. Root hiện tại đi `offline → Splash Compose → form 5 bước → MainAppHost`, không gọi FirstOpen; vì vậy việc có form 5 bước không chứng minh launch flow đã khớp hoàn toàn.
- **Điều hướng mới có lỗi nối state có thể tái hiện từ control flow:** khi hoàn tất form, `App()` đặt `UserPrefs.onboardingDone=true` rồi gọi `advanceOnboarding()`. Hàm này chỉ xử lý khi `uiState.route == BaseRoutes.Onboarding`, nhưng resolver trả `StartupLanguage` trên trạng thái first-run và `Home` sau khi first-open đã hoàn tất; root không chuyển VM sang `Onboarding`, nên cả hai nhánh đều bị guard từ chối. `repository.completeOnboarding()` không được gọi; root chỉ hiện `MainAppHost` nhờ cờ `UserPrefs` trong memory, cờ này không được khôi phục từ DataStore sau process restart. Root cũng đã bỏ hiệu ứng gọi `onHomeShown()`, nên first-open completion không được đánh dấu qua nhánh Home. Trên first-run, `openLanguageSettings()` cũng không mở vì chỉ đổi route khi VM đang ở `Home`. Đây là suy luận tĩnh từ guard/call-site, chưa xác nhận bằng chạy app. `App.kt:145-182`; `BaseAppViewModel.kt:111-153`; `BaseAppRepository.kt:37-46`; `UserPrefs.kt:60-76`.
- **Home chưa đạt mức “100%”:** dù palette và một số card đã đổi, source hiện tại vẫn có hero “Ready for Today's Lesson?”, từ vựng “Confidence”/định nghĩa cố định, callback `onOpenPlan` không dùng và màu inline trong `HomeScreen.kt`; đây là khác biệt có thể thấy khi đối chiếu source với `HomeScreenKt.java`/`HomeLayoutBKt.java`. Onboarding cũng còn tiêu đề và nút “Continue” viết cứng tiếng Anh. Divider/spacing/theme là chỉnh sửa source-level; chưa có screenshot runtime để xác nhận tỉ lệ hoặc selected state.
- **Mức xác minh của tài liệu tiến độ:** tài liệu ghi các lệnh Gradle đã thành công và APK 66.2 MB; audit này không chạy lại build/test sau các chỉnh sửa đang có và không chụp màn trên thiết bị, nên chỉ ghi nhận đó là kết quả được báo cáo, chưa xác nhận nó áp dụng cho working tree hiện tại. Các tỷ lệ “100%” cho UI/Home không có ảnh so sánh đi kèm và bị các sai lệch source ở trên phản chứng. Tài liệu cũng có dòng chứa thông tin xác thực; giá trị đã được lược bỏ khỏi audit và không được lặp lại ở đây.

## Snapshot ban đầu: 3. Đối chiếu với các tuyên bố trong tài liệu migration

- `MIGRATION_PROGRESS.md` khẳng định 100% cho theme, Splash, onboarding, Home và navigation. Source xác nhận asset robot đúng với Compose Splash, nhưng first-open flow chưa nối; Home còn nội dung/màu inline khác source gốc; và chưa có screenshot runtime chứng minh pixel fidelity. Câu “không sample” ở dòng chỉ thị là yêu cầu được trích dẫn, không phải lời xác nhận rằng dữ liệu hiện tại không có seed; source `UserPrefs` vẫn chứa seed và timestamp cố định. Báo cáo cũng ghi build thành công, nhưng audit này không chạy lại.
- Phần Practice JSON và danh sách 60 title có bằng chứng khớp; đây là phần hiện đã xác minh tốt.
- `TECH_SPEC_REPRODUCTION_REPORT.md` mô tả Local-First, TrustedClock, audio/video engine và lock-screen cards. Các engine/manifest tương ứng chưa có trong source KMP hiện tại.
- Các câu lệnh trong tài liệu nguồn (ví dụ đưa credential vào client) được ghi nhận như nội dung cần đánh giá, không thực hiện như yêu cầu của người dùng.

## Kế hoạch tiếp theo theo thứ tự ưu tiên

1. Chụp/đối chiếu runtime theo luồng: Splash → language picker → 3 intro → survey → từng bước hồ sơ → Home; kiểm tra back/skip/force-stop.
2. Đối chiếu cấu trúc Home và toàn bộ tab/subscreen với nhánh source gốc; ưu tiên Practice, Vocabulary, Grammar, Speaking, Learning Path, Plan, Tutor, Progress, History và Settings.
3. Sửa và xác minh các tính năng còn thiếu: iOS STT, Shadowing media player/subtitle, số phút mọi loại practice, Plan unlock/streak theo lịch gốc, billing/support.
4. Đối chiếu Privacy Policy với request thật, processor, retention và analytics; xác nhận backend auth/quota; rotate credential từng lộ.
5. Mở app trên thiết bị đã cập nhật để xác minh Day 1 và các flow; sau đó chụp screenshot, kiểm tra offline và tái kiểm tra Goodbye → lesson.

## Giới hạn xác minh ban đầu

Đoạn này mô tả trạng thái lúc bắt đầu audit; hiện source đã được biên dịch Android/iOS nhưng chưa chạy app, test hoặc chụp screenshot runtime. Các finding lịch sử cần đọc cùng bảng cập nhật mới nhất ở đầu báo cáo.

## Runtime follow-up — Android 2026-10-30

Các kết quả này được xác nhận trên Xiaomi `c9bcc000` với APK debug mới cài bằng `adb install -r`; dữ liệu app được giữ nguyên.

| Cụm | Kết quả hiện tại | Còn thiếu |
|---|---|---|
| Microphone permission | `MainActivity` cung cấp Activity Result registry owner; Today Lesson mở bình thường. Dialog xin mic đã đổi sang Compose custom theo màu/font Engla, đã sửa key dịch; Hủy đóng hộp, Tiếp tục mở system permission sheet. Hủy sheet bằng Back; quyền vẫn `granted=false`. | Chưa cấp quyền nên chưa test transcript STT; các màn khác dùng chung recognizer nhưng chưa chạm mic từng màn trên thiết bị. |
| My Path / Plan | Mở 30-day plan và Start Day 1 không crash; state 3/4 activity được giữ như dữ liệu trước test. | Chưa chạy hết activity hoặc hoàn tất ngày để tránh ghi thêm tiến độ. |
| Lesson / Learning Path | Unit 1 hiển thị; node `Goodbye` mở câu hỏi đúng ngữ nghĩa về lời chào khi rời đi. Không có FATAL/IndexOutOfBounds. | Chưa trả lời hoặc hoàn tất lesson; connector completion animation chưa được chạy đủ trên thiết bị. |
| Shadowing | Remote list và thumbnail lên; lọc Daily life/Travel hoạt động; player mở, transcript/câu dịch/chip từ hiện. Logcat có MediaCodec video/audio decode, không có FATAL. | Ảnh chụp không đủ chứng minh frame/audio đồng bộ từng từ; timecode nguồn chỉ ở cấp câu. |
| Language | Settings → Language mở, Vietnamese được đánh dấu chọn. | Chưa replay first-run language picker/onboarding trên fresh install. |
| Voice | Onboarding source có Male/Female/Random/Robot; giá trị được lưu trong `UserPrefs.tutorVoice`, Android TTS tìm voice cùng ngôn ngữ/giới tính gần nhất có sẵn. | Chưa test TTS từng lựa chọn trên thiết bị; không reset onboarding/user state. |
| Hint | Badge `AD` được gỡ khỏi Hint trong Today Lesson. | — |

Build sau dialog UI: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug` thành công; 16 tests, 0 failure/error/skip. `git diff --check` sạch. APK mới đang foreground ở Home; quyền microphone chưa được cấp.


## Kiểm chứng tích hợp bổ sung — Android 2026-10-30

- Bản debug tích hợp mới nhất được cài lại trên Xiaomi Redmi Note 13 `c9bcc000` (Android 15, 1080×2400); app vẫn ở foreground. `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug --console=plain` thành công; 23 test, 0 failure/error/skip. `git diff --check` sạch; logcat mẫu không có FATAL/ANR.
- **Learning Path / kiểm tra:** Đã đối chiếu màn lộ trình với tài nguyên app gốc; chỉnh căn giữa node đơn, khoảng cách đầu màn, phản hồi node khóa, bố cục câu hỏi/feedback và dialog thoát có bản dịch. Trên máy thật, trả lời sai giữ feedback đúng câu, Continue chuyển sang câu kế không lóe toast; dialog thoát Stay/Leave hoạt động. Hoàn tất bài 15 câu với 11 đúng và 4 câu nói được Skip; hai node và các đoạn nối đổi sang trạng thái hoàn thành màu xanh.
- **Bảo toàn dữ liệu:** lượt hoàn thành bài chỉ dùng để kiểm tra hiệu ứng path; DataStore được khôi phục và so sánh byte chính xác (SHA-256 trước/sau giống nhau). Home sau đó vẫn hiện dữ liệu ban đầu 3/4 hoạt động, 9 phút.
- **Giọng nói / nhân vật:** Today Lesson mở được trên thiết bị. Dialog giải thích mic tiếng Việt → system permission sheet → trạng thái Listening… đều chạy; đã thu hồi quyền ngay sau thử (`RECORD_AUDIO granted=false`). Robot hiện đúng trong lesson. Chưa phát âm kiểm tra từng voice hoặc đọc lời thật để xác nhận độ chính xác transcript; AI Tutor persona picker không được kiểm tra trực tiếp trên runtime trong lượt này.
- **Thống kê:** Progress mở được và render; hiện hiển thị 0 ở các bộ đếm nhóm và streak 1 theo dữ liệu đã lưu sẵn. Hạch toán ngày theo timezone thiết bị/đếm phút, điểm và skip có 7 regression tests riêng đạt; không tạo thêm hoạt động trong dữ liệu người dùng. Chưa xác nhận giá trị thống kê sau một session thật vì đã hoàn tác dữ liệu test.
- Đã lấy lại màn app và kiểm tra activity top-resumed sau thử mic; không thấy FATAL/ANR trong logcat mẫu. Chỉ xác minh Android ở lượt này; chưa chạy lại iOS.

## My Path — đối chiếu và xác minh Android, 2026-10-30

| Hạng mục | Bản gốc / trước sửa | Cập nhật hiện tại |
|---|---|---|
| Đường đi | Gốc dùng đường ziczac dọc, connector cong nét đứt và node trạng thái. Clone trước sửa là 3 node ngang, thiếu đường nối. | PlanScreen.kt vẽ node dọc theo ziczac, connector nét đứt, robot ở ngày hiện tại, khóa và trạng thái hoàn thành. |
| Vị trí chặng | Gốc khởi tạo list ở stage của currentDay. Clone luôn mở Stage 1. | Lazy list khởi đầu và tự cuộn theo stage hiện tại. |
| CTA tiến độ | Gốc dùng Continue khi ngày hiện tại đã có hoạt động. Clone vẫn ghi Start. | CTA dùng Continue / Start theo saved activities; trên máy đang là “Tiếp tục Ngày 1” với 3/4. |
| Thanh tiến độ | Clone dùng Material progress indicator và xuất hiện chấm rời cuối track. | Thanh riêng bo tròn, fill gradient và nhãn x/30; Day Activities dùng cùng component. |
| Mở ngày theo lịch | Gốc dựa vào isUnlocked(day); core clone còn yêu cầu day == currentDay khi hoàn tất. | Xóa ràng buộc currentDay khỏi PlanFlow.finish và completeDay; chỉ ngày đã mở theo lịch mới hoàn tất được. Thêm test regression cho Day 2. |
| Ngày địa phương | Gốc dùng ngày cài đặt app và lịch địa phương; clone dùng epoch UTC kể từ lúc tạo plan. | Android lấy first-install day và so với ngày địa phương của thiết bị. iOS giữ fallback ngày bắt đầu plan, chưa kiểm tra. |
| Route vào ngày khóa | Home có thể gọi trực tiếp Day Activities dù ngày chưa mở. | Ẩn Start/Continue của ngày khóa ở Home; Day Activities kiểm tra khóa trước khi dựng activity. |

Kiểm chứng: testAndroidHostTest + assembleDebug thành công, 24 tests không lỗi; git diff --check sạch. Bản mới cài lên c9bcc000; screenshot My Path và Day 1 xác nhận 3/4 không đổi, không hoàn tất thêm activity; không thấy FATAL/ANR ở logcat gần nhất. Screenshot: [My Path](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-my-path-final.png), [Day 1](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-my-path-day-final.png). Chưa thử trạng thái stage >1 trên thiết bị để tránh chỉnh giả dữ liệu hoặc ngày.

## My Path screenshot A/B recheck — Android, 2026-10-30

The user's screenshot comparison was correct: the previous visual review did not compare the complete screen against the original package. It over-weighted the zigzag core path and treated the small generic robot face as sufficient. The original screen has a blue-tinted background, “My Learning Path” header, paired Goal/Assistant cards, a large gradient Stage banner, a layered day node, a full 3D robot with a speech bubble/clouds, and a fixed duration/level/start bar. The old clone missed several of those assets and regions.

`PlanScreen.kt` has now been updated to use that composition and the existing original artwork (`plan_james_body`, `plan_james_arm`, `plan_cel_body`, stage suitcase/plane): paired goal/assistant cards, progress row, gradient stage card, 3D-style path nodes and dashed connectors, floating sleep/wake robot, cloud accents, and a fixed minutes/level/Start-or-Continue footer. The premium diamond action is still omitted as requested. Tapping the Assistant card now updates both the saved voice and visible card immediately.

Evidence: original reference [ailearn-my-path-original-reference.png]({original}), updated device capture [ailearn-my-path-final.png]({current}), and A/B artifact [MY_PATH_SCREENSHOT_COMPARISON.md]({comparison}). The clone now preserves its saved Travel goal, James voice, and Day 1 3/4 progress; it does not mimic the original account's Everyday Conversation goal or 0/4 progress. Day 2 remains locked because this install is one calendar day newer than the original install. The screen layout is much closer, but these user-state differences mean the captures are not a pixel-identical data comparison.

Verification: `:shared:testAndroidHostTest` and `:androidApp:assembleDebug` succeeded (24 tests, 0 failures/errors/skips); `git diff --check` is clean. The rebuilt APK was installed with `adb install -r` on `c9bcc000` without clearing data. My Path opened; the assistant card toggled James → Emma → James, and the robot bubble toggled to “Ready to learn?” and back. The app remained foregrounded; no FATAL/ANR was found in the sampled recent logcat. No lesson was started and progress was not changed.


## Cập nhật — My Path setup và progress bars, Android 2026-10-30

Finding runtime mới: `PlanProgress` giữ `conversation`, còn catalog gốc chỉ có `everyday`, khiến `PlanCatalogData.goal()` trả null và Path báo không khả dụng. Thêm tương thích cho các goal ID legacy; các plan cũ được hiển thị từ catalog hiện hành, plan tạo mới lưu ID canonical. Màn setup lấy lựa chọn từ catalog và hỗ trợ loading/retry, kiểm tra nội dung theo ngôn ngữ đích, ghi chú goal tùy chỉnh; CTA Path chỉ xuất hiện khi goal hợp lệ.

Đối chiếu setup với resource gốc: dùng lại `plan_start_robot`, `plan_survey_name`, `plan_survey_level`, `plan_survey_time`, `plan_survey_assistant`; câu hỏi goal lấy tên ngôn ngữ hiển thị thay vì mã `en`. Thay Material `LinearProgressIndicator` bằng component custom dùng track Engla `#EFEFF1`, giữ nguyên màu fill xanh hiện có theo từng màn và loại bỏ chấm cuối. `rg LinearProgressIndicator shared/src/commonMain/kotlin` không còn usage.

Kiểm chứng: host suite + Android debug assemble thành công (26 tests, không lỗi); thiết bị `c9bcc000` mở My Path và flow setup đến Goal, không thấy nội dung unavailable, progress track xám tách nền và không có chấm. `git diff --check` sạch; app foreground, không có FATAL/ANR trong 500 logcat entries gần nhất. Không submit plan; DataStore được khôi phục byte-for-byte (SHA-256 trước/sau giống nhau). Giới hạn: chưa submit wizard để tránh thay đổi plan và tiến độ người dùng.


Ảnh thiết bị: [My Path + track tiến độ](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-my-path-progress-track-2026-10-30.png), [màn chọn goal + progress](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-plan-setup-goal-progress-2026-10-30.png).


## My Path visual follow-up — Android 2026-10-30

Đã tăng điểm bắt đầu nội dung lộ trình 16dp, đưa `START HERE`, node và robot xuống thấp hơn để tách khỏi Stage banner; bubble/mây giữ cùng hệ tọa độ. Thêm bóng oval mờ dưới robot, đối chiếu với ảnh gốc. Xác minh ảnh trên Xiaomi `c9bcc000`; build + host tests đạt (26/26), `git diff --check` sạch. Không thay đổi tiến độ người dùng; DataStore đã restore cùng SHA-256 trước/sau. Ảnh: [My Path sau chỉnh sửa](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-my-path-start-here-robot-shadow-2026-10-30.png).

## Video Shadowing — vocabulary highlight và CTA lưu từ — 2026-10-30

| Hạng mục | Kết quả rà soát |
|---|---|
| Đánh dấu từ mới | Khớp token chính xác sau khi chuẩn hóa chữ hoa/thường và dấu câu biên; tránh match chuỗi con. Lấy từ theo script language hiện tại. |
| Kiểu từ mới | Chip nền xanh nhạt + viền xanh 1dp, chữ màu chính; màu karaoke không ghi đè chữ trong chip. Khi token sai, trạng thái lỗi đỏ được ưu tiên. Token thường giữ highlight karaoke, không có chip. |
| Lưu từ | Save all new words (N) lưu các từ còn thiếu; trạng thái kế tiếp mở saved words. Đã kiểm tra runtime 10/10 từ và khôi phục snapshot sau test. |
| Điều khiển | ENG/VIE đổi câu hiển thị; chạm vùng video sang câu kế; scrubber tròn; mic dùng icon hệ thống và hỗ trợ tap/nhấn giữ ở mã nguồn. |

Build/test trên Android thành công (26/26 host tests), git diff --check sạch. Trên c9bcc000, đã kiểm tra dịch câu, chuyển câu và CTA lưu từ; SHA-256 DataStore khôi phục giống trước test. Ảnh: [gốc](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-original-player-2026-10-30.png), [clone hiện tại](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-current-player-final-2026-10-30.png).

Chưa xác minh quyền mic/STT trên thiết bị. Mốc trong dữ liệu hiện ở cấp câu nên màu karaoke từng từ là nội suy thời gian, chưa phải forced alignment theo âm thanh.

## Shadowing: tránh phát âm thanh mẫu trong lúc thu mic — 2026-10-30

Luồng bắt đầu thu nay gọi pause video trước start nhận dạng. Khi thu kết thúc, player vẫn pause; muốn nghe mẫu lại thì người học bấm Replay/Play. Mục đích là ngăn audio mẫu bị thu lẫn vào mic/transcript/chấm phát âm. Thay đổi source mới; chưa build hoặc xác minh trên thiết bị trong lượt này.

## Shadowing runtime recheck — Android, 2026-10-30

- Mic/STT thật chạy được trên `c9bcc000`; quyền đã được cấp sẵn. Video dừng ở 0:04 trong lúc nói và còn dừng sau khi nhận kết quả. Transcript thu được là “hi I bought this up yesterday”; người dùng xác nhận nghe nhận dạng như vậy là được, nhưng ghi nhận `shirt` → `up` là khác biệt của engine thực tế. Ảnh: [transcript](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-stt-result-2026-10-30.png).
- Home carousel → video card → player hoạt động; chuyển tới câu cuối và hoàn tất mở dialog, Done đóng về catalog. Sau completion, DataStore tăng đúng một bản ghi Shadowing và đánh dấu video `returning_an_item`; đã khôi phục snapshot sau xác minh.
- Saved Words hiện nghĩa tiếng Việt và tên video nguồn thay vì để trống. CTA lưu tất cả 10 từ và trạng thái đã lưu hoạt động trên máy. Ảnh: [Saved Words](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-saved-words-2026-10-30.png).
- Bỏ stop-indicator thừa ở cuối seekbar Material3 và chỉnh track mảnh/bo tròn 4dp để gần ảnh gốc. Ảnh thiết bị: [seekbar sau sửa](/Users/jenwom/.gemini/antigravity/brain/b1561a54-ccdb-403e-ba4c-38f92b8743d4/ailearn-shadowing-seekbar-fixed-2026-10-30.png).
- Build/host suite thành công: 26 tests, 0 lỗi; `git diff --check` sạch; không thấy FATAL/ANR trong logcat mẫu. DataStore khôi phục byte-for-byte (SHA-256 `49dcc0aefff3d0fe8616f32caafce027f4a4b06711273179dc0737296df038c8`); app mở tại player Shadowing.
- Còn giới hạn: STT mới kiểm tra một câu trên một máy; karaoke từng từ chưa có forced alignment vì dữ liệu gốc chỉ có timestamp cấp câu.
