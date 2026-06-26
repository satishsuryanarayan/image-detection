# Image Object Detection API

Spring Boot REST API for ingesting image URLs, optionally detecting objects, persisting metadata in MySQL, and searching images by detected objects.

## Tech Stack
- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA / Hibernate
- MySQL 9.7.1 (Docker image)
- Maven
- Docker / Docker Compose

## Features
- `POST /images` to save an image URL with an optional label
- automatic label generation when label is omitted
- optional object detection using a pluggable detection service
- `GET /images` to list all saved images with pagination
- `GET /images/{imageId}` to fetch one image
- `GET /images?objects=dog,car` to filter by detected object names with pagination
- centralized error handling with appropriate HTTP status codes
- integration tests using Spring Boot Test and MockMvc

## Object Detection Strategy
The application supports three detector implementations selected with `DETECTOR_PROVIDER`:

1. **Mock detection** with `DETECTOR_PROVIDER=mock` for local demos and tests
2. **Imagga-backed detection** with `DETECTOR_PROVIDER=imagga`
3. **Clarifai-backed detection** with `DETECTOR_PROVIDER=clarifai`

The mock detector derives object names from URL keywords such as `cat`, `dog`, `car`, and `person`, which keeps the app self-contained for tests and local demos.

## API Examples

### Create image
`POST /images`

Request:
```json
{
  "imageUrl": "https://example.com/dog-park.jpg",
  "label": "Park photo",
  "enableDetection": true
}
```

Response:
```json
{
  "id": 1,
  "imageUrl": "https://example.com/dog-park.jpg",
  "label": "Park photo",
  "detectionEnabled": true,
  "createdAt": "2026-06-23T14:00:00Z",
  "detectedObjects": ["dog"]
}
```

### List images
`GET /images?page=0&size=20`

Response:
```json
{
  "content": [
    {
      "id": 1,
      "imageUrl": "https://example.com/dog-park.jpg",
      "label": "Park photo",
      "detectionEnabled": true,
      "createdAt": "2026-06-23T14:00:00Z",
      "detectedObjects": ["dog"]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### Search by objects
`GET /images?objects=dog,car&page=0&size=20`

Search uses **match-any semantics**: an image is returned if it contains at least one of the requested object names.
Pagination is zero-based and defaults to `page=0`, `size=20`, sorted by `createdAt` descending.

### Get image by id
`GET /images/{imageId}`

## Run Locally with Maven

### 1. Start MySQL
You can run the database using Docker (the project Dockerfile currently targets `mysql:9.7.1`):
```bash
docker build -t image-detection-mysql ./mysql
docker run --name image-detection-mysql \
  -e MYSQL_DATABASE=image_detection \
  -e MYSQL_USER=image_user \
  -e MYSQL_PASSWORD=image_password \
  -e MYSQL_ROOT_PASSWORD=root_password \
  -p 3306:3306 \
  image-detection-mysql
```

### 2. Set environment variables
Set these in the same terminal before starting the application:
```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/image_detection
export SPRING_DATASOURCE_USERNAME=image_user
export SPRING_DATASOURCE_PASSWORD=image_password
export DETECTOR_PROVIDER=mock

# Optional: Imagga detector
export IMAGGA_API_KEY=your_imagga_api_key
export IMAGGA_API_SECRET=your_imagga_api_secret
export IMAGGA_THRESHOLD=70
export IMAGGA_LIMIT=-1
export IMAGGA_CONNECTION_TIMEOUT_SECONDS=1
export IMAGGA_REQUEST_TIMEOUT_SECONDS=2

# Optional: Clarifai detector
export CLARIFAI_PAT=your_clarifai_pat
export CLARIFAI_USER_ID=clarifai
export CLARIFAI_APP_ID=main
export CLARIFAI_MODEL_ID=general-image-recognition
export CLARIFAI_MIN_CONFIDENCE=0.70
export CLARIFAI_CONNECTION_TIMEOUT_SECONDS=1
export CLARIFAI_REQUEST_TIMEOUT_SECONDS=5
```

`DETECTOR_PROVIDER` defaults to `mock`. Set it to `imagga` or `clarifai` to use an external detector.

> Note: keep real API keys, PATs, and secrets in environment variables or a local `.env`/run configuration. Do not commit them to source control.

### 3. Run the Spring Boot app
```bash
mvn spring-boot:run
```

## Run with Docker Compose
Docker Compose builds and starts both services:
- API: `http://localhost:8080`
- MySQL: `localhost:3306`

The compose file expects local secret files for Imagga and Clarifai. Leave provider-specific files empty when not using that provider.

### Option A: use mock detection
```bash
mkdir -p secrets
touch secrets/imagga.api-key secrets/imagga.api-secret
touch secrets/clarifai.pat secrets/clarifai.user-id secrets/clarifai.app-id secrets/clarifai.model-id
DETECTOR_PROVIDER=mock docker compose up --build
```

### Option B: use Clarifai detection
```bash
mkdir -p secrets
touch secrets/imagga.api-key secrets/imagga.api-secret
printf 'your_clarifai_pat' > secrets/clarifai.pat
printf 'clarifai' > secrets/clarifai.user-id
printf 'main' > secrets/clarifai.app-id
printf 'general-image-recognition' > secrets/clarifai.model-id
DETECTOR_PROVIDER=clarifai docker compose up --build
```

The example model path `clarifai/main/general-image-recognition` is intended for Clarifai's general image recognition model. If your Clarifai account or model page shows different user, app, or model IDs, use those values instead.

### Option C: use Imagga detection
```bash
mkdir -p secrets
printf 'your_imagga_api_key' > secrets/imagga.api-key
printf 'your_imagga_api_secret' > secrets/imagga.api-secret
touch secrets/clarifai.pat secrets/clarifai.user-id secrets/clarifai.app-id secrets/clarifai.model-id
DETECTOR_PROVIDER=imagga docker compose up --build
```

Spring Boot imports `/run/secrets/` as a config tree, so the mounted files are read as properties such as `imagga.api-key`, `imagga.api-secret`, `clarifai.pat`, `clarifai.user-id`, `clarifai.app-id`, and `clarifai.model-id`.

> Important: `secrets/` is ignored by this repository's `.gitignore`. Keep real credentials out of source control.

## Detector Configuration

### Provider Selection
| Environment variable | Spring property | Default | Description |
| --- | --- | ---: | --- |
| `DETECTOR_PROVIDER` | `detector.provider` | `mock` | Detector implementation to use: `mock`, `imagga`, or `clarifai`. |

### Clarifai Configuration
When `DETECTOR_PROVIDER=clarifai`, these settings control Clarifai model calls:

| Environment variable | Spring property / Docker secret | Default | Description |
| --- | --- | ---: | --- |
| `CLARIFAI_PAT` | `clarifai.pat` | empty | Clarifai personal access token. Required for Clarifai mode. |
| `CLARIFAI_USER_ID` | `clarifai.user-id` | `clarifai` | Clarifai user/owner id for the selected model. |
| `CLARIFAI_APP_ID` | `clarifai.app-id` | `main` | Clarifai app id for the selected model. |
| `CLARIFAI_MODEL_ID` | `clarifai.model-id` | `general-image-recognition` | Clarifai model id. |
| `CLARIFAI_MIN_CONFIDENCE` | `clarifai.min-confidence` | `0.70` | Minimum concept confidence persisted as a detected object. |
| `CLARIFAI_CONNECTION_TIMEOUT_SECONDS` | `clarifai.connection-timeout-seconds` | `1` | Timeout for establishing the HTTP connection to Clarifai. |
| `CLARIFAI_REQUEST_TIMEOUT_SECONDS` | `clarifai.request-timeout-seconds` | `5` | Timeout for the full Clarifai request/response wait. |

### Imagga Configuration
When Imagga credentials are configured, these optional settings control tagging behavior and upstream timeouts:

| Environment variable | Spring property | Default | Description |
| --- | --- | ---: | --- |
| `IMAGGA_THRESHOLD` | `imagga.threshold` | `70` | Minimum confidence threshold sent to Imagga. |
| `IMAGGA_LIMIT` | `imagga.limit` | `-1` | Maximum number of tags requested from Imagga; `-1` uses Imagga's default behavior. |
| `IMAGGA_CONNECTION_TIMEOUT_SECONDS` | `imagga.connection-timeout-seconds` | `1` | Timeout for establishing the HTTP connection to Imagga. |
| `IMAGGA_REQUEST_TIMEOUT_SECONDS` | `imagga.request-timeout-seconds` | `2` | Timeout for the full Imagga request/response wait. |

### External Detector Upstream Error Handling
When object detection is backed by Imagga or Clarifai, upstream failures are mapped to API responses as follows:

| Upstream condition | API status |
| --- | ---: |
| External detector returns a `4xx` client error | `400 Bad Request` |
| External detector returns a `5xx` server error | `502 Bad Gateway` |
| External detector returns malformed or corrupted response data | `502 Bad Gateway` |
| External detector request times out before a response is received | `504 Gateway Timeout` |
| External detector is unavailable or the connection cannot be established | `503 Service Unavailable` |
| Any remaining unexpected application failure | `500 Internal Server Error` |

## Build Docker Images Manually

Application image:
```bash
docker build -t image-detection-app .
```

MySQL image:
```bash
docker build -t image-detection-mysql ./mysql
```

## Run Tests
```bash
mvn test
```

## Swagger UI / OpenAPI
After starting the application, interactive API documentation is available at:

```text
http://localhost:8080/swagger-ui.html
```

If your local setup redirects differently, use:

```text
http://localhost:8080/swagger-ui/index.html
```

The raw OpenAPI document is available at:

```text
http://localhost:8080/v3/api-docs
```

## Project Structure
```text
src/main/java/com/ss/imagedetection
├── config
├── controller
├── detector
├── dto
├── entity
├── exception
├── repository
└── service
```
