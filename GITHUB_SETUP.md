# ============================================================
# TeamUp — GitHub Setup & Deployment Guide
# Run these commands in a PowerShell or Git Bash terminal.
# ============================================================

## 1. Repository Strategy

For a student project like TeamUp, use a **Monorepo** (single repository).

Why:
  - One repo = one URL to share with professors/recruiters
  - Simpler CI/CD: one GitHub Actions workflow handles both backend + frontend
  - Atomic commits across both stacks
  - No need to manage two sets of branch protection rules

## 2. Credentials Safety Checklist

Before pushing, confirm these files are NOT tracked by Git:

  - .env                              ← NEVER
  - src/main/resources/application-local.properties  ← NEVER
  - src/main/resources/application-secrets.properties ← NEVER
  - frontend/.env.local               ← NEVER

Verify by running:
    git status --ignored | Select-String "env"

All credential files are already listed in .gitignore

## 3. Step-by-Step Git Setup

--- STEP 1: Navigate to the project root ---
    cd C:\Users\phong\IdeaProjects\TeamDown

--- STEP 2: Initialize Git ---
    git init

--- STEP 3: Configure your identity (once per machine) ---
    git config user.name "Your Name"
    git config user.email "your.email@example.com"

--- STEP 4: Create .env from example (DO NOT edit the real .env yet) ---
    Copy-Item .env.example .env
    Copy-Item frontend/.env.example frontend/.env.local

--- STEP 5: Stage all files (respects .gitignore) ---
    git add .

--- STEP 6: Check what will be committed ---
    git status

    ✅ You should see: src/, frontend/src/, pom.xml, package.json, README.md, .gitignore
    ❌ You should NOT see: node_modules/, target/, .env, .idea/

--- STEP 7: Make the initial commit ---
    git commit -m "feat: initial TeamUp project setup

    - Spring Boot 3 backend: task management, contribution calc,
      peer review, notifications, cron job, PDF/Excel reports
    - React + Vite + Tailwind frontend: task workspace, progress
      slider, effort estimator, real-time dashboard with pie chart,
      leader review portal, anonymous peer review, report export
    - PostgreSQL/MySQL database schema
    - @EnableScheduling cron job for 3-day inactivity warnings

    See README.md for full project documentation."

--- STEP 8: Create the main branch explicitly ---
    git branch -M main

--- STEP 9: Link to your GitHub remote ---
    Replace YOUR_USERNAME and YOUR_REPO_NAME with your actual values.
    git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git

--- STEP 10: Push to GitHub ---
    git push -u origin main

## 4. Verifying Your Push

After pushing, open:
    https://github.com/YOUR_USERNAME/YOUR_REPO_NAME

Confirm:
  ✅ README.md is visible
  ✅ pom.xml is visible
  ✅ frontend/package.json is visible
  ✅ .gitignore is visible
  ✅ NO node_modules/ folder
  ✅ NO target/ folder
  ✅ NO .env file

## 5. If You Made a Mistake (pushed secrets by accident)

DO THIS IMMEDIATELY:
    git filter-repo --path .env --invert-purge --force
    git commit --amend -m "chore: remove accidentally committed .env"
    git push --force

Then ROTATE your exposed credentials (change passwords, API keys).

## 6. GitHub Actions CI/CD (Optional)

Create .github/workflows/ci.yml in your repo for automated testing:
    name: CI
    on: [push, pull_request]
    jobs:
      build:
        runs-on: ubuntu-latest
        steps:
          - uses: actions/checkout@v4
          - name: Set up JDK 17
            uses: actions/setup-java@v4
            with:
              distribution: 'temurin'
              java-version: '17'
          - name: Build with Maven
            run: ./mvnw clean verify -DskipTests
          - name: Build frontend
            run: cd frontend && npm ci && npm run build
