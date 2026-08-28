# Gene UX audit

The current app has a strong monochrome visual identity but still asks users to leave the persona context for every question. The ask flow is a bottom drawer, which is better than an alert dialog but still too shallow for a multi-turn conversation. It cannot preserve a conversation, resume one, or reuse a prior answer as context.

The current persistence layer stores people and memories but has no session or message model. Remote requests send the latest 20 memories wholesale rather than selecting context relevant to the user’s question. Back navigation is implemented as a direct callback from the persona page and is not centralized, which makes future multi-page navigation fragile.

The redesign keeps the current feature set and adds only high-leverage structure: a dedicated chat page for each conversation, session cards under a person, persistent user and assistant messages, a single composer with optional quick prompts, a local lexical relevance selector, and centralized back-stack navigation. No new dashboard, social layer, notification system, or extra modes are planned.
