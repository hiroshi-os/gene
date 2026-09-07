# Gene AI server

Stateless FastAPI gateway that fronts Groq for the Gene Android app. Designed for many concurrent clients: no per-user state, no database, and (for now) no auth.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/health` | Liveness + whether `GROQ_API_KEY` is set |
| `POST` | `/v1/chat/completions` | OpenAI-compatible chat completions → Groq |
| `POST` | `/v1/audio/transcriptions` | Multipart audio transcription → Groq Whisper |

Clients may send an `Authorization` header; it is ignored. The Groq key stays on the server.

## Setup

```bash
cd server
python -m venv .venv
# Windows
.venv\Scripts\activate
# macOS / Linux
source .venv/bin/activate

pip install -r requirements.txt
cp .env.example .env   # set GROQ_API_KEY
```

## Run

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Or:

```bash
python run.py
```

Android emulator → host machine: use `http://10.0.2.2:8000/v1` as the Gene AI endpoint in Settings.

Physical device on the same LAN: use `http://<your-lan-ip>:8000/v1`.

## Environment

| Variable | Default | Notes |
|----------|---------|-------|
| `GROQ_API_KEY` | _(required)_ | Groq API key |
| `GROQ_CHAT_MODEL` | `llama-3.3-70b-versatile` | Default when the client omits `model` |
| `GROQ_TRANSCRIPTION_MODEL` | `whisper-large-v3` | Default Whisper model |
| `GROQ_BASE_URL` | `https://api.groq.com/openai/v1` | Override only if needed |
| `HOST` / `PORT` | `0.0.0.0` / `8000` | Bind address |
| `MAX_COMPLETION_TOKENS` | `1024` | Soft cap per chat request |
| `REQUEST_TIMEOUT_SECONDS` | `60` | Upstream timeout |

## Multi-user notes

- Requests are independent; nothing is stored.
- CORS is open (`*`) while the service is unauthenticated.
- Cap tokens and timeouts so one client cannot stall a worker indefinitely.
- Add auth, rate limits, and abuse controls before a public deployment.
