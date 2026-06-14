# TeamUp

> An objective referee for student group projects — automatically tracks real contributions to prevent uneven workloads.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17 + Spring Boot 3.2 |
| Database | PostgreSQL / MySQL |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| Frontend | _(planned)_ |

## Features (Planned)

- Task assignment with a 0–100% progress slider
- Mandatory file submission on task completion (100%)
- Leader approval / rejection workflow for submissions
- Real-time contribution % dashboard (pie chart)
- Automated 3-day inactivity check via scheduled job
- Anonymous peer review with 1–5 scoring
- PDF/Excel report export for teachers

## Project Structure

```
├── pom.xml                              # Maven config (Spring Boot 3.2.6, Java 17)
├── railway.json                         # Railway deployment config (Nixpacks builder)
├── Procfile                            # Railway process type declaration
├── .java-version                       # Java 17 version file
├── README.md                           # Documentation
└── src/main/
    ├── java/com/teamup/
    │   ├── TeamUpApplication.java          # Main entry point (@EnableScheduling)
    │   └── model/
    │       ├── Role.java                  # STUDENT, TEACHER
    │       ├── TaskStatus.java            # TODO, IN_PROGRESS, PENDING_REVIEW, DONE
    │       ├── User.java                  # User entity
    │       ├── Group.java                 # Group entity (maps to groups_table)
    │       ├── Task.java                  # Task entity
    │       ├── Submission.java            # File submission entity
    │       └── PeerReview.java            # Peer review entity
    └── resources/
        ├── application.properties          # DB + JPA config (env var overrides on Railway)
        └── schema.sql                      # Full PostgreSQL + MySQL DDL script
```

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 14+ **or** MySQL 8+

### 1. Clone & configure

```bash
# Create the database
# PostgreSQL:
psql -U postgres -c "CREATE DATABASE teamup_db;"

# MySQL:
# mysql -u root -p -e "CREATE DATABASE teamup_db;"
```

Edit `src/main/resources/application.properties`:

```properties
# PostgreSQL (default)
spring.datasource.url=jdbc:postgresql://localhost:5432/teamup_db
spring.datasource.driver-class-name=org.postgresql.Driver

# OR MySQL — comment out PostgreSQL and uncomment MySQL section
# spring.datasource.url=jdbc:mysql://localhost:3306/teamup_db
# spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### 2. Run

```bash
mvn spring-boot:run
```

> **Note:** With `spring.jpa.hibernate.ddl-auto=update`, Hibernate will auto-create tables on first launch. Run `schema.sql` manually if you want explicit control.

## Database Schema

See `src/main/resources/schema.sql` for the complete PostgreSQL and MySQL DDL.

### Entity Relationships

```
User  (1)──(M)  Group        : leader_id
User  (1)──(M)  Task         : assigned_to
Group (1)──(M)  Task
Task  (1)──(M)  Submission
Group (1)──(M)  PeerReview
User  (1)──(M)  PeerReview   : reviewer_id
User  (1)──(M)  PeerReview   : reviewee_id
```

### Key Constraints

| Entity | Constraint |
|--------|-----------|
| `tasks.progress` | CHECK 0–100 |
| `peer_reviews.score` | CHECK 1–5 |
| `peer_reviews` | UNIQUE(group_id, reviewer_id, reviewee_id) |

## Deploy on Railway

### Prerequisites

- [Railway](https://railway.app) account (login with GitHub)
- Project pushed to a GitHub repository

### Step 1 — Push to GitHub

```bash
cd c:\Users\phong\IdeaProjects\TeamDown
git init
git add .
git commit -m "Initial TeamUp project"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/TeamUp.git
git push -u origin main
```

### Step 2 — Create Railway Project

1. Go to [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub repo**
2. Select your `TeamUp` repository
3. Railway auto-detects Java + Maven via `railway.json` → starts build

### Step 3 — Add PostgreSQL Database

1. In your Railway project → **Add a Service** → **Database** → **Add PostgreSQL**
2. Wait for the database to provision
3. Railway injects `DATABASE_URL` automatically

### Step 4 — Configure Environment Variables

In Railway dashboard → **Variables**, add:

```
JDBC_DATABASE_URL      = (Railway auto-fills this from DATABASE_URL)
JDBC_DATABASE_USERNAME = (auto-fills from DATABASE_URL)
JDBC_DATABASE_PASSWORD = (auto-fills from DATABASE_URL)
```

Or simply reference `DATABASE_URL` directly in `application.properties`.

### Step 5 — Deploy

- Railway triggers a **build** (`mvn clean package`) automatically on push
- On success, app is live at `https://your-project-name.up.railway.app`

### Redeploy

Just push to GitHub — Railway auto-redeploys:

```bash
git add .
git commit -m "Your changes"
git push
```

## Next Steps

1. **Repository layer** — extend `JpaRepository` for each entity
2. **Service layer** — business logic (contribution calculation, cron job)
3. **REST Controllers** — expose API endpoints
4. **Security** — JWT authentication + role-based access
5. **Frontend** — React / Vue SPA
