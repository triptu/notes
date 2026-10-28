# Maintainer guide

Notes for Android is a native Android notes app (Kotlin, Compose) that edits an Obsidian vault and syncs it through a private GitHub repository. It reuses the sync engine of the Obsidian plugin Gitless Vault Sync, so the phone and the plugin commit the same way. This repository, `triptu/notes`, is public. Personal details for the maintainer's own setup (vault names, paths, devices) live in `CLAUDE.local.md`, which git ignores; keep them out of tracked files.

## Layout

- `android/`: the Gradle project. `app/` is the app, `sync/` wraps the engine for Kotlin.
- `vault-sync/`: a git submodule of [triptu/markdown-github-sync](https://github.com/triptu/markdown-github-sync), the plugin. The app uses only its engine, `plugin/src/git-sync` (and the access wording in `plugin/src/ui/copy.ts`). `android/sync/js/entry.ts` imports it, and `android/sync/js/build.mjs` bundles it with esbuild into one script for QuickJS during the Gradle build. Never copy engine code into this repository; change it in the plugin's repository.
- `site/`: the static page for notes.tushar.ai. It's the Vercel project `notes` and deploys on push to `main` when `site/` changes. Screens and notes there are made up.
- `docs/images/`: screenshots for the README, taken from the debug build on an emulator with the sample vault.

## Build and test

```sh
git submodule update --init
npm ci --prefix vault-sync
cd android && ./gradlew :app:testDebugUnitTest installDebug
```

Node must be on `PATH` (or pass `-Pnode=/path/to/node`). Set `ANDROID_SERIAL` when more than one device is attached.

The release app is `io.github.triptu.notes`, built with `./gradlew :app:assembleRelease` and signed with a key configured in `~/.gradle/gradle.properties`. The debug app is `io.github.triptu.notes.debug`.

### Bumping the engine

The submodule is pinned to one commit of the plugin's `main`. To take engine changes:

```sh
git -C vault-sync fetch && git -C vault-sync checkout origin/main
npm ci --prefix vault-sync
cd android && ./gradlew :app:testDebugUnitTest :sync:testDebugUnitTest :app:assembleRelease
```

Then commit the new `vault-sync` pointer.

## Rules

- Test the app on an emulator, not the owner's phone. The phone carries only the release app, which holds the owner's real vault. Don't install the debug app or anything else on the phone, and never open, tap, clear or reconfigure the release app; updating it with `adb install -r` is fine when asked. The phone can show up twice in `adb devices` (by IP and by name); pick one with `ANDROID_SERIAL`.
- Never connect the debug app to a real vault.
- Before any `adb shell input` on an emulator, check `mCurrentFocus` is the debug app.
- Never put the owner's vault name, paths or note content into tracked files.

## App notes

- `vault/FileVaultStore.kt` writes through the sync module's `VaultFiles`, the one writer of the vault folder, so sync's compare-and-swap and the editor share a lock. Never write the folder directly.
- `syncing/` connects the app to `android/sync`: `GitHubSync` owns the client, setup, the encrypted token and conflict settling (both versions kept inline between Git conflict markers); `SendEditsWorker` sends edits after the app leaves the screen. Sync copy lives in `syncing/SyncCopy.kt`. `adb logcat -s NotesSync` shows status changes; debug builds also log engine calls and request timings (never paths or note text).
- Debug builds have "Load sample vault" in settings while sync isn't set up; setting up sync deletes it first. The sample notes are made up; never copy real vault content into the repo.
- Icons are Phosphor SVGs imported with `android/tools/import-phosphor-icons.sh`.
- `voice/` is the speaking view. `LiveSpeech` runs Android's recognizer (on-device first); on Android 13+ `MicCapture` owns the mic and feeds it through a pipe as one segmented session, and keeps a WAV for the optional OpenAI refine (`refine/`). `Transcript` stitches results across sessions; drafts live in `files/voice-drafts`, outside the vault. Copy lives in `voice/VoiceCopy.kt`. `adb logcat -s NotesVoice` shows sessions in debug builds.
- To test speech without talking, put 16 kHz mono 16-bit PCM at the debug app's `cache/voice-test.pcm` (`run-as io.github.triptu.notes.debug`); the debug app plays it instead of the mic. Delete it afterwards.
- `am start ... --es theme light|dark` forces a theme in debug builds, for screenshots.
