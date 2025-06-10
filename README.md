# DICOM Uploader Service

A modern, asynchronous Spring Boot microservice for uploading DICOM files from AWS S3 to cloud platforms (Azure or GCP). The service queues jobs, persists metadata locally, and supports job tracking.

---

## 🚀 Features

- ✅ Asynchronous upload of DICOM files
- ✅ Platform support: **GCP** and **Azure**
- ✅ AWS S3 integration using presigned URLs
- ✅ Job queueing with local persistence (file-based)
- ✅ Job tracking via REST API
- ✅ Structured logging with SLF4J
- ✅ Jackson integration for modern Java (JDK 21+) with full `Instant` support
- ✅ Industry-grade folder structure & exception handling

---

## 📁 Project Structure

```sql
+---jobs
|   +---completed
|   |       3a37950b-49e5-4474-925d-eb367fe275f4.json
|   |       4b064e17-eef5-49c9-9953-3d56edf665d2.json
|   |       5da37287-00cb-4d63-8421-15864829745a.json
|   |       65ccbd97-5003-4056-bbd6-34c7f6f0e668.json
|   |       6aec6da2-b687-48fd-92fc-bc6458517dc5.json
|   |       81746844-c6b6-4c03-be66-e8a31f135865.json
|   |       820a361b-614d-4907-a2fd-841a6ca329a3.json
|   |       e3f15dda-ca90-4117-9ffa-5c85f0a1ddfa.json
|   |       
|   +---failed
|   |       13cb57a0-b419-418c-9c57-724f44c0bb75.json
|   |       6d733033-509b-47d2-82a6-31058f62575e.json
|   |       8fd92524-2a3d-4ed7-933f-eb739445c660.json
|   |       91047830-caf8-49e9-90db-0401a8e03efa.json
|   |       fa388de9-2a9a-4cf4-970e-4f1c3c38fe61.json
|   |       
|   +---in_progress
|   \---queued
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
|   |   |               |       AWSs3Config.java
|   |   |               |       AzureConfig.java
|   |   |               |       GCPConfig.java
|   |   |               |       JacksonConfig.java
|   |   |               |       
|   |   |               +---controller
|   |   |               |       DicomController.java
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
|   |   |               |       
|   |   |               +---service
|   |   |               |       AzureUploader.java
|   |   |               |       DicomUploaderService.java
|   |   |               |       GCPUploader.java
|   |   |               |       S3PresignedUrlService.java
|   |   |               |       
|   |   |               \---util
|   |   |                       FileJobStore.java
|   |   |                       
|   |   \---resources
|   |           application.yml
|   |           
|   \---test
|       \---java
|           \---com
|               \---estuate
|                   \---dicom_uploader
|                           DicomUploaderApplicationTests.java
|           
```




## 📦 Tech Stack

- **Java 21**
- **Spring Boot 3+**
- **Jackson Databind + jsr310**
- **AWS SDK (v2)** for presigned URL generation
- **Apache HTTP Components** for file uploads
- **Lombok** for cleaner POJOs
- **SLF4J + Logback** for structured logging

---

## ⚙️ Setup Instructions

### 1. Clone the Repo
```bash
git clone https://github.com/your-org/dicom-uploader.git
cd dicom-uploader
```
### 2. Set AWS Credentials
```bash
Make sure your environment has access to AWS (via ~/.aws/credentials, environment variables, or EC2 role).
```
### 3. Configure application.yml or application.properties
**Note:**
Add your S3 bucket and Azure/GCP credentials if needed.

```yaml
aws:
  s3:
    bucket-name: your-dicom-bucket

azure:
  dicom-endpoint: https://your-instance.dicom.azurehealthcareapis.com
  token-endpoint: https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token
  client-id: your-client-id
  client-secret: your-client-secret
  scope: https://healthcareapis.azure.com/.default

```

**Note:-** For GCP, use standard Application Default Credentials.

### 4. Build the Project
```bash
./mvnw clean install
java -jar target/dicom-uploader-0.0.1-SNAPSHOT.jar

```

## 🧪 API Endpoints
### 📤 Queue a DICOM Upload
```http
POST /dicom/upload
Content-Type: application/json

{
  "objectKey": "path/to/dicom/file.dcm",
  "platform": "azure" // or "gcp"
}
```
**Response:**
```json
{
  "status": "success",
  "message": "Job queued",
  "jobId": "65ccbd97-5003-4056-bbd6-34c7f6f0e668"
}

```
### 🔍 Check Job Status
        
```http
GET /dicom/status/{jobId}
```
**Response:**
```json
{
"jobId": "65ccbd97-5003-4056-bbd6-34c7f6f0e668",
"status": "COMPLETED",
"platform": "azure",
"createdAt": "2025-06-09T10:22:33Z",
"updatedAt": "2025-06-09T10:23:01Z"
}

```

## 💼 Job Status Lifecycle

```text
QUEUED ➝ IN_PROGRESS ➝ COMPLETED / FAILED
```
