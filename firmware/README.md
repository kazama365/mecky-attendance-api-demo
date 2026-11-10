# ESP32 wiring and libraries

The sketch targets an ESP32 DevKit, AS608/R307 UART fingerprint reader, and SSD1306 128x64 I2C OLED. Use a regulated supply that meets the fingerprint module voltage/current requirements; share ground.

| Signal | ESP32 | Module |
|---|---:|---|
| Fingerprint TX | GPIO16 / RX2 | Sensor TX |
| Fingerprint RX | GPIO17 / TX2 | Sensor RX |
| OLED SDA / SCL | GPIO21 / GPIO22 | SDA / SCL |
| Buzzer + | GPIO25 | Buzzer signal |
| Green LED | GPIO26 | Through resistor |
| Red LED | GPIO27 | Through resistor |
| Ground | GND | Sensor, OLED, buzzer |

Install Arduino libraries **Adafruit Fingerprint Sensor Library**, **Adafruit SSD1306**, **Adafruit GFX Library**, and **ArduinoJson 7**. Install Espressif ESP32 board support; LittleFS, WiFi, HTTPClient and time are included with the core. Set Wi-Fi, server URL, and the exact `DEVICE_API_KEY` before compiling. The server URL must use the PC's LAN IP, not `localhost`.

Enrollment allocates `templateCount + 1` as the sensor slot. Do not delete templates or reorder enrollment slots without also reconciling employee mappings. The sketch persists a JSON queue in LittleFS and retries uploads in order after connectivity returns. Device clock is set from NTP in Asia/Manila; if NTP is unavailable at boot, do not rely on the current placeholder timestamp for a production attendance record.
