# Світло?

- minSdk 7 (Android 2.1), targetSdk/compileSdk 35, без бібліотек.
- Сборка: Android Studio (Ladybug+) → Open, або `gradle wrapper && ./gradlew assembleRelease` (JDK 17).
- 
- Сервер: `http://uae1.alwaysdata.net/` (cleartext дозволений в маніфесті).

## Структура
- `MainActivity` + `Screen` — навігація в одній Activity.
- `MainScreen`, `LocationScreen`, `RegionScreen`, `GroupScreen`, `NameScreen`, `SettingsScreen` — шість екранів макету.
- `Ui`, `IconView`, `SvgPath`, `Icons` — верстка за координатами макету та іконки з SVG-шляхів.
- `Repo`, `Schedule`, `Api`, `Groups`, `Store`, `Region`, `S` — данні, читання TSV, мережа, строки uk/en.
- `Scheduler`, `Updater`, `AlarmReceiver`, `BootReceiver`, `UpdateService`, `Compat` — фон, сповіщення, сумісність версій.

## Звуки сповіщень 
Свох звуки сповіщень в `app/src/main/res/raw/`:
- `snd_neutral` — зміна графіку;
- `snd_happy` — «Світло увімкнуть через…»;
- `snd_sad` — «Світло вимкнуть через…».

Android 8+: три канали `change_v2` / `on_v2` / `off_v2`, старі `alerts` / `changes` видаляються.
Android 3.0–7.1: `setSound()`, 2.x: `Notification.sound`. Якщо звук сповіщення не вдалося підставити– буде системний.
`res/raw/keep.xml` допомагає стабільніше підставити звук не обрізаючись

Групи регіона берутся одним запросом `GET /<регион>/` ; або підбір регіонів, якщо не вдалося підключитись.
