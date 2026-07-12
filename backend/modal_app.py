"""Modal deployment for the Kids Learning backend.

Deploy (from the backend/ directory):

    pip install modal
    modal setup                                   # one-time sign-in, free tier
    modal secret create kids-learning \
        ANTHROPIC_API_KEY=sk-ant-... \
        APP_SHARED_TOKEN=<any-long-random-string>
    modal deploy modal_app.py

The deploy prints a public HTTPS URL. Enter that URL and your APP_SHARED_TOKEN in
the app's Parent mode -> Backend connection, and lesson generation goes live.

The AI key lives only in the Modal secret -- never in the APK or this repo. The
service scales to zero when idle, so family-scale usage fits in the free tier.
"""

import modal

app = modal.App("kids-learning-backend")

image = (
    modal.Image.debian_slim(python_version="3.11")
    .pip_install_from_requirements("requirements.txt")
    # The FastAPI app package and the shared lesson schema. schema.py resolves the
    # repo root three levels above itself, so the schema mounts at /lesson-schema.
    .add_local_dir("app", remote_path="/root/app")
    .add_local_dir("../lesson-schema", remote_path="/lesson-schema")
)


@app.function(
    image=image,
    secrets=[modal.Secret.from_name("kids-learning")],
    timeout=300,  # generation can take a couple of minutes for long source texts
)
@modal.asgi_app()
def fastapi_app():
    from app.main import app as api

    return api
