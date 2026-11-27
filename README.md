# 🚀 DevPilot — AI-Powered Codebase Intelligence & RAG Chat

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4%2B-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/React-19-blue?style=for-the-badge&logo=react" alt="React 19" />
  <img src="https://img.shields.io/badge/Vite-8.x-purple?style=for-the-badge&logo=vite" alt="Vite" />
  <img src="https://img.shields.io/badge/MySQL-8.4-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/Qdrant-Vector%20DB-red?style=for-the-badge&logo=qdrant" alt="Qdrant" />
  <img src="https://img.shields.io/badge/Groq%20LPU-Ultra--Fast%20AI-F55036?style=for-the-badge" alt="Groq" />
</p>

---

## 📌 Overview

**DevPilot** is an intelligent, developer-first AI code assistant that enables engineers to chat conversationally with their GitHub repositories. Powered by a **Retrieval-Augmented Generation (RAG)** pipeline, DevPilot indexes source code into vector embeddings, performs semantic similarity searches, and streams back context-rich answers grounded in real code citations.

---

## ✨ Key Features

- 🔐 **GitHub OAuth2 Integration**: Seamless one-click authentication with granular repository permissions. Secure AES-256 token encryption at rest.
- ⚡ **Lightning-Fast AI Inference**: Integrates **Groq LPU** inference engine (`openai/gpt-oss-120b`) delivering near-instant response streaming.
- 🧠 **Smart Code Indexing & Chunking**: Fetches repository trees recursively, filters non-essential assets, and intelligently chunks code with overlapping boundaries.
- 🔍 **Vector Semantic Search (Qdrant)**: High-performance vector retrieval matching developer questions to exact code modules and functions.
- 🎯 **Grounded Citations**: Responses include exact file paths, start/end line numbers, and relevant code excerpts.
- 🌊 **Real-Time SSE Streaming**: Low-latency Server-Sent Events (SSE) with a 60fps smooth typewriter effect on the React client.
- 💬 **Multi-Session History**: Persistent chat sessions organized per repository, backed by MySQL.
- 🎨 **Modern Developer UI**: Built with React 19, Tailwind CSS, Lucide icons, and full dark-mode aesthetics.

---

## 🏗️ Architecture

```
                                  ┌────────────────────────┐
                                  │   GitHub OAuth 2.0     │
                                  └───────────▲────────────┘
                                              │ Auth Token
                                              │
┌─────────────────┐       REST / SSE      ┌───┴────────────────┐       gRPC       ┌─────────────────┐
│                 │ ◄───────────────────► │   Spring Boot      │ ───────────────► │   Qdrant DB     │
│   React Client  │    Session Cookie     │   Backend (RAG)    │                  │  (Vector Store) │
│   (Vite + Tailwind)                     └───┬────────────┬───┘                  └─────────────────┘
└─────────────────┘                           │            │
                                         JDBC │            │ HTTPS (Inference)
                                              ▼            ▼
                                      ┌──────────────┐   ┌─────────────────┐
                                      │   MySQL DB   │   │  Groq LPU API   │
                                      │  (Relational)│   │ (GPT-OSS-120B)  │
                                      └──────────────┘   └─────────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.4+ | Core enterprise REST API, RAG orchestration, SSE streaming |
| **Security** | Spring Security 6, OAuth2 | GitHub OAuth, HTTP-only session cookies, AES-256 encryption |
| **Database** | MySQL 8.4 | User accounts, encrypted tokens, repository metadata, chat logs |
| **Vector DB** | Qdrant (latest) | High-speed vector similarity search and metadata filtering |
| **AI / LLM** | Groq LPU, OpenAI Embeddings | Ultra-fast token generation and vector embeddings |
| **Frontend** | React 19, Vite, Tailwind CSS | Single-page application, custom SSE hooks, smooth typewriter |
| **Containers** | Docker & Docker Compose | Local multi-service infrastructure (MySQL + Qdrant) |

---

## 📁 Project Structure

```
DEVPILOT/
├── backend/
│   ├── src/main/java/devPilot/backend/
│   │   ├── config/              # Security, CORS, Encryption & Vector store configs
│   │   ├── controllers/         # Auth, Repository, Chat & Session REST endpoints
│   │   ├── entities/            # JPA Entities (User, Repository, ChatSession, Message)
│   │   ├── repositories/        # Spring Data JPA repositories
│   │   └── services/            # GitHub API, Indexing, Vector Store, ChatStreamHandler
│   ├── src/main/resources/
│   │   └── application.properties # Main Spring configuration with env variables
│   ├── pom.xml                  # Maven dependencies & build setup
│   └── mvnw / mvnw.cmd          # Maven wrapper scripts
├── client/
│   ├── src/
│   │   ├── components/          # Chat, Sidebar, Navbar, Repo Selector, Code Citation
│   │   ├── hooks/               # Custom SSE streaming & typewriter hooks
│   │   ├── context/             # Global Auth & Active Session State
│   │   └── App.jsx              # Main Application Component
│   ├── package.json             # NPM dependencies & scripts
│   └── vite.config.js           # Vite build configuration
├── docker/
│   └── mysql/init.sql           # Schema initialization script
├── docker-compose.yml           # MySQL 8.4 + Qdrant services
├── .env.example                 # Environment variables reference template
└── README.md                    # Project documentation
```

---

## 🚦 Getting Started (Local Setup)

### Prerequisites

- **Java Development Kit (JDK)** 21 or higher
- **Node.js** 18+ and **npm**
- **Docker Desktop** (for running MySQL and Qdrant)
- A **GitHub OAuth Application** ([Create here](https://github.com/settings/developers))
- A **Groq Cloud API Key** ([Get free key here](https://console.groq.com))

---

### Step 1: Start Infrastructure Containers

Run the pre-configured MySQL and Qdrant containers:

```bash
docker compose up -d
```

Verify services are healthy:
- **MySQL**: `localhost:3307`
- **Qdrant Web UI / REST**: `http://localhost:6333/dashboard`
- **Qdrant gRPC**: `localhost:6334`

---

### Step 2: Configure Environment Variables

Create or set the required environment variables. You can export them or configure your IDE run profile:

```bash
# Groq API
export GROQ_API_KEY="gsk_your_groq_api_key_here"

# GitHub OAuth 2.0 Credentials
export GITHUB_CLIENT_ID="your_github_client_id"
export GITHUB_CLIENT_SECRET="your_github_client_secret"

# MySQL
export DB_URL="jdbc:mysql://localhost:3307/devpilot"
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
```

> **Note**: For GitHub OAuth App setup, configure:
> - **Homepage URL**: `http://localhost:5173`
> - **Authorization Callback URL**: `http://localhost:8080/login/oauth2/code/github`

---

### Step 3: Run the Spring Boot Backend

```bash
cd backend
./mvnw clean spring-boot:run
```

*(On Windows PowerShell, use `.\mvnw.cmd clean spring-boot:run`)*

The backend will start at `http://localhost:8080`.

---

### Step 4: Run the React Frontend

Open a new terminal:

```bash
cd client
npm install
npm run dev
```

The frontend will run at `http://localhost:5173`.

---

## 🔑 Environment Variables Reference

| Variable | Description | Default (Local) |
| :--- | :--- | :--- |
| `DB_URL` | JDBC URL for MySQL database | `jdbc:mysql://localhost:3307/devpilot` |
| `DB_USERNAME` | MySQL user | `root` |
| `DB_PASSWORD` | MySQL password | `Naveen@12345` |
| `GROQ_API_KEY` | Groq Cloud API key for LLM inference | *Required* |
| `GROQ_CHAT_MODEL` | LLM model name | `openai/gpt-oss-120b` |
| `EMBEDDING_MODEL` | Embedding model | `text-embedding-3-small` |
| `GITHUB_CLIENT_ID` | GitHub OAuth client ID | *Required* |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth client secret | *Required* |
| `QDRANT_HOST` | Qdrant host | `localhost` |
| `QDRANT_PORT` | Qdrant gRPC port | `6334` |
| `FRONTEND_URL` | Frontend origin for CORS and redirect | `http://localhost:5173` |
| `VITE_API_BASE_URL` | Backend URL for frontend calls | `http://localhost:8080` |

---

## 📦 Deployment Guide

For full production deployment instructions with **Render** (Spring Boot Backend + Managed MySQL + Qdrant Cloud) and **Vercel** (React Frontend), check the [Deployment Guide](#-production-deployment-instructions) or the documentation below.

---

## 📄 License

This project is licensed under the MIT License — see the LICENSE file for details.
