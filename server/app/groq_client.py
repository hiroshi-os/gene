from __future__ import annotations

from typing import Any

import httpx
from fastapi import HTTPException, UploadFile

from app.config import Settings


class GroqClient:
    """Thin async proxy to Groq's OpenAI-compatible API."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._client = httpx.AsyncClient(
            base_url=settings.groq_base_url.rstrip("/"),
            headers={
                "Authorization": f"Bearer {settings.groq_api_key}",
            },
            timeout=settings.request_timeout_seconds,
        )

    async def aclose(self) -> None:
        await self._client.aclose()

    def _ensure_configured(self) -> None:
        if not self._settings.groq_api_key.strip():
            raise HTTPException(
                status_code=503,
                detail="Gene AI is not configured: set GROQ_API_KEY on the server.",
            )

    async def chat_completions(self, payload: dict[str, Any]) -> dict[str, Any]:
        self._ensure_configured()
        body = dict(payload)
        body.setdefault("model", self._settings.groq_chat_model)
        if "max_tokens" in body:
            body["max_tokens"] = min(
                int(body["max_tokens"]),
                self._settings.max_completion_tokens,
            )
        elif "max_completion_tokens" in body:
            body["max_completion_tokens"] = min(
                int(body["max_completion_tokens"]),
                self._settings.max_completion_tokens,
            )
        else:
            body["max_tokens"] = min(220, self._settings.max_completion_tokens)

        response = await self._client.post("/chat/completions", json=body)
        return await self._json_or_raise(response)

    async def transcribe(
        self,
        file: UploadFile,
        model: str | None = None,
        language: str | None = None,
    ) -> dict[str, Any]:
        self._ensure_configured()
        filename = file.filename or "audio.m4a"
        content_type = file.content_type or "application/octet-stream"
        data: dict[str, Any] = {
            "model": (model or self._settings.groq_transcription_model).strip()
            or self._settings.groq_transcription_model,
        }
        if language:
            data["language"] = language

        file_bytes = await file.read()
        if not file_bytes:
            raise HTTPException(status_code=400, detail="Empty audio upload.")

        response = await self._client.post(
            "/audio/transcriptions",
            data=data,
            files={"file": (filename, file_bytes, content_type)},
        )
        return await self._json_or_raise(response)

    @staticmethod
    async def _json_or_raise(response: httpx.Response) -> dict[str, Any]:
        try:
            body = response.json()
        except ValueError:
            body = {"error": {"message": response.text or "Upstream returned non-JSON"}}

        if response.is_success:
            return body if isinstance(body, dict) else {"data": body}

        detail = body
        if isinstance(body, dict):
            err = body.get("error")
            if isinstance(err, dict) and err.get("message"):
                detail = err["message"]
            elif body.get("message"):
                detail = body["message"]

        raise HTTPException(status_code=response.status_code, detail=detail)
