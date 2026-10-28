# Notes for Android

A calm Android app for writing in your Obsidian vault. Notes save on the phone first, then sync to your own private GitHub repository.

[notes.tushar.ai](https://notes.tushar.ai/) · Pairs with the Obsidian plugin [Gitless Vault Sync](https://github.com/triptu/markdown-github-sync) ([vaultsync.tushar.ai](https://vaultsync.tushar.ai/)), so your phone and your computer share one repository.

[![Poster for the film: a phone taking a spoken note. Click to watch it on notes.tushar.ai.](https://raw.githubusercontent.com/triptu/notes/main/site/poster.jpg)](https://notes.tushar.ai/)

## Writing

<img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/zen.png" width="260" alt="Editing a note: headings, links and lists render in place while the keyboard is open" />

Headings, links, tasks and images render in place. The symbols behind them show only on the line you're editing. Type `[[` and the app suggests notes to link.

## Home

<img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/home.png" width="260" alt="Home: four pinned notes and a pinned folder, one old note to revisit, and a New note bar with a mic" />

Pin notes and folders from your vault. This week opens the week's note and starts it from your template when it doesn't exist yet. One old note appears at the bottom each day, in case you want to read it again.

## Speaking a note

<img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/voice.png" width="260" alt="Speaking a note: the page fills in as you talk, with Pause, Save and Type below" />

Tap the mic and the words appear as you talk, through the phone's own speech recognition. Add an OpenAI key in settings if you want a cleaner transcript after you save. Without a key, the app never talks to OpenAI.

## Widgets and sharing

<img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/widgets.jpg" width="260" alt="Home screen widgets: a pinned Todos note with tasks you can tick, and a New note bar" />

A pinned note sits on the home screen with tasks you can tick. A New note bar writes or records in one tap. Text shared from any app goes into a new note or onto the end of one you pick.

## The vault

<img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/vault.png" width="260" alt="The vault's folders and notes, with a search field at the top" />

Every folder and note from your vault, with search. New notes are named by the time and their first words, so they sort by date.

## Wide screens

<img src="https://raw.githubusercontent.com/triptu/notes/main/docs/images/unfolded.png" width="790" alt="Unfolded on a foldable in dark mode: the pinned notes on the left, the open note on the right" />

The app follows the phone's theme. Unfolded, or on a tablet, the note opens beside the page you came from. A button puts that page away so the note has the whole screen.

## Sync

Edits save on the phone and sync to your private GitHub repository as commits. Leaving the app sends what you just wrote. Obsidian on your computer syncs with the same repository through the plugin. A note changed in both places keeps both edits in the note.

## Getting started

There is no store listing, so a coding agent builds the app for you. It needs Android 12 or newer. Turn on USB debugging on the phone, plug it into your computer, and give the agent this:

```
Build https://github.com/triptu/notes and install it on my Android phone. Follow the build steps in AGENTS.md in that repo.
```

Then, in the app's settings, connect a private GitHub repository. Create token opens GitHub with the permissions filled in. On your computer, install [Gitless Vault Sync](https://community.obsidian.md/plugins/github-vault-sync) in Obsidian and point it at the same repository. To update the app later, ask the agent to pull and build again.
