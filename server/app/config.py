from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    groq_api_key: str = ""
    groq_base_url: str = "https://api.groq.com/openai/v1"
    groq_chat_model: str = "llama-3.3-70b-versatile"
    groq_transcription_model: str = "whisper-large-v3"
    host: str = "0.0.0.0"
    port: int = 8000
    # Soft per-request caps so one noisy client cannot monopolize a worker.
    max_completion_tokens: int = 1024
    request_timeout_seconds: float = 60.0


@lru_cache
def get_settings() -> Settings:
    return Settings()
