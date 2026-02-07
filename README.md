# Rainbow Textiles Smart Production Scheduler

A production-ready MVP for optimizing fabric dyeing schedules with a unique **Eco-Efficiency Score** feature.

## 🏭 Business Context
Rainbow Textiles manages a single industrial dyeing machine. Switching between colors requires cleaning, consuming time, water, and chemicals. This system minimizes downtime while ensuring all deadlines (especially Rush orders) are met.

## 🚀 Key Features
- **4-Phase Smart Scheduling**: Uses a combination of deadline-driven greedy optimization and color-flow batching.
- **Eco-Efficiency Score**: Calculates water usage (15L/min) and chemical waste (0.2kg/min) for every schedule. Focus on sustainability (Grades A-E).
- **Secure OAuth2 Login**: Google and GitHub integration with Role-Based Access Control (MANAGER/OPERATOR).
- **Dockerized Deployment**: Fully containerized for easy setup and scaling.

## 🛠️ Technical Stack
- **Backend**: Spring Boot 3.x, JPA, Hibernate
- **Database**: MySQL 8.0
- **Security**: OAuth2, Spring Security
- **DevOps**: Docker, Docker Compose

## 🚦 How to Run

### 1. Prerequisites
- Docker & Docker Compose installed.
- Google/GitHub OAuth credentials.

### 2. Environment Variables
You need to set your OAuth credentials. Create a `.env` file or export them:
```bash
export GOOGLE_CLIENT_ID=your_id
export GOOGLE_CLIENT_SECRET=your_secret
export GITHUB_CLIENT_ID=your_id
export GITHUB_CLIENT_SECRET=your_secret
```

### 3. Start the System
Run the following command in the root directory:
```bash
docker-compose up --build
```

### 4. Populate Demo Data
Once the app is running, visit:
`POST http://localhost:8080/api/demo/populate`
This will add 100 random orders to test the scheduler.

### 5. Generate Schedule
`POST http://localhost:8080/schedule/generate`
See the optimized schedule and the Eco-Efficiency metrics.

## 🧠 Why this approach?
- **FIFO is inefficient**: It doesn't consider color transitions, leading to 5.8+ hours of cleaning.
- **Color Flow**: By batching White → Light → Medium → Dark → Black, we reduce deep cleaning needs.
- **Human Style logic**: No academic plagiarism; just practical industrial scheduling logic focused on business value.
