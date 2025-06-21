
# 🏥 DICOM Uploader Microservice

A modern, **asynchronous**, **cloud-agnostic** Spring Boot service for **uploading and retrieving DICOM files** from various cloud storage platforms (AWS S3, Azure Blob, GCP Storage, and native GCP DICOM Store). The system ensures **high-performance**, **non-blocking job processing**, **platform-based delegation**, and **robust local job status tracking**.

---

## 🚀 Key Features

- ✅ Asynchronous job queue with scheduled background processing (non-blocking)
- ✅ Supports uploads via:
  - **AWS S3 presigned URLs**
  - **Azure Blob Storage**
  - **GCP Cloud Storage**
- ✅ Supports native retrieval via **GCP Healthcare DICOM API**
- ✅ Multi-cloud platform delegation via **Factory Pattern**
- ✅ Job persistence and tracking using **local filesystem-based JSON files**
- ✅ Job lifecycle visibility with REST endpoints (`QUEUED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`)
- ✅ Structured and centralized error handling
- ✅ Fully decoupled **service-oriented architecture**
- ✅ Clean modular folder structure and logging
- ✅ Support for large file handling (via streaming uploads)
- ✅ Designed with **industry-grade async patterns** using Spring's `@Async` and scheduler

---

## 📦 Tech Stack

| Layer | Tech |
|------|------|
| Language | **Java 21** |
| Framework | **Spring Boot 3+** |
| HTTP | **Apache HttpComponents 5+** |
| Logging | **SLF4J + Logback** |
| JSON & Dates | **Jackson + JSR310 (Instant)** |
| Cloud | **AWS SDK v2**, **Azure SDK**, **Google Cloud Healthcare API** |
| Tools | **Lombok**, **OpenAPI**, **Postman** |

---

## 🗂️ Architecture Overview

![Alt Text](https://raw.githubusercontent.com/latecoder10/nDP_VNA/refs/heads/master/nDP_v1.svg)

---
## 🗂️ Folder Structure
```sql
+---jobs
|   +---completed
|   |       b2a716cc-58c9-4af7-ab73-2ee8984766a6.json
|   |       bce80d2b-77d4-43ef-bfc3-2c276540a91b.json
|   |       d4761015-84b0-414e-ae64-52cc88107d0d.json
|   |       
|   +---failed
|   |       ee2fcd59-1361-4bb4-9d9a-f25645388e7b.json
|   |       f492b6cb-d7fb-41e3-9c50-220ec8675286.json
|   |       
|   +---in_progress
|   \---queued
+---postman-collection
|       Dicom-Uploader.postman_collection.json
|       
+---src
|   +---main
|   |   +---java
|   |   |   \---com
|   |   |       \---estuate
|   |   |           \---dicom_uploader
|   |   |               |   DicomUploaderApplication.java
|   |   |               |   
|   |   |               +---async
|   |   |               |       JobProcessor.java
|   |   |               |       JobQueueManager.java
|   |   |               |       JobScheduler.java
|   |   |               |       
|   |   |               +---config
|   |   |               |       AsyncConfig.java
|   |   |               |       AWSs3Config.java
|   |   |               |       AzureConfig.java
|   |   |               |       GCPConfig.java
|   |   |               |       JacksonConfig.java
|   |   |               |       
|   |   |               +---controller
|   |   |               |       DicomController.java
|   |   |               |       DicomDeleteController.java
|   |   |               |       
|   |   |               +---dto
|   |   |               |       DicomBlobRetrievalRequest.java
|   |   |               |       DicomRetrievalRequest.java
|   |   |               |       UploadRequest.java
|   |   |               |       UploadResponse.java
|   |   |               |       
|   |   |               +---exception
|   |   |               |       DicomConflictException.java
|   |   |               |       DicomUploadException.java
|   |   |               |       GlobalExceptionHandler.java
|   |   |               |       InvalidTokenException.java
|   |   |               |       UploadFailedException.java
|   |   |               |       
|   |   |               +---model
|   |   |               |       ErrorResponse.java
|   |   |               |       Job.java
|   |   |               |       JobStatus.java
|   |   |               |       JobType.java
|   |   |               |       
|   |   |               +---service
|   |   |               |   |   CloudUploaderFactory.java
|   |   |               |   |   DicomRetrievalService.java
|   |   |               |   |   DownloaderService.java
|   |   |               |   |   S3PresignedUrlService.java
|   |   |               |   |   UploadOrchestratorService.java
|   |   |               |   |   
|   |   |               |   +---azure
|   |   |               |   |       AzureUploaderService.java
|   |   |               |   |       
|   |   |               |   +---gcp
|   |   |               |   |       GCPRetrievalService.java
|   |   |               |   |       GCPUploaderService.java
|   |   |               |   |       
|   |   |               |   \---uploader
|   |   |               |       |   CloudUploader.java
|   |   |               |       |   
|   |   |               |       \---impl
|   |   |               |               AzureUploader.java
|   |   |               |               BlobUploaderAzure.java
|   |   |               |               BlobUploaderGCP.java
|   |   |               |               GCPUploader.java
|   |   |               |               
|   |   |               \---util
|   |   |                       DicomValidator.java
|   |   |                       FileJobStore.java
|   |   |                       
|   |   \---resources
|   |           application.yml
|   |           

```

## ⚙️ Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/dicom-uploader.git
cd dicom-uploader
```

### 2. Configure Your Cloud Credentials

Edit `application.yml` or `application.properties`:

```yaml
aws:
  region: us-east-1
  s3:
    bucket-name: your-dicom-bucket

azure:
  dicom-endpoint: https://your-instance.dicom.azurehealthcareapis.com
  token-endpoint: https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token
  client-id: your-client-id
  client-secret: your-client-secret
  scope: https://healthcareapis.azure.com/.default

gcp:
  project-id: your-gcp-project
  region: your-gcp-region
  dataset: your-dataset
  dicom-store: your-dicom-store
```

> 📝 For GCP, ensure your service account or local environment is authenticated using `GOOGLE_APPLICATION_CREDENTIALS`.

### 3. Build and Run the Project
```bash
./mvnw clean install
java -jar target/dicom-uploader-0.0.1-SNAPSHOT.jar
```

---

## 🔌 API Endpoints

### 📤 Upload DICOM File (Async)
```http
POST /dicom/upload
Content-Type: application/json

{
  "objectKey": "path/to/file.dcm",
  "platform": "azure" // or "gcp", "s3"
}
```

**Response**
```json
{
  "status": "success",
  "message": "Job queued",
  "jobId": "bce80d2b-77d4-43ef-bfc3-2c276540a91b"
}
```

---

### 📥 Retrieve DICOM File from GCP
```http
POST /dicom/retrieve
Content-Type: application/json

{
  "studyInstanceUID": "1.2.840.113619...",
  "seriesInstanceUID": "...",
  "sopInstanceUID": "..."
}
```

---

### ❌ Delete a Local DICOM Job (from filesystem)
```http
DELETE /dicom/delete?jobId=<jobId>
```

---

### 🔍 Check Job Status
```http
GET /dicom/status/{jobId}
```

**Response**
```json
{
  "jobId": "bce80d2b-77d4-43ef-bfc3-2c276540a91b",
  "status": "COMPLETED",
  "platform": "azure",
  "createdAt": "2025-06-09T10:22:33Z",
  "updatedAt": "2025-06-09T10:23:01Z"
}
```

---

## 📊 Job Lifecycle

```text
QUEUED → IN_PROGRESS → COMPLETED / FAILED
```

Each stage corresponds to a file saved under:
```bash
/jobs/queued/
       in_progress/
       completed/
       failed/
```

---

## 📜 Logging & Monitoring

- Uses **SLF4J + Logback**
- Async flow logs job IDs, status, platform, timestamps
- All job events are persisted as `.json` in `/jobs/<status>/` folder

---

## 🧩 Future Enhancements (Pluggable Roadmap)

- [ ] Native Azure DICOM API retrieval
- [ ] Retry mechanism for transient failures
- [ ] Export logs in `.csv` for audit
- [ ] JWT/OAuth2-based API security
- [ ] UI Dashboard for job monitoring

---

## 👨‍💻 Contributing

1. Fork the repository
2. Create a feature branch
3. Submit a pull request with clear title and description

---

## 📜 License

MIT License. © Estuate Inc.
