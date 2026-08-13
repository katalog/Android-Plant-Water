# 식물 물주기 알림 앱 — 진행 기록

## 2026-08-04 오늘 한 것

### 1. 기술 스택 확정
- UI: Jetpack Compose + Material 3 Expressive
- 카메라: CameraX
- 로컬 DB: Room
- 알림/스케줄: WorkManager + NotificationCompat (액션 버튼 포함)
- 식물 인식: Gemini API(Flash/Flash-Lite, 무료 티어) 우선 검토, PlantNet API 대안
  - 무료 한도는 공식 수치와 실사용 후기가 달라서, 실제 개발 시작 시점에 AI Studio에서 재확인 필요
  - API 실패 대비 수동 선택 드롭다운 폴백 필수로 설계에 반영

### 2. 화면 6개 + 흐름 확정
- 1. 홈(식물 목록) / 1-a. 물주기 캘린더 / 2. 사진 찍기 / 3. 인식 중 / 4. 정보 확인 / 5. 일정 등록·수정(공유 화면)
- 저충실도 와이어프레임으로 전체 화면 확인 완료
- 캘린더 화면에 일정 수정 진입점(설정 아이콘) 추가 결정

### 3. 데이터 모델 초안
```
Plant(id, name, species, photoUri, wateringIntervalDays, reminderTime, lastWateredAt)
WateringLog(id, plantId, wateredAt)
```

### 4. 산출물
- `plant_app_navigation.md` — 전체 화면/흐름도/데이터모델 정리본 (`01_plan` 폴더에 저장됨)

---

## 2026-08-05 오늘 한 것

### 1. Room 스키마를 프로젝트에 편입
- `10_codes`에 미리 작성해둔 `Plant`, `WateringLog` Entity, `PlantDao`, `WateringLogDao`, `PlantDatabase`, `PlantRepository`를 `app/src/main/java/com/moonkata/plantwater/data/local/`로 복사
- `app/build.gradle.kts`, 루트 `build.gradle.kts`, `libs.versions.toml`에 Room 2.8.4 + KSP `2.2.10-2.0.2`(Kotlin 2.2.10과 버전 매칭) 의존성/플러그인 추가
- **이슈**: AGP 9.3.1의 "built-in Kotlin" 기능과 KSP가 `kotlin.sourceSets` DSL을 쓰는 방식이 충돌 ([google/ksp#2729](https://github.com/google/ksp/issues/2729), 아직 KSP 쪽 미수정)
  - `gradle.properties`에 `android.disallowKotlinSourceSets=false` 추가해서 우회
  - `./gradlew compileDebugKotlin` 성공 확인 (kspDebugKotlin → compileDebugKotlin)

### 2. 홈 화면 Compose 구현
- `ui/home/HomeScreen.kt` 신규: `Scaffold` + FAB("+") + `LazyColumn`으로 식물 카드 리스트
- `PlantCard`: 썸네일 자리(🌱 placeholder, 아직 실제 이미지 로딩 없음), 이름, "다음 물주기까지 D-N" 상태, "물 줬어요" `FilledTonalButton`
- D-day 계산: `lastWateredAt`(없으면 `createdAt`) + `wateringIntervalDays` 기준, 지나면 "D+N 지난" 표시
- `MainActivity`에서 더미 데이터 3개(몬스테라/산세베리아/금전수)로 렌더링, "물 줬어요" 클릭 시 로컬 state로 `lastWateredAt` 갱신 (Room 연동은 아직 안 함)
- 실기기/에뮬레이터에서 실행 확인 완료, 오류 없음

### 3. 일정 등록/수정 화면(5번) Compose 구현
- `ui/schedule/ScheduleScreen.kt` 신규: 신규/수정 공용 화면
  - "며칠마다 물 줄까요" `Slider`(1~30일) + 알림 시간 `TimePicker`(Material3, `ExperimentalMaterial3Api`)
  - 상단 "닫기" 버튼, 하단 저장 버튼(신규="알림 등록하기" / 수정="수정하기")
  - `isEditMode`, `initialIntervalDays/Hour/Minute` 파라미터로 프리필 지원 (수정 모드 진입 경로는 캘린더 화면이 아직 없어서 미연결)
- `MainActivity`에 Home ↔ NewPlantSchedule 화면 전환(sealed interface `Screen`) 연결: FAB → 일정 등록 화면, 저장 시 새 `Plant`(이름/종은 "새 식물"/"미분류" 임시값, 4번 화면 생기면 교체 필요) 추가 후 홈 복귀
- **버그 발견 및 수정**: `var plants by mutableStateOf(...)`, `var screen by mutableStateOf(...)`를 `remember {}` 없이 선언해서, 상태를 바꿔 recomposition이 일어날 때마다 state 객체가 새로 생성되며 즉시 초기값(`Screen.Home`)으로 리셋됨 → FAB를 눌러도 화면 전환이 바로 원복되어 "아무 반응 없음"처럼 보였음. 둘 다 `remember { mutableStateOf(...) }`로 감싸서 해결
  - **교훈**: Compose에서 `setContent {}` 블록 최상위(컴포저블 함수 스코프) 안에서 `mutableStateOf`를 쓸 때는 항상 `remember`로 감싸야 함. 안 감싸면 컴파일은 되지만 상태가 유지되지 않음 (Lint가 "Creating a state object during composition without using `remember`"로 잡아줌)
- 실기기/에뮬레이터에서 실행 확인 완료: FAB → 일정 등록 → 저장 → 홈 복귀 정상 동작

---

---

## 2026-08-11 오늘 한 것

### 1. Room을 홈/일정 화면에 실제 연동
- `MainActivity`에서 `PlantRepository(PlantDatabase.getInstance(applicationContext))`를 만들어서 로컬 `mutableStateOf` 더미 데이터를 걷어냄
- `HomeScreen`은 `repository.observePlants()` (Flow) → `collectAsState`로 구독, "물 줬어요"는 `repository.markWatered(plantId)` 호출로 교체 (log insert + lastWateredAt 갱신이 한 트랜잭션으로 처리됨)
- 일정 등록(5번, 신규 모드) 저장 시 `repository.addPlant(...)`로 실제 insert, 이름/종은 여전히 "새 식물"/"미분류" 임시값 (4번 화면 생기면 교체)

### 2. WorkManager 알림 등록/취소 로직
- `androidx.work:work-runtime-ktx:2.11.2` 추가
- `reminder/ReminderScheduler` — 식물별 `PeriodicWorkRequest`를 워크 이름 `water_reminder_plant_{id}`로 unique 예약, 재저장 시 `ExistingPeriodicWorkPolicy.UPDATE`로 갱신. 초기 지연시간은 오늘 알림시각이 이미 지났으면 내일로 계산
- `reminder/WaterReminderWorker` (`CoroutineWorker`) — `PlantRepository`로 식물 조회 후 "물 줬어요"/"나중에" 액션 버튼 2개짜리 알림 표시. POST_NOTIFICATIONS 권한 없으면 조용히 스킵
- `reminder/WaterActionReceiver` (`BroadcastReceiver`) — "물 줬어요" 탭 시 앱 안 열어도 `repository.markWatered()` 실행 (`goAsync()`로 suspend 완료까지 프로세스 유지), "나중에"는 알림만 닫음
- `PlantWaterApp`(`Application`) 신규 — 알림 채널(`water_reminder`) 앱 시작 시 1회 생성
- `MainActivity`에서 Android 13+ 대상으로 `POST_NOTIFICATIONS` 런타임 권한을 첫 진입 시 요청
- 매니페스트에 권한, `Application` 등록, `WaterActionReceiver` 등록 추가
- `./gradlew compileDebugKotlin`, `assembleDebug` 둘 다 성공 확인 (에뮬레이터 실기기 알림 수신 테스트는 아직 안 함 — 다음 세션에서 실기기 확인 필요)

---

## 2026-08-11 (계속) — CameraX 연동

### 3. 사진 찍기 화면(2번) 구현
- `androidx.camera:camera-{core,camera2,lifecycle,view} 1.4.2` 추가
- `ui/camera/CameraScreen.kt` 신규: `PreviewView` + `AndroidView`로 풀스크린 프리뷰, 하단 원형 셔터, 상단 "닫기"
  - `CAMERA` 런타임 권한 없으면 권한 요청 화면으로 대체 (권한 허용하기 / 닫기)
  - 촬영본은 `filesDir/photos/plant_{timestamp}.jpg`에 저장 후 `file://` `Uri`를 콜백으로 전달 (앱 내부에서만 쓰므로 FileProvider 없이 처리)
- 매니페스트에 `CAMERA` 권한, `uses-feature camera.any required=false` 추가
- `MainActivity` 내비게이션 변경: 홈 FAB → **카메라**(신규) → 일정 등록(5번, 신규모드). `Screen.NewPlantSchedule`이 `photoUri`를 들고 다니다가 `addPlant()` 호출 시 `Plant.photoUri`에 반영
  - 3(인식 중)/4(정보 확인) 화면이 아직 없어서, 촬영 후 바로 5번으로 건너뜀 — 4번 화면 생기면 그 사이에 끼워 넣어야 함
- `./gradlew compileDebugKotlin`, `assembleDebug` 둘 다 성공 확인 (에뮬레이터/실기기에서 촬영 자체는 아직 테스트 안 함)

---

## 2026-08-11 (계속2) — 남은 화면 전부 구현 (정보확인/Gemini/캘린더/삭제/썸네일)

### 4. 정보 확인 화면(4번)
- `ui/info/InfoScreen.kt` 신규: 사진 썸네일, 이름 입력, 인식 결과(학명/물주기/광량 카드) 표시
- 인식 실패 시 또는 "인식이 틀렸나요?" 버튼으로 수동 모드 전환 → `PlantCatalog`(몬스테라/산세베리아/금전수/스킨답서스/선인장/기타 6종) `ExposedDropdownMenuBox`로 직접 선택
- 확인 시 (name, species, intervalDays)를 5번(신규 모드)으로 전달, "새 식물"/"미분류" 임시값 제거됨

### 5. Gemini API 연동 + 인식 중 화면(3번)
- `recognition/PlantIdentification.kt` — 인식 결과 데이터클래스 + `PlantCatalog`(수동 폴백용)
- `recognition/GeminiPlantIdentifier.kt` — AI Studio REST 엔드포인트(`generativelanguage.googleapis.com`, 모델 `gemini-2.5-flash`) 직접 호출. `HttpURLConnection` + `org.json`만 사용(SDK/Firebase 의존성 없이 최소 구현), `responseMimeType: application/json`으로 구조화 응답 받아 파싱. 실패/식별불가 시 `Result.failure` → 인식 실패로 처리
- API 키: `local.properties`의 `GEMINI_API_KEY`를 `build.gradle.kts`에서 읽어 `BuildConfig.GEMINI_API_KEY`로 노출 (커밋 안 됨, **아직 빈 값 — AI Studio에서 키 발급 후 로컬에 채워넣어야 실제 인식 동작**)
- `ui/recognition/RecognitionScreen.kt` 신규 — 썸네일 + 로딩 인디케이터, `LaunchedEffect`에서 Gemini 호출 후 결과(or null) 콜백
- `util/PhotoUtils.kt` 신규 — 파일 URI → 다운샘플링 디코딩 공용 유틸 (인식/정보확인/홈카드 3곳에서 재사용, `inSampleSize` 계산해서 원본 고화질 그대로 메모리에 올리지 않음)
- 매니페스트에 `INTERNET` 권한 추가

### 6. 캘린더 화면(1-a) + 일정 수정 모드
- `ui/calendar/CalendarScreen.kt` 신규 — `java.util.Calendar` 기반 월간 그리드 직접 구현(라이브러리 없이), 물 준 날짜 원형 표시 + 오늘 날짜 테두리, 이전/다음 달 이동, 하단 "마지막 물주기 N일 전" 요약 + "물 줬어요" 버튼
- 상단바에 "설정"(→ 5번 수정모드, 기존 값 프리필) / "삭제" 버튼
- 홈 카드 클릭 시 캘린더로 진입하도록 연결

### 7. 식물 삭제 + 알림 취소
- `PlantDao.deleteById`, `PlantRepository.deletePlant` 추가 (WateringLog는 FK CASCADE로 같이 정리)
- 캘린더 화면 "삭제" → 확인 다이얼로그 → 삭제 시 `ReminderScheduler.cancel()`도 같이 호출해서 예약된 알림도 취소

### 8. 홈 카드 실제 사진 썸네일
- `HomeScreen`의 `PlantCard`가 `PhotoUtils.decodeBitmap(plant.photoUri)`로 디코딩해서 표시, 없으면 기존 🌱 placeholder 유지

### 9. 전체 내비게이션 재배선
- `MainActivity`: 홈 → 카메라(2) → 인식중(3) → 정보확인(4) → 일정등록(5) → 홈, 그리고 홈 → 캘린더(1-a) → 설정(5, 수정모드)/삭제 흐름까지 전부 연결
- `./gradlew compileDebugKotlin`, `assembleDebug` 둘 다 성공. **실기기 테스트는 아직 전혀 안 함 — 다음에 한 번에 몰아서 확인 예정**

---

---

## 2026-08-11 (계속3) — 알림 디버그 테스트 버튼

### 10. 캘린더 화면에 디버그 전용 "N분 후 알림" 버튼 추가
- WorkManager의 `PeriodicWorkRequest`는 최소 반복 간격이 15분으로 고정되어 있어 실제 "1분/5분마다 반복" 주기는 만들 수 없음
- 대신 `ReminderScheduler.scheduleDebugTest(context, plantId, delayMinutes)` 추가 — 실제 반복 예약과 별개의 워크 이름(`water_reminder_debug_test_{id}`)으로 `OneTimeWorkRequest`를 예약해서 N분 뒤 알림 1회만 발사 (반복 아님, 알림/액션버튼 동작 확인용)
- `CalendarScreen`에 `BuildConfig.DEBUG`일 때만 보이는 "1분 후 알림" / "5분 후 알림" 버튼 추가, `MainActivity`에서 연결
- `./gradlew compileDebugKotlin` 성공 확인. 릴리즈 빌드에는 안 보임(BuildConfig.DEBUG=false)

---

## 2026-08-11 (계속4) — 실기기 테스트 진행 + 갤러리 선택 + Gemini 실제 연동

### 11. 실기기 테스트 1차 결과
- 카메라 촬영, 정보확인, 일정 등록, 수정/삭제, 홈 카드 썸네일까지 정상 동작 확인
- 캘린더/알림은 실제 날짜 변경 없이 확인하기 어려워서 10번(디버그 테스트 버튼)으로 검증 예정

### 12. 카메라 화면에 갤러리에서 사진 선택 기능 추가
- `ui/camera/CameraScreen.kt`에 시스템 Photo Picker(`ActivityResultContracts.PickVisualMedia`) 연동 — 별도 저장소 권한 없이 갤러리 사진 선택 가능
- 고른 사진은 `content://` Uri라 앱 내부 로직(`PhotoUtils` 등)이 기대하는 `file://` 경로로 맞추기 위해 `filesDir/photos/`로 복사 후 사용 (촬영 사진과 동일 경로 규칙)
- 왼쪽 아래에 최근 갤러리 사진을 실제로 불러와 보여주는 64dp 둥근 썸네일 버튼(촬영 버튼 72dp보다 살짝 작게) 추가 — 네이티브 카메라 앱 스타일
  - `READ_MEDIA_IMAGES`(13+) / `READ_EXTERNAL_STORAGE`(그 이하) 권한 요청, 거부해도 선택 기능 자체는 계속 동작(썸네일만 안 보임)

### 13. Gemini 인식 로깅 + 모델명 이슈 해결
- `GeminiPlantIdentifier`에 요청 시작/응답 코드/성공·실패 로그(`Log.d`/`Log.e`) 추가 — `adb logcat -s GeminiPlantIdentifier`로 확인 가능
- **이슈 1 (네트워크)**: 특정 네트워크에서 `generativelanguage.googleapis.com` 연결 자체가 안 돼 `SocketTimeoutException` 발생, 안드로이드 기본 okhttp가 여러 IP를 순차 재시도해서 폴백까지 약 2분 소요됨 → `connectTimeout`/`readTimeout`을 15s/20s → 6s/12s로 낮춰서 폴백까지 걸리는 시간 단축. Wi-Fi로 바꾸니 정상 연결됨
- **이슈 2 (모델 단종)**: `gemini-2.5-flash`가 신규 API 키에는 404("no longer available to new users")로 막혀있음 → `gemini-flash-latest`(항상 최신 flash를 가리키는 별칭)로 교체, curl로 정상 응답 확인 후 적용
- 실기기 재테스트로 이름/학명/물주기 등 실제 Gemini 인식 결과가 정상적으로 채워지는 것 확인 완료

## 다음에 할 것

1. 캘린더/알림 실동작 확인 — 10번 디버그 버튼("1분 후 알림"/"5분 후 알림")으로 알림 수신 + 액션 버튼("물 줬어요"/"나중에") 테스트
2. 갤러리 사진 선택 플로우 실기기 확인 (촬영 대비 새로 추가된 기능이라 아직 안 해봄)
3. (스트레치) TFLite 온디바이스 폴백, 홈 화면 위젯(Glance)
4. UX 다듬기는 실기기 테스트 마무리 후 피드백 보고 결정

## 다음 세션 시작할 때 참고
- 이 파일이랑 `plant_app_navigation.md`를 같이 열어서 이어가면 됨
- 화면/구조가 바뀌면 `plant_app_navigation.md` 쪽을 갱신하고, 이 파일은 "완료 항목 체크 + 다음 우선순위 갱신" 용도로 계속 씀
- **화면 6개 + 캘린더 + 갤러리 선택 + 디버그 알림 테스트 전부 구현/빌드 성공.** Gemini 인식도 실제 키로 정상 동작 확인됨(`gemini-flash-latest` 사용 중)
- 남은 건 캘린더/알림 실동작 확인과 갤러리 선택 플로우 실기기 확인
