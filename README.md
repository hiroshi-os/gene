# Gene

Gene is a Kotlin Android Jetpack Compose app for building private, evolving context about people through notes and speech-to-text memories.

## Included

The app includes a monochrome interface with a pure-white and pure-black theme toggle synchronized with the Android system bars, a people list, person profiles, drawer-based flows instead of popup forms, a unified memory composer, quick text memories, speech recognition memories, quote capture, signal capture with an optional interpretation field, pattern capture, recommendation, favorite, feeling, and open-question memory modes, ten-memory persona summaries with manual and checkpoint refresh, a two-row bento quick-action grid, an automatic three-quote carousel, persistent chat sessions, a dedicated conversational page, fresh-chat-only temporary mode, reusable earlier-session context, concise in-character replies, voice input, per-reply confidence and expandable memory references, session favorite/rename/delete actions, local memory and chat search, deletion of a person and all related memories, an optional floating capture bubble, and a custom Gene launcher icon.

The default intelligence engine works offline. It uses the saved memories to produce concise conversational replies and avoids claiming access to another person's hidden thoughts. Chats are saved by default. Temporary mode is available only on a fresh chat before its first message is saved. Saved sessions can be favorited, renamed, deleted, and reopened. New saved sessions can reuse a small bounded slice of earlier session context. Before a remote request, Gene runs a fully local feature-hashed vector search over memories and sends only the most relevant memories instead of the entire person history. The selected memory IDs are stored with each assistant reply so the user can expand its references. An optional OpenAI-compatible endpoint can be configured in Settings. When configured, the selected context is sent to that endpoint for an answer. The persona page keeps search in the top bar, uses the bottom composer for memory capture, and opens an unsaved fresh chat only from the adjacent chat action. The app does not continuously record audio; it uses Android speech recognition for both Voice memory and chat voice input.

## Project structure

- `app/src/main/java/com/gene/app/MainActivity.kt`: Compose screens and interaction flows.
- `app/src/main/java/com/gene/app/data/GeneDatabase.kt`: local people, memories, sessions, messages, summaries, and session actions database.
- `app/src/main/java/com/gene/app/data/LocalRelevanceSearch.kt`: fully on-device feature-hashed vector retrieval and confidence scoring.
- `app/src/main/java/com/gene/app/data/InsightEngine.kt`: offline persona synthesis and evidence-aware answers.
- `app/src/main/java/com/gene/app/data/RemoteInsightEngine.kt`: optional OpenAI-compatible chat completion adapter.
- `app/src/main/java/com/gene/app/service/FloatingCaptureService.kt`: movable overlay bubble with long-press menu, persona selection, quick open, and close behavior.

## Build

Use Android Studio with JDK 17 or newer, then open this directory and run the `app` configuration.

For a command-line debug build:

```bash
./gradlew assembleDebug
```

The generated APK is under `app/build/outputs/apk/debug/app-debug.apk`.

## Remote intelligence

In Settings, enter an OpenAI-compatible base URL or full chat-completions URL, a model identifier, and an API key. Leave the fields blank to keep all reasoning local. The optional floating bubble can be dragged to a preferred position. Tap it to open Gene; long-press it to choose a persona, open Gene, or close the bubble. The selected persona and bubble position are kept locally. Remote answers should be treated as suggestions and verified through direct communication, especially for relationship decisions.

## Privacy boundaries

Gene is designed for user-entered notes and user-initiated transcripts. Users should obtain appropriate consent before recording or transcribing another person. The app should not be used for covert surveillance, impersonation, diagnosis, or making high-stakes decisions about another person.
