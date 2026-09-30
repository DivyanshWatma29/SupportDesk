# SupportDesk - Campus Service Desk (IT & Student Support)

SupportDesk is an academic project that models a campus service desk. Students and staff can raise a ticket for an IT or campus problem, track it with a ticket number (no login needed), and support staff manage the ticket until it is resolved.

## Live Demo

**[Full-Stack Demo (Render)](https://supportdesk-frontend-uory.onrender.com/)**

Demo staff login (shown on the Sign In screen): `admin` / `DemoAdmin2026!` or `agent` / `DemoAgent2026!`.

The free host may take 30-50 seconds to wake up on the first visit. The live demo uses a temporary in-memory database (H2), so data resets when it restarts. The MySQL version runs locally using `database/schema.sql`.

## Objective

The objective of this project is to create a simple campus help desk where students and staff can report IT and campus issues to the right department, and support staff can manage them until they are resolved.

## Features

- Raise a support ticket with name, email, department, priority, and issue details
- Generate a ticket number for tracking
- Track a ticket using its ticket number
- View all tickets in a staff dashboard
- Change ticket status from Open to In Progress or Resolved
- Add a short resolution note
- Filter tickets by department, priority, or status

## Technologies Used

- React and Vite for the frontend
- Java and Spring Boot for the backend
- MySQL for data storage
- Spring Data JPA for database operations
- Spring Security for staff login

## Project Files

| Folder/File | Use |
| --- | --- |
| `frontend/` | React user interface for raising, tracking, and managing tickets. |
| `backend/` | Spring Boot API, ticket logic, and database connection. |
| `database/schema.sql` | Creates the MySQL tables. |
| `render.yaml` | Cloud deployment configuration. |

## Database Tables

| Table | Purpose |
| --- | --- |
| `departments` | Stores the departments: IT Support, Staff Support (HR), Fees & Finance, Campus Facilities, and ID Cards & Access. |
| `users` | Stores requester and staff information. |
| `tickets` | Stores ticket title, description, priority, status, and dates. |
| `audit_logs` | Stores simple history when a ticket is created or updated. |

## Ticket Flow

1. A user fills in the Raise Ticket form.
2. The system saves the ticket and generates a ticket number.
3. The user can enter that ticket number in Track Ticket to check its status.
4. A staff member opens the dashboard and changes the status when work starts or finishes.
5. The user can track the same ticket again to see the update.

## How to Run Locally

### Backend

Requirements: Java 21 and Maven.

```powershell
cd backend
$env:ADMIN_PASSWORD = "choose_an_admin_password"
$env:AGENT_PASSWORD = "choose_an_agent_password"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend starts at `http://localhost:8080` using the H2 development database. Use username `admin` or `agent` with the password you set above to open the staff dashboard.

### Frontend

Requirements: Node.js and npm.

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in a browser.

### MySQL Database

For MySQL, run `database/schema.sql` in MySQL Workbench. Then set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` before starting the backend without the `dev` profile.
