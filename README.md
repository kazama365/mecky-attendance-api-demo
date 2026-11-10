# Mecky's attendance demo: browser + API

This Docker app serves the C# Blazor browser portal and the Java Spring Boot API from one Render web service. The root page (`/`) is the browser sign-in page. The desktop WPF app can continue using the same API URL.

## Demo accounts

- Admin: `admin` / `DemoAdmin2026!`
- Worker: `demo-worker-one` / `DemoWorker2026!`
- Worker: `demo-worker-two` / `DemoWorker22026!`

The admin can view the dashboard, employee list, attendance records, and a payroll preview. Admins can start/check fingerprint enrollment for each employee from the Employees page. A worker can view only their own profile and attendance for a selected range of up to five days.

Workers can create an account from **New worker? Create an account** on the sign-in screen. Signup requires an existing active employee ID, a unique username, and a password of at least 10 characters. New accounts are created inactive and cannot sign in until an Admin opens **User access** and selects **Approve account**. Public signup can only create worker accounts; it cannot grant Admin access. Since this demo uses in-memory H2, pending registrations and approvals are erased when Render restarts the service.

## Payroll demo behavior

- Select a date range, calculate the preview, edit/save an employee's daily rate, then save the period as a payroll run. A saved period is immutable; choose a different date range for another run.
- Gross demo pay = attendance days with a time-in scan × daily rate, rounded to centavos. Recorded absences and late minutes are shown for reference but do not change pay. Deductions and undertime are zero/not calculated.
- This is a school/demo payroll calculator, not Philippine statutory payroll. It does not calculate tax, SSS, PhilHealth, Pag-IBIG, overtime, holiday pay, or legal deductions.
- A saved run stores a daily-rate snapshot. MySQL deployments apply migration `V4__payroll_rate_snapshot.sql`; the Render demo's H2 schema is created automatically.
- The current Render demo uses in-memory H2. Payroll runs, attendance and employee-to-fingerprint links are lost when the service restarts or sleeps. Use a persistent database before entering real records.

## Render configuration

Keep these environment variables on the Render web service:

- `SPRING_PROFILES_ACTIVE` = `demo`
- `JWT_SECRET` = a private random value at least 32 characters long
- `DEVICE_API_KEY` = a long private random string, at least 32 characters; the ESP32 sketch must contain this same value

The demo profile uses an in-memory H2 database. Demo records reset when the Render service restarts or sleeps; do not use this demo database for real payroll or production records.

## Update the existing GitHub repository

The repository root should contain `Dockerfile`, `pom.xml`, `src/`, and the new `frontend-web/` directory.

1. Replace the existing root `Dockerfile` with this package's `Dockerfile`.
2. Add/replace the `frontend-web/` directory and the updated Java sources under `src/main/java/`.
3. Add `src/main/resources/db/migration/V4__payroll_rate_snapshot.sql` so MySQL-backed deployments can validate the new payroll field.
4. Add `DEVICE_API_KEY` to the Render service Environment. Its name must be exactly `DEVICE_API_KEY`; its value is copied into the ESP32 sketch. Keep the existing `SPRING_PROFILES_ACTIVE=demo` and `JWT_SECRET` values.
5. Commit the changes to the branch connected to Render (the current demo uses `main`).
6. In Render, open the service and choose **Manual Deploy → Deploy latest commit**. If auto-deploy is on, Render may start building as soon as the commit arrives.
7. Wait for the deployment status to say **Live**, then open the service URL in Chrome. The sign-in page should load at the root URL.

## Connect the ESP32 device

Use `firmware/MeckyAttendanceESP32Web/MeckyAttendanceESP32Web.ino` from the repository root. Install Adafruit Fingerprint Sensor Library, Adafruit GFX, Adafruit SH110X and ArduinoJson 7 in Arduino IDE. Set Wi-Fi credentials and copy the exact **value** of Render's `DEVICE_API_KEY` into the sketch. In Render, the environment variable **key/name** must be exactly `DEVICE_API_KEY` (do not use the secret itself as the key name). Keep `SERVER_URL` set to this service's HTTPS URL.

Wire sensor TX→GPIO16 and RX→GPIO17; SH1106 SDA→GPIO21 and SCL→GPIO22; buzzer I/O→GPIO25. Keep grounds common, select DOIT ESP32 DEVKIT V1, upload, and open Serial Monitor at 115200 baud. Sign in as admin, open Employees → Start enrollment, then scan the same finger twice at the device. Check status in the Employees screen. Successful time-in/out and server-confirmed denials sound one 1-second tone; boot and enrollment remain silent.

The demo firmware uses insecure TLS certificate validation to simplify the Render demo connection. Replace that with a trusted CA certificate before handling real employee data.

## Build locally

The Dockerfile builds the browser project with .NET 10, copies its published static files into the Java app, and builds the Spring Boot jar. Render builds this Dockerfile automatically when the connected GitHub branch changes.
