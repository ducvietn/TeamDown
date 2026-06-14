# TeamUp — Railway Deployment Guide

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  Railway Project                                        │
│                                                         │
│  ┌────────────────────┐    ┌────────────────────────┐  │
│  │  Backend Service   │    │   PostgreSQL Plugin    │  │
│  │  Spring Boot 3     │    │   ( Relational DB )    │  │
│  │  Port 8080         │    │   DATABASE_URL         │  │
│  │  teamup-0.0.1.jar  │    └────────────────────────┘  │
│  └────────┬───────────┘                                 │
│           │                                              │
│           │  spring.data.mongodb.uri                     │
│           ▼                                              │
│  ┌────────────────────┐    ┌────────────────────────┐  │
│  │  MongoDB Plugin    │    │   Frontend Service     │  │
│  │  (GridFS Storage)  │    │   React + Vite        │  │
│  │  MONGO_URL         │    │   Port 3000           │  │
│  │  Port 27017        │    │   (Static hosting)     │  │
│  └────────────────────┘    └────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Part 1 — Provision Databases on Railway

### Step 1.1: Create a Railway Project

1. Go to [railway.app](https://railway.app) → **New Project**
2. Select **Empty Project** → name it `teamup`
3. You will be redirected to the project dashboard

### Step 1.2: Add PostgreSQL

1. Click **New** → **Database** → **Add PostgreSQL**
2. Wait for the plugin to provision (typically 30–60 seconds)
3. Once ready, click on the PostgreSQL plugin → **Variables** tab
4. Railway auto-generates these variables (note them down):
   ```
   DATABASE_URL=postgresql://user:password@host:port/dbname
   ```

### Step 1.3: Add MongoDB

1. Click **New** → **Database** → **Add MongoDB**
2. Wait for provisioning to complete
3. Click the MongoDB plugin → **Variables** tab
4. Railway provides `MONGODB_URL` or a connection URI variable
5. **You need the full connection string.** Click **Connect** → copy the connection string:
   ```
   MONGO_URL=mongodb://user:password@host:port/dbname
   ```
   or if Railway provides individual variables:
   ```
   MONGO_URL=mongodb://host:port/dbname
   MONGO_USERNAME=user
   MONGO_PASSWORD=password
   ```

### Step 1.4: Note all plugin connection strings

| Plugin | Variable Name | Format |
|--------|-------------|--------|
| PostgreSQL | `DATABASE_URL` | `postgresql://user:pass@host:port/db` |
| PostgreSQL | `JDBC_DATABASE_URL` | Same as DATABASE_URL |
| PostgreSQL | `JDBC_DATABASE_USERNAME` | Extracted from DATABASE_URL |
| PostgreSQL | `JDBC_DATABASE_PASSWORD` | Extracted from DATABASE_URL |
| MongoDB | `MONGO_URL` | `mongodb://user:pass@host:port/db` |
| MongoDB | `MONGO_DATABASE` | `teamup` (create this DB name manually if needed) |

---

## Part 2 — Deploy the Spring Boot Backend

### Step 2.1: Connect GitHub Repository

1. In your Railway project, click **New** → **Deploy from GitHub repo**
2. Authorize Railway to access your GitHub account
3. Select your `TeamUp` repository
4. Railway will auto-detect the Java Maven project from `railway.json`

### Step 2.2: Configure Build & Start

Railway uses the existing `railway.json`:

```json
{
  "build": {
    "builder": "NIXPACKS",
    "nixpacks": { "plan": "java-maven" }
  },
  "deploy": {
    "startCommand": "java -jar target/teamup-0.0.1-SNAPSHOT.jar"
  }
}
```

No changes needed — Nixpacks will:
- Detect Java 17 from pom.xml
- Run `mvn clean package -DskipTests`
- Produce `target/teamup-0.0.1-SNAPSHOT.jar`

### Step 2.3: Set Environment Variables

Navigate to your backend service → **Variables** tab.

Add these variables (replace values with your actual PostgreSQL connection):

| Variable | Value | Notes |
|----------|-------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production profile |
| `PORT` | `8080` | Railway sets this automatically; keep as fallback |
| `MONGO_URL` | `mongodb://user:pass@host:port/db` | From Step 1.3 |
| `MONGO_DATABASE` | `teamup` | The MongoDB database name |
| `JDBC_DATABASE_URL` | `postgresql://user:pass@host:port/db` | From Step 1.2 |
| `JDBC_DATABASE_USERNAME` | `user` | From DATABASE_URL |
| `JDBC_DATABASE_PASSWORD` | `password` | From DATABASE_URL |
| `CORS_ALLOWED_ORIGINS` | `https://your-frontend-app.up.railway.app` | Your Railway frontend URL (add later) |

> **Tip:** Instead of splitting JDBC variables individually, Spring Boot also accepts the full `DATABASE_URL` directly:
> ```
> DATABASE_URL=postgresql://user:pass@host:port/dbname
> spring.datasource.url=${DATABASE_URL}
> ```

### Step 2.4: Deploy

1. Click **Deploy** (or push to the connected GitHub branch)
2. Watch the build log in real-time
3. After the build completes, Railway will start the app
4. Find the public URL at the top: `https://teamup-backend.up.railway.app`

### Step 2.5: Verify Backend is Running

```bash
curl https://your-backend-url.up.railway.app/api/users
# Should return [] or {"success":true,"data":[],"message":"..."}
```

---

## Part 3 — Deploy the Frontend

### Step 3.1: Create a New Railway Service

1. In the same Railway project → **New** → **Empty Service**
2. Name it `frontend`

### Step 3.2: Connect GitHub

1. Click **Deploy from GitHub repo**
2. Select your `TeamUp` repository
3. Under **Root Directory**, enter: `frontend`

### Step 3.3: Configure Build & Start

The `frontend/railway.json` configures Railway to use Node.js:

```json
{
  "build": { "builder": "NIXPACKS", "nixpacks": { "plan": "nodejs" } },
  "deploy": {
    "numReplicas": 1,
    "startCommand": "npm run start",
    "healthcheckPath": "/"
  }
}
```

Railway will detect `package.json` and run `npm install` then `npm run start`.

> **Important:** Add a `start` script to `frontend/package.json` for production:
> ```json
> "scripts": {
>   "dev": "vite",
>   "build": "vite build",
>   "preview": "vite preview",
>   "start": "npx serve -s dist -l 3000"
> }
> ```
> Then run: `cd frontend && npm install && npm install serve`

### Step 3.4: Set Environment Variables

Navigate to the frontend service → **Variables**:

| Variable | Value |
|----------|-------|
| `VITE_API_URL` | `https://your-backend-url.up.railway.app/api` |
| `NODE_ENV` | `production` |

### Step 3.5: Update Backend CORS

Go back to the backend service → **Variables** and add:

```
CORS_ALLOWED_ORIGINS=https://your-frontend-url.up.railway.app
```

Then redeploy the backend to pick up the new CORS variable.

---

## Part 4 — Frontend Integration (File Upload Flow)

The updated submission flow with MongoDB GridFS:

```
Student selects PDF
        │
        ▼
POST /api/files/upload (multipart/form-data)
  body: { file: PDF, uploaderId, taskId }
        │
        ▼
GridFSService.store() → MongoDB GridFS
        │
        ▼
Returns: { gridFsFileId: "66f1a2b3c4d5e6f7a8b9c0d1" }
        │
        ▼
Frontend stores gridFsFileId locally
  OR immediately calls submitFile() with the formData
        │
        ▼
POST /api/tasks/{taskId}/submit-file (multipart/form-data)
  → Backend stores file in GridFS
  → Saves Submission(gridFsFileId=...) in PostgreSQL
  → Task status → PENDING_REVIEW
```

### Frontend code to submit a PDF file:

```javascript
// In ProgressSlider.jsx or TaskCard.jsx
const handleSubmitFile = async (taskId, file) => {
  const formData = new FormData()
  formData.append('file', file)                      // PDF file
  formData.append('uploaderId', String(currentUser.userId))

  try {
    // Step 1: Upload PDF to MongoDB GridFS
    const uploadResult = await fileApi.upload(formData)
    const gridFsFileId = uploadResult.gridFsFileId

    // Step 2: Submit task with the gridFsFileId
    await taskApi.submitFile(taskId, formData)
    alert('Nộp bài thành công! Trưởng nhóm sẽ duyệt.')
    onSubmitted()
  } catch (e) {
    alert('Lỗi: ' + e.message)
  }
}

// Leader downloads a submitted PDF:
const downloadUrl = fileApi.getDownloadUrl(gridFsFileId)
// => "https://backend.up.railway.app/api/files/66f1a2b3..."
window.open(downloadUrl, '_blank')
```

---

## Part 5 — Database Schema Migration

Since `ddl-auto=update`, Hibernate will automatically add the new columns:

```sql
-- After first deployment, Hibernate will run:
ALTER TABLE submissions ADD COLUMN gridfs_file_id VARCHAR(64);
ALTER TABLE submissions ADD COLUMN original_filename VARCHAR(255) NOT NULL;
ALTER TABLE submissions ADD COLUMN content_type VARCHAR(100);
ALTER TABLE submissions ADD COLUMN file_size_bytes BIGINT;
```

> **Important:** If you have existing submissions, they will have `null` values for the new columns.
> Hibernate will not populate them automatically. Run a migration:
>
> ```sql
> UPDATE submissions
> SET original_filename = 'submission.pdf',
>     content_type = 'application/pdf'
> WHERE original_filename IS NULL;
> ```

For production, switch to Flyway or Liquibase migrations instead of `ddl-auto=update`.

---

## Part 6 — Troubleshooting

| Problem | Solution |
|---------|----------|
| **MongoDB connection refused** | Verify `MONGO_URL` is correct and the MongoDB plugin is on the same Railway project |
| **CORS errors in frontend** | Add frontend URL to `CORS_ALLOWED_ORIGINS` in backend env vars |
| **400 Bad Request on file upload** | Ensure Content-Type is `multipart/form-data`, not `application/json` |
| **Build fails: Maven not found** | Railway Nixpacks auto-detects Java Maven; ensure pom.xml is in repo root |
| **File download returns 404** | Verify `gridFsFileId` is valid (24-char hex string) |
| **File too large** | Check `spring.servlet.multipart.max-file-size=50MB` in application.properties |
| **Spring Boot doesn't read env vars** | Restart the service after adding environment variables |
| **PostgreSQL connection refused** | Ensure PostgreSQL plugin is running and `JDBC_DATABASE_URL` is correct |

---

## Part 7 — Production Checklist

- [ ] PostgreSQL plugin provisioned and `JDBC_DATABASE_URL` set
- [ ] MongoDB plugin provisioned and `MONGO_URL` set
- [ ] Backend deployed with all environment variables
- [ ] Frontend deployed with `VITE_API_URL` pointing to backend
- [ ] CORS configured with both Railway URLs
- [ ] Existing `submission` table schema migrated (new columns)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` (switch from `update` after first deploy)
- [ ] Maven test suite passing before pushing to production
- [ ] `spring.jpa.show-sql=false` confirmed in production
