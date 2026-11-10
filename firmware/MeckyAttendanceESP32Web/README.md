# ESP32 online attendance firmware

This sketch connects the AS608/R307 fingerprint module, 1.3-inch SH1106 OLED, and buzzer to the deployed Java API on Render.

## Wiring

| Module pin | ESP32 DOIT DevKit V1 |
|---|---|
| Fingerprint TX | GPIO16 (UART2 RX) |
| Fingerprint RX | GPIO17 (UART2 TX) |
| Fingerprint GND | GND |
| Fingerprint power | Connect only to a positively identified VCC/V+ **input** on the exact reader board. Do not connect a pin you identify as a power **output** to the ESP32 5V pin. |
| OLED SDA | GPIO21 |
| OLED SCL/SCK | GPIO22 |
| OLED GND | GND |
| OLED VCC | 3V3 (unless your OLED board is marked for 5V) |
| Buzzer signal / I/O | GPIO25 |
| Buzzer GND | GND |

Keep grounds common. UART is crossed: sensor TX goes to ESP32 RX16, sensor RX to ESP32 TX17. Do not feed 5 V UART logic into an ESP32 pin; use a level shifter if the sensor's TX is 5 V logic.

## Arduino IDE libraries

Install these libraries from Library Manager:

- Adafruit Fingerprint Sensor Library
- Adafruit GFX Library
- Adafruit SH110X
- ArduinoJson (7.x)

Select **DOIT ESP32 DEVKIT V1**, the USB serial port, and upload. Open Serial Monitor at **115200 baud**.

## Set credentials before upload

Edit the top of `MeckyAttendanceESP32Web.ino`:

- `WIFI_SSID`: the Wi-Fi network name available at the device location.
- `WIFI_PASSWORD`: that network's password.
- `DEVICE_API_KEY`: the exact value of `DEVICE_API_KEY` in Render → service → Environment.
- Keep `SERVER_URL` as `https://mecky-attendance-api-demo.onrender.com` for the current Render service.

The API key must be a long random secret and must match in both places. Do not commit a real key or Wi-Fi password to the public GitHub repository. Render's demo service must have `SPRING_PROFILES_ACTIVE=demo`, `JWT_SECRET`, and `DEVICE_API_KEY` set.

## Enrollment and scanning

Sign in to the admin website, open Employees, start an employee's fingerprint enrollment, then scan the same finger twice when prompted on the OLED/Serial Monitor. The server links the sensor slot to the selected employee. On a scan, the ESP32 posts the slot and time to the API. Accepted time-in/time-out scans show on the admin attendance and worker portal. The buzzer is silent at boot, during enrollment, and for network errors; it gives one 1-second tone only when the server confirms an accepted or denied scan. Duplicate scans only show a message.

The `setInsecure()` TLS setting in this demo sketch skips certificate verification so the ESP32 can connect to Render without manually installing a CA certificate. Use a trusted CA certificate before using real employee or payroll data. The Render Free service can sleep; its first response may take a while. The current Render profile uses in-memory H2, so a restart clears attendance, saved payroll runs, and employee-to-slot links. Fingerprint templates stored inside the sensor remain until the sensor is reset.
