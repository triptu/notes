# Notes for Android

A calm Android app for writing in your Obsidian vault. Notes save on the phone first, then sync as commits to your own private GitHub repository, with no server in between.

[notes.tushar.ai](https://notes.tushar.ai/) · Pairs with the Obsidian plugin [Gitless Vault Sync](https://community.obsidian.md/plugins/github-vault-sync) ([vaultsync.tushar.ai](https://vaultsync.tushar.ai/))

<p>
  <img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/home.png" width="260" alt="Home: pinned notes, one old note to revisit, and a New note bar with a mic" />
  <img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/zen.png" width="260" alt="Editing a note: the header steps away while you type" />
  <img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/vault.png" width="260" alt="The vault's folders and notes, with search" />
</p>
<p>
  <img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/voice.png" width="260" alt="Speaking a note: the page fills in as you talk" />
  <img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/widgets.jpg" width="260" alt="Home screen widgets: a pinned note with tasks you can tick, and a New note bar" />
</p>
<p>
  <img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/unfolded.png" width="790" alt="Unfolded on a foldable: the list on the left, the open note on the right, in dark mode" />
</p>

- Markdown renders in place; the symbols show only on the line you're editing.
- Speak a note with the phone's own speech recognition, on-device first.
- Pinned notes, This week's journal note, templates, `[[links]]`, search and home screen widgets.
- Syncs with the same engine as the Obsidian plugin, so the phone and your computer share one repository.

## Build and install

You need Android Studio's SDK (or `ANDROID_HOME`), JDK 17 and Node.js.

```sh
git clone --recursive https://github.com/triptu/notes.git
cd notes
npm ci --prefix vault-sync
cd android
./gradlew installDebug
```

`installDebug` puts the debug build on the connected phone or emulator. For a release build, add your own signing key to `~/.gradle/gradle.properties` (`NOTES_RELEASE_STORE_FILE`, `NOTES_RELEASE_STORE_PASSWORD`, `NOTES_RELEASE_KEY_ALIAS`) and run `./gradlew :app:assembleRelease`.

In the app, connect a private GitHub repository with a fine-grained token that can read and write its contents. Install the plugin in Obsidian on your computer and point it at the same repository.

`vault-sync/` is the plugin's repository as a git submodule; the app bundles its sync engine at build time. [AGENTS.md](AGENTS.md) covers the layout, tests and how to update the engine.
