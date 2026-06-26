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
The application supports two detector implementations:

1. **Imagga-backed detection** when `IMAGGA_API_KEY` and `IMAGGA_API_SECRET` are configured
2. **A default mock detector** when credentials are not configured

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
export IMAGGA_API_KEY=your_imagga_api_key
export IMAGGA_API_SECRET=your_imagga_api_secret
export IMAGGA_THRESHOLD=70
export IMAGGA_LIMIT=-1
export IMAGGA_CONNECTION_TIMEOUT_SECONDS=1
export IMAGGA_REQUEST_TIMEOUT_SECONDS=2
```

If the Imagga credentials are omitted, the application automatically falls back to the built-in mock detector.

> Note: keep your real API key and secret in environment variables or a local `.env`/run configuration. Do not commit them to source control.

### 3. Run the Spring Boot app
```bash
mvn spring-boot:run
```

## Run with Docker Compose
Docker Compose builds and starts both services:
- API: `http://localhost:8080`
- MySQL: `localhost:3306`

The compose file expects two local secret files for Imagga credentials. Leave them empty to use the mock detector, or put real credentials in them to use Imagga.

### Option A: use mock detection
```bash
mkdir -p secrets
touch secrets/imagga.api-key secrets/imagga.api-secret
docker compose up --build
```

### Option B: use Imagga detection
```bash
mkdir -p secrets
printf 'your_imagga_api_key' > secrets/imagga.api-key
printf 'your_imagga_api_secret' > secrets/imagga.api-secret
docker compose up --build
```

Spring Boot imports `/run/secrets/` as a config tree, so the mounted files are read as `imagga.api-key` and `imagga.api-secret`.

> Important: `secrets/` is ignored by this repository's `.gitignore`. Keep real credentials out of source control.

## Build Docker Images Manually

Application image:
```bash
docker build -t image-detection-app .
```

MySQL image:
```bash
docker build -t image-detection-mysql ./mysql
```

## Imagga Configuration
When Imagga credentials are configured, these optional settings control tagging behavior and upstream timeouts:

| Environment variable | Spring property | Default | Description |
| --- | --- | ---: | --- |
| `IMAGGA_THRESHOLD` | `imagga.threshold` | `70` | Minimum confidence threshold sent to Imagga. |
| `IMAGGA_LIMIT` | `imagga.limit` | `-1` | Maximum number of tags requested from Imagga; `-1` uses Imagga's default behavior. |
| `IMAGGA_CONNECTION_TIMEOUT_SECONDS` | `imagga.connection-timeout-seconds` | `1` | Timeout for establishing the HTTP connection to Imagga. |
| `IMAGGA_REQUEST_TIMEOUT_SECONDS` | `imagga.request-timeout-seconds` | `2` | Timeout for the full Imagga request/response wait. |

## Imagga Upstream Error Handling
When object detection is backed by Imagga, upstream failures are mapped to API responses as follows:

| Imagga/upstream condition | API status |
| --- | ---: |
| Imagga returns a `4xx` client error | `400 Bad Request` |
| Imagga returns a `5xx` server error | `502 Bad Gateway` |
| Imagga returns malformed or corrupted response data | `502 Bad Gateway` |
| The Imagga request times out before a response is received | `504 Gateway Timeout` |
| Imagga is unavailable or the connection cannot be established | `503 Service Unavailable` |
| Any remaining unexpected application failure | `500 Internal Server Error` |

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
src/main/java/com/heb/imagedetection
├── config
├── controller
├── detector
├── dto
├── entity
├── exception
├── repository
└── service
```
