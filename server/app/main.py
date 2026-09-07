from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Any, AsyncIterator

from fastapi import FastAPI, File, Form, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import Settings, get_settings
from app.groq_client import GroqClient


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    settings = get_settings()
    client = GroqClient(settings)
    app.state.settings = settings
    app.state.groq = client
    try:
        yield
    finally:
        await client.aclose()


def create_app() -> FastAPI:
    app = FastAPI(
        title="Gene AI",
        description=(
            "Stateless multi-user gateway for Gene. OpenAI-compatible chat and "
            "transcription endpoints, backed by Groq. No auth or database yet."
        ),
        version="0.1.0",
        lifespan=lifespan,
    )

    # Mobile clients hit this from many devices; keep CORS open while auth is absent.
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=False,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.get("/health")
    async def health(request: Request) -> dict[str, Any]:
        settings: Settings = request.app.state.settings
        return {
            "status": "ok",
            "service": "gene-ai",
            "configured": bool(settings.groq_api_key.strip()),
            "chat_model": settings.groq_chat_model,
            "transcription_model": settings.groq_transcription_model,
        }

    @app.post("/v1/chat/completions")
    async def chat_completions(request: Request) -> JSONResponse:
        payload = await request.json()
        if not isinstance(payload, dict):
            return JSONResponse(
                status_code=400,
                content={"error": {"message": "Request body must be a JSON object."}},
            )
        if "messages" not in payload:
            return JSONResponse(
                status_code=400,
                content={"error": {"message": "messages is required."}},
            )
        client: GroqClient = request.app.state.groq
        result = await client.chat_completions(payload)
        return JSONResponse(content=result)

    @app.post("/v1/audio/transcriptions")
    async def audio_transcriptions(
        request: Request,
        file: UploadFile = File(...),
        model: str | None = Form(default=None),
        language: str | None = Form(default=None),
    ) -> JSONResponse:
        client: GroqClient = request.app.state.groq
        result = await client.transcribe(file=file, model=model, language=language)
        return JSONResponse(content=result)

    return app


app = create_app()
