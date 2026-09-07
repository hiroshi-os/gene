"""Run Gene AI locally.

  cd server
  python -m venv .venv
  .venv\\Scripts\\activate   # Windows
  pip install -r requirements.txt
  copy .env.example .env    # then set GROQ_API_KEY
  uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
"""

from app.config import get_settings


def main() -> None:
    import uvicorn

    settings = get_settings()
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=True,
    )


if __name__ == "__main__":
    main()
