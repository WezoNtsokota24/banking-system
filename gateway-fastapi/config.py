from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    app_name: str = "Banking Gateway API"
    environment: str = "development"
    spring_backend_url: str = "http://spring-backend:8080"  # Default for Docker container

    class Config:
        env_file = ".env"

settings = Settings()
