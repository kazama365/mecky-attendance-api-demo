/*
  Mecky's Attendance: ESP32 + AS608/R307 + SH1106 OLED + buzzer + Render API

  Sensor UART2: sensor TX -> ESP32 GPIO16 (RX), sensor RX -> ESP32 GPIO17 (TX)
  OLED I2C: SDA -> GPIO21, SCL/SCK -> GPIO22, address usually 0x3C
  Buzzer signal -> GPIO25; buzzer GND -> ESP32 GND

  The buzzer is silent except for one 1-second tone after a server-confirmed
  accepted or denied attendance scan. Fingerprints are stored on the sensor;
  the server stores the employee-to-slot mapping and attendance records.
*/

#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <LittleFS.h>
#include <Adafruit_Fingerprint.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SH110X.h>
#include <time.h>

// Replace these before uploading. DEVICE_API_KEY must exactly match Render's
// DEVICE_API_KEY environment variable. Never commit real credentials to GitHub.
const char* WIFI_SSID = "YOUR_WIFI_NAME";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";
const char* SERVER_URL = "https://mecky-attendance-api-demo.onrender.com";
const char* DEVICE_API_KEY = "PASTE_THE_SAME_DEVICE_API_KEY_FROM_RENDER";
const char* DEVICE_NAME = "ESP32-01";

constexpr int SENSOR_RX_PIN = 16; // Sensor TX connects here
constexpr int SENSOR_TX_PIN = 17; // Sensor RX connects here
constexpr int BUZZER_PIN = 25;
constexpr uint32_t SENSOR_BAUD = 57600;
constexpr uint16_t SENSOR_CAPACITY = 300;
constexpr uint8_t OLED_ADDRESS = 0x3C; // If blank, check 0x3D
constexpr unsigned long HEARTBEAT_INTERVAL_MS = 30000;
constexpr unsigned long COMMAND_POLL_INTERVAL_MS = 4000;
constexpr char QUEUE_FILE[] = "/scan-queue.json";
constexpr char ENROLL_RESULT_FILE[] = "/enroll-result.json";

HardwareSerial sensorSerial(2);
Adafruit_Fingerprint finger(&sensorSerial);
Adafruit_SH1106G display(128, 64, &Wire, -1);
bool oledReady = false;
unsigned long lastHeartbeat = 0;
unsigned long lastCommandPoll = 0;
unsigned long lastReconnectAttempt = 0;

void showScreen(const String& title, const String& detail = "") {
  if (!oledReady) return;
  display.clearDisplay();
  display.setTextColor(SH110X_WHITE);
  display.setTextSize(1);
  display.setCursor(0, 4);
  display.println("MECKY ATTENDANCE");
  display.drawLine(0, 16, 127, 16, SH110X_WHITE);
  display.setTextSize(title.length() > 10 ? 1 : 2);
  display.setCursor(0, 24);
  display.println(title);
  display.setTextSize(1);
  display.setCursor(0, 50);
  display.println(detail);
  display.display();
}

void outcomeBeep(bool accepted) {
  // Deliberately one short event, only after the API returns its final result.
  // Accepted is higher; denied is lower. Each tone lasts exactly one second.
  tone(BUZZER_PIN, accepted ? 2400 : 1200, 1000);
  delay(1010);
  noTone(BUZZER_PIN);
}

String localTimestamp() {
  struct tm timeInfo;
  if (!getLocalTime(&timeInfo, 1500)) return "";
  char buffer[24];
  strftime(buffer, sizeof(buffer), "%Y-%m-%dT%H:%M:%S", &timeInfo);
  return String(buffer);
}

String makeScanUuid() {
  char buffer[37];
  const uint64_t mac = ESP.getEfuseMac();
  const uint32_t a = esp_random();
  const uint16_t b = esp_random() & 0xffff;
  const uint16_t c = esp_random() & 0x0fff;
  const uint16_t d = (esp_random() & 0x3fff) | 0x8000;
  snprintf(buffer, sizeof(buffer), "%08lx-%04x-4%03x-%04x-%012llx",
           static_cast<unsigned long>(a), b, c, d,
           static_cast<unsigned long long>(mac & 0xffffffffffffULL));
  return String(buffer);
}

bool requestServer(const String& path, const String* body, String& response) {
  if (WiFi.status() != WL_CONNECTED) return false;
  WiFiClientSecure secureClient;
  // DEMO ONLY: accepts any HTTPS certificate. For a real payroll deployment,
  // replace setInsecure() with Render's trusted root CA using secureClient.setCACert().
  secureClient.setInsecure();
  HTTPClient http;
  http.setConnectTimeout(20000);
  http.setTimeout(70000); // Render Free may take ~50 seconds to wake after idle.
  if (!http.begin(secureClient, String(SERVER_URL) + path)) return false;
  http.addHeader("X-Device-Key", DEVICE_API_KEY);
  if (body != nullptr) http.addHeader("Content-Type", "application/json");
  const int code = body == nullptr ? http.GET() : http.POST(*body);
  if (code > 0) response = http.getString();
  http.end();
  return code >= 200 && code < 300;
}

bool serverPost(const String& path, const String& body, String& response) {
  return requestServer(path, &body, response);
}

bool serverGet(const String& path, String& response) {
  return requestServer(path, nullptr, response);
}

bool loadQueue(JsonDocument& queue) {
  if (!LittleFS.exists(QUEUE_FILE)) {
    queue["scans"].to<JsonArray>();
    return true;
  }
  File file = LittleFS.open(QUEUE_FILE, "r");
  if (!file) return false;
  const DeserializationError error = deserializeJson(queue, file);
  file.close();
  return !error;
}

void saveQueue(JsonDocument& queue) {
  File file = LittleFS.open(QUEUE_FILE, "w");
  if (file) {
    serializeJson(queue, file);
    file.close();
  }
}

bool queueScan(const String& scanUuid, uint16_t slot, const String& timestamp) {
  JsonDocument queue;
  if (!loadQueue(queue)) return false;
  JsonObject scan = queue["scans"].as<JsonArray>().createNestedObject();
  scan["scanUuid"] = scanUuid;
  scan["slotId"] = slot;
  scan["timestamp"] = timestamp;
  saveQueue(queue);
  return true;
}

void syncQueuedScans() {
  JsonDocument queue;
  if (!loadQueue(queue)) return;
  JsonArray scans = queue["scans"].as<JsonArray>();
  if (scans.isNull() || scans.size() == 0) return;
  String payload;
  serializeJson(queue, payload);
  String response;
  if (serverPost("/api/device/scan", payload, response)) {
    queue.clear();
    queue["scans"].to<JsonArray>();
    saveQueue(queue);
    Serial.println("Queued attendance scans synced to server.");
  }
}

void showServerOutcome(uint16_t sensorSlot, const String& response) {
  JsonDocument result;
  if (deserializeJson(result, response) != DeserializationError::Ok ||
      result["results"].as<JsonArray>().size() == 0) {
    showScreen("SERVER ERROR", "No scan result");
    Serial.println("The API response had no scan result.");
    return;
  }

  JsonObject scan = result["results"][0];
  const String status = scan["status"] | "";
  const String name = scan["employeeName"] | "";
  Serial.printf("Server scan status: %s, sensor slot: %u\n", status.c_str(), sensorSlot);

  if (status == "TIME_IN") {
    showScreen("ACCESS OK", name);
    outcomeBeep(true);
  } else if (status == "TIME_OUT") {
    showScreen("TIME OUT OK", name);
    outcomeBeep(true);
  } else if (status == "UNREGISTERED" || status == "REJECTED") {
    showScreen("ACCESS DENIED", "Not linked / rejected");
    outcomeBeep(false);
  } else if (status == "DUPLICATE") {
    showScreen("ALREADY SCANNED", name);
    // A duplicate is informational; do not add an extra alarm tone.
  } else {
    showScreen("SCAN ERROR", status);
    Serial.println("No attendance decision was returned; buzzer remains silent.");
  }
}

void submitScan(uint16_t slot) {
  const String timestamp = localTimestamp();
  if (timestamp.isEmpty()) {
    showScreen("TIME NOT READY", "Check Wi-Fi / NTP");
    Serial.println("No reliable time yet. Scan was not recorded.");
    return;
  }

  const String uuid = makeScanUuid();
  JsonDocument body;
  JsonObject scan = body["scans"].to<JsonArray>().createNestedObject();
  scan["scanUuid"] = uuid;
  scan["slotId"] = slot;
  scan["timestamp"] = timestamp;
  String payload;
  serializeJson(body, payload);
  String response;

  if (WiFi.status() != WL_CONNECTED) {
    if (queueScan(uuid, slot, timestamp)) {
      showScreen("SAVED OFFLINE", "Will sync later");
      Serial.println("Network unavailable; scan queued. No access tone until server confirms.");
    } else {
      showScreen("SAVE FAILED", "Check device storage");
      Serial.println("Unable to save scan to the ESP32 queue.");
    }
    return;
  }

  showScreen("CONTACTING API", "Please wait");
  if (serverPost("/api/device/scan", payload, response)) {
    showServerOutcome(slot, response);
    syncQueuedScans();
  } else {
    // Do not queue a timeout/401 response: a request may already have reached
    // the server. Keeping the same scan UUID and retrying is handled only for
    // known offline periods to avoid duplicate/ambiguous attendance outcomes.
    showScreen("API UNAVAILABLE", "Check Render / key");
    Serial.println("API call failed. Check Render service, Wi-Fi, and device key.");
  }
}

bool waitForFinger(const char* prompt) {
  showScreen("ENROLL", prompt);
  const unsigned long start = millis();
  while (millis() - start < 30000) {
    const uint8_t result = finger.getImage();
    if (result == FINGERPRINT_OK) return true;
    if (result != FINGERPRINT_NOFINGER && result != FINGERPRINT_PACKETRECIEVEERR) {
      Serial.printf("Enrollment image error: 0x%02x\n", result);
    }
    delay(80);
  }
  return false;
}

bool waitForFingerRemoval() {
  showScreen("LIFT FINGER", "Remove completely");
  const unsigned long start = millis();
  while (millis() - start < 20000) {
    if (finger.getImage() == FINGERPRINT_NOFINGER) return true;
    delay(80);
  }
  return false;
}

int findFreeSlot() {
  finger.getTemplateCount();
  const int capacity = finger.capacity > 0 ? min((int)finger.capacity, (int)SENSOR_CAPACITY) : SENSOR_CAPACITY;
  for (int slot = 1; slot <= capacity; ++slot) {
    if (finger.loadModel(slot) != FINGERPRINT_OK) return slot;
  }
  return -1;
}

bool reportEnrollment(long commandId, bool success, int slot, const String& message) {
  JsonDocument body;
  body["commandId"] = commandId;
  body["success"] = success;
  body["slotId"] = success ? slot : 0;
  body["message"] = message;
  String payload;
  serializeJson(body, payload);
  String response;
  if (serverPost("/api/device/enroll-result", payload, response)) {
    LittleFS.remove(ENROLL_RESULT_FILE);
    return true;
  }
  File file = LittleFS.open(ENROLL_RESULT_FILE, "w");
  if (file) { file.print(payload); file.close(); }
  Serial.println("Enrollment result saved locally; the ESP32 will retry it when connected.");
  return false;
}

void syncEnrollmentResult() {
  if (!LittleFS.exists(ENROLL_RESULT_FILE)) return;
  File file = LittleFS.open(ENROLL_RESULT_FILE, "r");
  if (!file) return;
  String payload = file.readString();
  file.close();
  String response;
  if (serverPost("/api/device/enroll-result", payload, response)) {
    LittleFS.remove(ENROLL_RESULT_FILE);
    Serial.println("Pending enrollment result synced to server.");
  }
}

void enrollFromCommand(long commandId) {
  const int slot = findFreeSlot();
  if (slot < 1) {
    Serial.println("No empty fingerprint slots available.");
    reportEnrollment(commandId, false, 0, "No empty fingerprint slot available");
    showScreen("ENROLL FAILED", "Sensor is full");
    return;
  }

  Serial.printf("Enrollment command %ld. Selected sensor slot %d.\n", commandId, slot);
  bool success = false;
  if (waitForFinger("Place finger")) {
    uint8_t result = finger.image2Tz(1);
    if (result == FINGERPRINT_OK && waitForFingerRemoval() && waitForFinger("Same finger again")) {
      result = finger.image2Tz(2);
      if (result == FINGERPRINT_OK && finger.createModel() == FINGERPRINT_OK &&
          finger.storeModel(slot) == FINGERPRINT_OK) success = true;
      else Serial.printf("Could not create/store template, sensor code 0x%02x\n", result);
    } else {
      Serial.printf("First enrollment scan failed, sensor code 0x%02x\n", result);
    }
  }

  reportEnrollment(commandId, success, slot,
                   success ? "Fingerprint enrolled" : "Enrollment timed out or scans did not match");
  if (success) {
    showScreen("ENROLLED", "Slot " + String(slot));
    Serial.printf("Enrollment successful. Sensor slot %d linked to employee by API.\n", slot);
  } else {
    showScreen("ENROLL FAILED", "Try again from site");
    Serial.println("Enrollment failed; start a new enrollment from the admin website.");
  }
  delay(1200);
}

void pollForEnrollmentCommand() {
  String response;
  if (!serverGet("/api/device/command", response)) return;
  JsonDocument command;
  if (deserializeJson(command, response) != DeserializationError::Ok) return;
  const String type = command["type"] | "NONE";
  if (type == "ENROLL") enrollFromCommand(command["id"] | 0L);
}

void sendHeartbeat() {
  JsonDocument body;
  body["device"] = DEVICE_NAME;
  body["firmware"] = "2.0.0";
  String payload;
  serializeJson(body, payload);
  String response;
  if (!serverPost("/api/device/heartbeat", payload, response))
    Serial.println("Heartbeat failed; check Render, Wi-Fi, and DEVICE_API_KEY.");
}

void waitForScannerRelease() {
  const unsigned long start = millis();
  while (millis() - start < 8000) {
    if (finger.getImage() == FINGERPRINT_NOFINGER) return;
    delay(100);
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  Wire.begin(21, 22);
  oledReady = display.begin(OLED_ADDRESS, true);
  showScreen("STARTING", "Mecky attendance");

  if (!LittleFS.begin(true)) Serial.println("LittleFS mount failed; offline queue unavailable.");
  sensorSerial.begin(SENSOR_BAUD, SERIAL_8N1, SENSOR_RX_PIN, SENSOR_TX_PIN);
  finger.begin(SENSOR_BAUD);
  if (!finger.verifyPassword()) {
    showScreen("SENSOR ERROR", "Check sensor UART");
    Serial.println("Fingerprint sensor did not respond. Check V+, GND, and crossed TX/RX.");
    while (true) delay(1000); // Silent failure loop; no continuous buzzer.
  }

  showScreen("CONNECTING", "Joining Wi-Fi");
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  configTzTime("PST-8", "pool.ntp.org", "time.google.com");
  const unsigned long wifiStart = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - wifiStart < 20000) delay(250);

  if (WiFi.status() == WL_CONNECTED) {
    showScreen("WAITING FOR TIME", "NTP sync");
    struct tm timeInfo;
    getLocalTime(&timeInfo, 12000);
    showScreen("READY", "Scan fingerprint");
    sendHeartbeat();
    syncEnrollmentResult();
    syncQueuedScans();
  } else {
    showScreen("OFFLINE", "Check Wi-Fi setup");
    Serial.println("Wi-Fi not connected. A valid time is required for scans to queue.");
  }
  Serial.println("Mecky's ESP32 attendance device is ready.");
}

void loop() {
  const unsigned long now = millis();
  if (WiFi.status() != WL_CONNECTED) {
    if (now - lastReconnectAttempt >= 5000) {
      lastReconnectAttempt = now;
      WiFi.reconnect();
    }
  } else {
    if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
      lastHeartbeat = now;
      sendHeartbeat();
    }
    if (now - lastCommandPoll >= COMMAND_POLL_INTERVAL_MS) {
      lastCommandPoll = now;
      syncEnrollmentResult();
      pollForEnrollmentCommand();
      syncQueuedScans();
    }
  }

  const uint8_t image = finger.getImage();
  if (image == FINGERPRINT_OK) {
    const uint8_t converted = finger.image2Tz();
    if (converted == FINGERPRINT_OK) {
      const uint8_t match = finger.fingerFastSearch();
      if (match == FINGERPRINT_OK) submitScan(finger.fingerID);
      else if (match == FINGERPRINT_NOTFOUND) submitScan(0); // server returns denied
      else Serial.printf("Fingerprint search failed: 0x%02x\n", match);
      waitForScannerRelease();
      showScreen("READY", "Scan fingerprint");
    } else {
      Serial.printf("Fingerprint image conversion failed: 0x%02x\n", converted);
    }
  }
  delay(80);
}
