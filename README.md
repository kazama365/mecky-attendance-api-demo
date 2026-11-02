# Mecky's attendance demo: browser + API

This Docker app serves the C# Blazor browser portal and the Java Spring Boot API from one Render web service. The root page (`/`) is the browser sign-in page. The desktop WPF app can continue using the same API URL.

## Demo accounts

- Admin: `admin` / `DemoAdmin2026!`
- Worker: `demo-worker-one` / `DemoWorker2026!`
- Worker: `demo-worker-two` / `DemoWorker22026!`

The admin can view the dashboard, employee list, and attendance records. A worker can view only their own profile and attendance for a selected range of up to five days.

## Render configuration

Keep these environment variables on the Render web service:

- `SPRING_PROFILES_ACTIVE` = `demo`
- `JWT_SECRET` = a private random value at least 32 characters long

The demo profile uses an in-memory H2 database. Demo records reset when the Render service restarts or sleeps; do not use this demo database for real payroll or production records.

## Update the existing GitHub repository

The repository root should contain `Dockerfile`, `pom.xml`, `src/`, and the new `frontend-web/` directory.

1. Replace the existing root `Dockerfile` with this package's `Dockerfile`.
2. Replace `src/main/java/com/mecky/attendance/config/SecurityConfig.java` with this package's version. It allows the public browser files and sign-in endpoint while keeping the admin and worker API routes protected.
3. Add the complete `frontend-web/` directory.
4. Commit the changes to the branch connected to Render (the current demo uses `main`).
5. In Render, open the service and choose **Manual Deploy → Deploy latest commit**. If auto-deploy is on, Render may start building as soon as the commit arrives.
6. Wait for the deployment status to say **Live**, then open the service URL in Chrome. The sign-in page should load at the root URL.

## Build locally

The Dockerfile builds the browser project with .NET 10, copies its published static files into the Java app, and builds the Spring Boot jar. Render builds this Dockerfile automatically when the connected GitHub branch changes.
