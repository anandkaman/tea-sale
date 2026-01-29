# 📱 Mobile Development Best Practices: Java, Kotlin & Flutter

This guide outlines essential precautions, tools, and strategies to ensure easy development and successful builds for your mobile projects.

---

## 🏗️ 1. The Foundation: Environment & Tools

### Android SDK & JDK
- **Consistency is King**: Always ensure your `JAVA_HOME` matches the version required by your Gradle version. Use **JDK 17** for modern Android development (AGP 8.0+).
- **SDK Platforms**: Install only the SDK platforms you need. Keep the **Build Tools** updated via the Android Studio SDK Manager.
- **Environment Variables**: Add `ANDROID_HOME` and `platform-tools` to your system PATH so you can use `adb` and `fastlane` from any terminal.

### The Build System (Gradle / Pubspec)
- **Gradle Wrapper**: Always use `./gradlew` instead of a global `gradle` command. This ensures everyone on the team uses the exact same build version.
- **Dependency Management**: 
    - **Android**: Use `libs.versions.toml` (Version Catalog) to centralize versions.
    - **Flutter**: Use `^` carefully; pinning versions (`1.2.3` instead of `^1.2.3`) prevents unexpected build breaks when libraries update.

---

## 🧩 2. Modularity & Architecture

### High-Level Modularity
- **Separate Concerns**: Don't put everything in one "app" module. 
    - `data`: For API calls, Database logic, and Models.
    - `ui`: For Fragments, Activities, or Widgets.
    - `domain`: For business logic and use cases.
- **Why?**: It makes testing easier, builds faster (only modified modules recompile), and prevents circular dependencies.

### App Info & Configuration
- **Package Names**: Choose a unique, reverse-domain name (e.g., `com.goldtea.sales`). Once published, changing this is extremely difficult.
- **Build Types**: Use `debug` and `release` flavors. Never hardcode API keys in code; use `local.properties` or `.env` files.

---

## 💾 3. Database & Data Strategy

### Local vs. Remote
- **Local Persistence**: Use **Room** (Android) or **Isar/Sqflite** (Flutter) for structured data. Use **DataStore/SharedPreferences** for small settings.
- **Remote (Cloud)**: 
    - **Firestore**: Excellent for real-time sync and offline support.
    - **SQL/NoSQL**: Choose based on whether your data is relational (Reports) or document-based (Logs).
- **Proactive Offline Support**: Always initialize your DB engine immediately. Implement **Optimistic UI** (update the screen first, sync in the background) to prevent the app from feeling "stiff" on slow internet.

---

## 🛡️ 4. Essential Precautions for Success

### Build Predictability
1. **Clean often**: If the UI isn't reflecting changes, run `./gradlew clean`.
2. **Deterministic IDs**: When using Firestore or MongoDB, generate IDs on the client side (UUID) to avoid duplicate records during offline sync.
3. **ProGuard / R8**: Always test your `release` build early. Logic that works in `debug` might break when code is shrank/obfuscated.

### UI & UX Safety
- **Fragment Lifecycle**: Always check `isAdded()` or `viewLifecycleOwner` before updating UI from a background thread to avoid `IllegalStateException`.
- **Thread Safety**: Never do DB/Network work on the Main Thread. Use **Coroutines** (Kotlin), **Executors/Tasks** (Java), or **Async/Await** (Flutter).

### Git Hygiene
- **.gitignore is Mandatory**: Never push `google-services.json`, `.idea`, `.env`, or `local.properties`.
- **Atomic Commits**: Commit one feature or one fix at a time. It makes "rolling back" a bad build much easier.

---

## 🛠️ Recommended Toolset
| Category | Recommended Tools |
| :--- | :--- |
| **IDE** | Android Studio, VS Code (for Flutter) |
| **Debug** | ADB (Android Debug Bridge), Logcat, DevTools |
| **Network** | Postman / Hoppscotch (for API testing) |
| **Backend** | Firebase (Auth/Firestore/Analytics) |
| **Version Control** | Git + GitHub/GitLab |

---

> [!TIP]
> **Build Success Secret**: Before adding a new library, check its "Method Count" and "Impact on Build Time". Bloated apps start slow and crash more often.
