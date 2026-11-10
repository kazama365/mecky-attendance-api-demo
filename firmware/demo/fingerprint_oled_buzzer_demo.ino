/*
  Mecky's fingerprint + OLED + buzzer demo
  Board: DOIT ESP32 DEVKIT V1

  Libraries to install through Arduino Library Manager:
    - Adafruit Fingerprint Sensor Library
    - Adafruit GFX Library
    - Adafruit SH110X

  Serial Monitor: 115200 baud, Newline
  Type E and press Enter to enroll a fingerprint.
*/

#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SH110X.h>
#include <Adafruit_Fingerprint.h>

// AS608/R307 UART wiring (cross TX and RX):
// Sensor TX -> ESP32 GPIO16 (RX2)
// Sensor RX -> ESP32 GPIO17 (TX2)
constexpr int FINGER_RX_PIN = 16;
constexpr int FINGER_TX_PIN = 17;
constexpr uint32_t FINGER_BAUD = 57600;

// 1.3-inch I2C SH1106 OLED: SDA -> GPIO21, SCL -> GPIO22.
constexpr int OLED_SDA_PIN = 21;
constexpr int OLED_SCL_PIN = 22;
constexpr uint8_t OLED_ADDRESS = 0x3C; // If blank, try 0x3D.

// Buzzer module I/O/SIG -> GPIO25. Its VCC and GND connect separately.
constexpr int BUZZER_PIN = 25;
// Default assumes an active-low buzzer module. If yours is active-HIGH, use 0.
#define BUZZER_ACTIVE_LOW 1
// Set to 1 for the passive piezo that sounded with the tone() test.
// Set to 0 for an active buzzer module.
#define BUZZER_IS_PASSIVE 1
constexpr unsigned int BUZZER_FREQUENCY = 2200;

HardwareSerial fingerprintSerial(2);
Adafruit_Fingerprint finger(&fingerprintSerial);
Adafruit_SH1106G display(128, 64, &Wire, -1);
bool oledReady = false;

const uint8_t BUZZER_ON_LEVEL = BUZZER_ACTIVE_LOW ? LOW : HIGH;
const uint8_t BUZZER_OFF_LEVEL = BUZZER_ACTIVE_LOW ? HIGH : LOW;

void showStatus(const String &title, const String &detail = "") {
  if (!oledReady) return;
  display.clearDisplay();
  display.setTextColor(SH110X_WHITE);
  display.setTextSize(1);
  display.setCursor(0, 0);
  display.println("MECKY ATTENDANCE DEMO");
  display.drawLine(0, 12, 127, 12, SH110X_WHITE);
  display.setTextSize(2);
  display.setCursor(0, 22);
  display.println(title);
  display.setTextSize(1);
  display.setCursor(0, 52);
  display.println(detail);
  display.display();
}

void buzzerBeep(unsigned int onMs) {
#if BUZZER_IS_PASSIVE
  tone(BUZZER_PIN, BUZZER_FREQUENCY);
  delay(onMs);
  noTone(BUZZER_PIN);
  digitalWrite(BUZZER_PIN, BUZZER_OFF_LEVEL);
#else
  digitalWrite(BUZZER_PIN, BUZZER_ON_LEVEL);
  delay(onMs);
  digitalWrite(BUZZER_PIN, BUZZER_OFF_LEVEL);
#endif
}

void beepSuccess() {
  buzzerBeep(1000);
}

void beepDenied() {
  buzzerBeep(1000);
}

void waitForFingerLift() {
  showStatus("LIFT FINGER", "Ready for next scan");
  const unsigned long started = millis();
  while (millis() - started < 10000) {
    if (finger.getImage() == FINGERPRINT_NOFINGER) return;
    delay(80);
  }
}

bool captureIntoBuffer(uint8_t bufferNumber, const String &prompt) {
  showStatus("PLACE FINGER", prompt);
  Serial.println(prompt);
  Serial.println("Place your finger flat on the scanner glass.");

  const unsigned long started = millis();
  unsigned long lastNotice = 0;
  while (millis() - started < 30000) {
    uint8_t result = finger.getImage();
    if (result == FINGERPRINT_OK) {
      Serial.println("Image captured.");
      result = finger.image2Tz(bufferNumber);
      if (result == FINGERPRINT_OK) {
        Serial.println("Fingerprint image processed.");
        return true;
      }
      Serial.printf("Could not process image (sensor code 0x%02X).\n", result);
      showStatus("SCAN ERROR", "Try again");
      return false;
    }

    if (result != FINGERPRINT_NOFINGER && millis() - lastNotice > 1500) {
      Serial.printf("Scanner capture code: 0x%02X\n", result);
      lastNotice = millis();
    }
    delay(60);
  }

  Serial.println("Timed out waiting for a finger.");
  showStatus("TIME OUT", "Try enrollment again");
  return false;
}

bool waitForFingerRemoval() {
  Serial.println("Lift your finger completely off the scanner.");
  showStatus("LIFT FINGER", "Remove it, then wait");
  const unsigned long started = millis();
  while (millis() - started < 20000) {
    if (finger.getImage() == FINGERPRINT_NOFINGER) {
      Serial.println("Finger removed.");
      return true;
    }
    delay(80);
  }
  Serial.println("Timed out waiting for finger removal.");
  return false;
}

bool readEnrollmentId(uint16_t &id) {
  Serial.println();
  Serial.println("Enter an UNUSED fingerprint ID from 1 to 300, then press Enter:");
  showStatus("ENROLL ID", "Type ID in Serial Monitor");
  while (!Serial.available()) delay(10);

  String input = Serial.readStringUntil('\n');
  input.trim();
  if (input.length() == 0) return false;

  char *endPointer = nullptr;
  long parsed = strtol(input.c_str(), &endPointer, 10);
  if (*endPointer != '\0' || parsed < 1 || parsed > 300) {
    Serial.println("Invalid ID. Enter a number from 1 to 300.");
    return false;
  }
  id = static_cast<uint16_t>(parsed);

  // The sensor library doesn't provide a portable occupied-slot list. The
  // operator must choose an unused ID; reusing an ID may replace its template.
  return true;
}

void enrollFingerprint() {
  uint16_t id = 0;
  if (!readEnrollmentId(id)) return;

  Serial.printf("\nStarting enrollment for ID %u.\n", id);
  Serial.println("First scan:");
  showStatus("FIRST SCAN", "Place your finger");
  if (!captureIntoBuffer(1, "First scan")) return;

  if (!waitForFingerRemoval()) {
    showStatus("ENROLL FAILED", "Finger was not lifted");
    return;
  }

  Serial.println("Second scan:");
  showStatus("SECOND SCAN", "Use same finger");
  if (!captureIntoBuffer(2, "Second scan")) return;

  Serial.println("Comparing the two scans...");
  uint8_t result = finger.createModel();
  if (result != FINGERPRINT_OK) {
    Serial.printf("Scans did not match or model failed (sensor code 0x%02X).\n", result);
    showStatus("NO MATCH", "Enroll same finger");
    return;
  }

  result = finger.storeModel(id);
  if (result != FINGERPRINT_OK) {
    Serial.printf("Could not save fingerprint (sensor code 0x%02X).\n", result);
    showStatus("SAVE FAILED", "Check sensor storage");
    return;
  }

  Serial.printf("ENROLLMENT SUCCESSFUL. Fingerprint saved at ID %u.\n", id);
  showStatus("ENROLLED", "ID " + String(id));
  delay(1200);
  showStatus("READY", "Scan enrolled finger");
}

void handleSerialCommand() {
  if (!Serial.available()) return;
  String command = Serial.readStringUntil('\n');
  command.trim();
  command.toUpperCase();
  if (command == "E") {
    enrollFingerprint();
  } else if (command == "?" || command == "HELP") {
    Serial.println("Commands: E = enroll a fingerprint; ? = show this help.");
  } else if (command.length() > 0) {
    Serial.println("Unknown command. Type E to enroll or ? for help.");
  }
}

void scanForAccess() {
  uint8_t result = finger.getImage();
  if (result == FINGERPRINT_NOFINGER) return;
  if (result != FINGERPRINT_OK) {
    Serial.printf("Image capture failed (sensor code 0x%02X).\n", result);
    showStatus("SCAN ERROR", "Try placing finger flat");
    delay(300);
    return;
  }

  Serial.println("Fingerprint image captured. Searching stored fingerprints...");
  result = finger.image2Tz();
  if (result != FINGERPRINT_OK) {
    Serial.printf("Image processing failed (sensor code 0x%02X).\n", result);
    showStatus("READ ERROR", "Try again");
    waitForFingerLift();
    return;
  }

  result = finger.fingerFastSearch();
  if (result == FINGERPRINT_OK) {
    Serial.printf("ACCESS OK - matched fingerprint ID %u (confidence %u).\n",
                  finger.fingerID, finger.confidence);
    showStatus("ACCESS OK", "ID " + String(finger.fingerID));
    beepSuccess();
  } else if (result == FINGERPRINT_NOTFOUND) {
    Serial.println("ACCESS DENIED - fingerprint is not enrolled.");
    showStatus("ACCESS DENIED", "Fingerprint not enrolled");
    beepDenied();
  } else {
    Serial.printf("Fingerprint search failed (sensor code 0x%02X).\n", result);
    showStatus("SEARCH ERROR", "Check scanner");
  }
  waitForFingerLift();
  showStatus("READY", "Scan or type E to enroll");
}

void setup() {
  Serial.begin(115200);
  Serial.setTimeout(30000);

  pinMode(BUZZER_PIN, OUTPUT);
#if BUZZER_IS_PASSIVE
  noTone(BUZZER_PIN);
  digitalWrite(BUZZER_PIN, BUZZER_OFF_LEVEL);
#else
  digitalWrite(BUZZER_PIN, BUZZER_OFF_LEVEL);
#endif

  Wire.begin(OLED_SDA_PIN, OLED_SCL_PIN);
  oledReady = display.begin(OLED_ADDRESS, true);
  if (oledReady) {
    showStatus("STARTING", "Fingerprint demo");
  } else {
    Serial.println("OLED not found. Check SDA/SCL, power, address, and SH1106 driver.");
  }

  fingerprintSerial.begin(FINGER_BAUD, SERIAL_8N1, FINGER_RX_PIN, FINGER_TX_PIN);
  finger.begin(FINGER_BAUD);
  if (!finger.verifyPassword()) {
    Serial.println("Fingerprint reader did not respond. Check power and crossed TX/RX wiring.");
    showStatus("SENSOR ERROR", "Check UART wiring");
    while (true) {
      delay(1000);
    }
  }

  finger.getTemplateCount();
  Serial.println();
  Serial.println("Fingerprint + OLED + buzzer demo ready.");
  Serial.printf("Templates currently stored: %u\n", finger.templateCount);
  Serial.println("When enrolling, choose an ID you know is unused; reusing an ID can replace that stored print.");
  Serial.println("ACCESS OK or ACCESS DENIED: one 1-second buzzer sound.");
  Serial.println("Type E and press Enter to enroll. Use Serial Monitor at 115200 baud, Newline.");
  showStatus("READY", "Scan or type E to enroll");
}

void loop() {
  handleSerialCommand();
  scanForAccess();
  delay(50);
}
