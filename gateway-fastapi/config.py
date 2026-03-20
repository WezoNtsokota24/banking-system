from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    app_name: str = "Banking Gateway API"
    environment: str = "development"
    spring_backend_url: str = "http://localhost:8080"

    class Config:
        env_file = ".env"

settings = Settings()
