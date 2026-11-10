#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <LittleFS.h>
#include <Adafruit_Fingerprint.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <time.h>
const char* WIFI_SSID = "CHANGE_ME";
const char* WIFI_PASSWORD = "CHANGE_ME";
const char* SERVER_URL = "http://192.168.1.10:8080";
const char* DEVICE_API_KEY = "CHANGE_ME";
const char* QUEUE_FILE = "/scan-queue.json";
constexpr int FP_RX=16, FP_TX=17, BUZZER=25, LED_OK=26, LED_ERROR=27;
constexpr unsigned long HEARTBEAT_MS=10000, COMMAND_POLL_MS=3000;
HardwareSerial fpSerial(2);
Adafruit_Fingerprint finger(&fpSerial);
Adafruit_SSD1306 display(128,64,&Wire,-1);
unsigned long lastHeartbeat=0,lastCommandPoll=0;
bool displayReady=false;
String isoNow(){struct tm t;if(!getLocalTime(&t,1000))return "";char b[24];strftime(b,sizeof(b),"%Y-%m-%dT%H:%M:%S",&t);return String(b);}
String makeUuid(){char b[37];uint64_t mac=ESP.getEfuseMac();snprintf(b,sizeof(b),"%08lx-%04x-4%03x-%04x-%012llx",(unsigned long)esp_random(),(unsigned)(esp_random()&0xffff),(unsigned)(esp_random()&0xfff),(unsigned)(esp_random()&0x3fff)|0x8000,(unsigned long long)(mac&0xffffffffffffULL));return String(b);}
void show(const String& a,const String& b=""){if(!displayReady)return;display.clearDisplay();display.setTextColor(SSD1306_WHITE);display.setTextSize(1);display.setCursor(0,12);display.println("MECKY ATTENDANCE");display.drawLine(0,22,127,22,SSD1306_WHITE);display.setTextSize(2);display.setCursor(0,30);display.println(a);display.setTextSize(1);display.setCursor(0,54);display.println(b);display.display();}
void beep(int count=1,int duration=120){for(int i=0;i<count;i++){tone(BUZZER,2200,duration);delay(duration+60);}}
bool serverPost(const String& path,const String& body,String& response){if(WiFi.status()!=WL_CONNECTED)return false;HTTPClient http;http.setTimeout(2500);http.begin(String(SERVER_URL)+path);http.addHeader("Content-Type","application/json");http.addHeader("X-Device-Key",DEVICE_API_KEY);int code=http.POST(body);if(code>0)response=http.getString();http.end();return code>=200&&code<300;}
bool serverGet(const String& path,String& response){if(WiFi.status()!=WL_CONNECTED)return false;HTTPClient http;http.setTimeout(2500);http.begin(String(SERVER_URL)+path);http.addHeader("X-Device-Key",DEVICE_API_KEY);int code=http.GET();if(code>0)response=http.getString();http.end();return code>=200&&code<300;}
bool loadQueue(JsonDocument& doc){if(!LittleFS.exists(QUEUE_FILE)){doc["scans"].to<JsonArray>();return true;}File f=LittleFS.open(QUEUE_FILE,"r");if(!f)return false;auto err=deserializeJson(doc,f);f.close();return !err;}
void saveQueue(JsonDocument& doc){File f=LittleFS.open(QUEUE_FILE,"w");if(f){serializeJson(doc,f);f.close();}}
void syncQueue(){JsonDocument queue;if(!loadQueue(queue))return;JsonArray scans=queue["scans"].as<JsonArray>();if(scans.isNull()||scans.size()==0)return;String response,body;serializeJson(queue,body);if(serverPost("/api/device/scan",body,response)){queue.clear();queue["scans"].to<JsonArray>();saveQueue(queue);}}
bool queueScan(const String& uuid,uint16_t slot,const String& timestamp){JsonDocument queue;if(!loadQueue(queue))return false;JsonObject s=queue["scans"].as<JsonArray>().createNestedObject();s["scanUuid"]=uuid;s["slotId"]=slot;s["timestamp"]=timestamp;saveQueue(queue);return true;}
void recordScan(uint16_t slot){
 String ts=isoNow();if(ts.isEmpty()){show("CLOCK ERROR","Waiting for NTP");beep(3,90);return;}String uuid=makeUuid();
 JsonDocument body;JsonObject scan=body["scans"].to<JsonArray>().createNestedObject();scan["scanUuid"]=uuid;scan["slotId"]=slot;scan["timestamp"]=ts;String payload,response;serializeJson(body,payload);
 if(WiFi.status()!=WL_CONNECTED){if(!queueScan(uuid,slot,ts)){show("QUEUE ERROR","Check storage");beep(3,90);return;}show("OFFLINE MODE","Scan saved on device");beep(2,80);digitalWrite(LED_ERROR,HIGH);delay(400);digitalWrite(LED_ERROR,LOW);return;}
 if(serverPost("/api/device/scan",payload,response)){JsonDocument result;if(deserializeJson(result,response)==DeserializationError::Ok){JsonObject r=result["results"][0];String state=r["status"]|"";String name=r["employeeName"]|"";if(state=="TIME_IN"){show("TIME IN",name);digitalWrite(LED_OK,HIGH);beep();delay(700);digitalWrite(LED_OK,LOW);}else if(state=="TIME_OUT"){show("TIME OUT",name);digitalWrite(LED_OK,HIGH);beep(2,90);delay(700);digitalWrite(LED_OK,LOW);}else if(state=="DUPLICATE"){show("ALREADY SCANNED",name);beep(2,90);}else if(state=="UNREGISTERED"){show("NOT REGISTERED","Contact administrator");beep(3,90);}else{show("SCAN REJECTED",name);beep(2,100);}}syncQueue();}
 else{if(!queueScan(uuid,slot,ts)){show("QUEUE ERROR","Check storage");beep(3,90);}else{show("OFFLINE MODE","Queued for upload");beep(2,80);}}
}
void heartbeat(){StaticJsonDocument<128>d;d["device"]="ESP32-01";d["firmware"]="1.0.0";String body,response;serializeJson(d,body);if(serverPost("/api/device/heartbeat",body,response)){digitalWrite(LED_OK,HIGH);delay(25);digitalWrite(LED_OK,LOW);}}
void postEnrollResult(long id,bool success,int slot,const String& message){StaticJsonDocument<256>d;d["commandId"]=id;d["success"]=success;d["slotId"]=slot;d["message"]=message;String body,response;serializeJson(d,body);serverPost("/api/device/enroll-result",body,response);}
bool waitForFinger(){show("ENROLL","Place finger");for(int i=0;i<100;i++){uint8_t p=finger.getImage();if(p==FINGERPRINT_OK)return true;if(WiFi.status()!=WL_CONNECTED)WiFi.reconnect();delay(100);}return false;}
bool waitFingerRemoved(){for(int i=0;i<100;i++){if(finger.getImage()==FINGERPRINT_NOFINGER)return true;delay(100);}return false;}
void enroll(long commandId){finger.getTemplateCount();int slot=finger.templateCount+1;bool ok=waitForFinger()&&finger.image2Tz(1)==FINGERPRINT_OK&&waitFingerRemoved()&&waitForFinger()&&finger.image2Tz(2)==FINGERPRINT_OK&&finger.createModel()==FINGERPRINT_OK&&finger.storeModel(slot)==FINGERPRINT_OK;postEnrollResult(commandId,ok,ok?slot:0,ok?"Fingerprint enrolled":"Enrollment capture or storage failed");if(ok){show("ENROLLED","Slot "+String(slot));digitalWrite(LED_OK,HIGH);beep(2,100);delay(700);digitalWrite(LED_OK,LOW);}else{show("ENROLL FAILED","Try again");beep(3,100);}}
void pollCommand(){String response;if(!serverGet("/api/device/command",response))return;StaticJsonDocument<512>d;if(deserializeJson(d,response)!=DeserializationError::Ok)return;if(String(d["type"]|"")=="ENROLL")enroll(d["id"]|0L);}
void setup(){Serial.begin(115200);pinMode(BUZZER,OUTPUT);pinMode(LED_OK,OUTPUT);pinMode(LED_ERROR,OUTPUT);LittleFS.begin(true);Wire.begin(21,22);displayReady=display.begin(SSD1306_SWITCHCAPVCC,0x3C);show("STARTING","Connecting Wi-Fi");fpSerial.begin(57600,SERIAL_8N1,FP_RX,FP_TX);finger.begin(57600);if(!finger.verifyPassword()){show("SENSOR ERROR","Check wiring");while(true){beep(2,100);delay(1800);}}WiFi.mode(WIFI_STA);WiFi.setAutoReconnect(true);WiFi.begin(WIFI_SSID,WIFI_PASSWORD);configTzTime("Asia/Manila","pool.ntp.org","time.google.com");unsigned long start=millis();while(WiFi.status()!=WL_CONNECTED&&millis()-start<15000)delay(300);show(WiFi.status()==WL_CONNECTED?"READY":"OFFLINE MODE","Scan fingerprint");syncQueue();}
void loop(){if(WiFi.status()!=WL_CONNECTED)WiFi.reconnect();else{if(millis()-lastHeartbeat>HEARTBEAT_MS){lastHeartbeat=millis();heartbeat();}if(millis()-lastCommandPoll>COMMAND_POLL_MS){lastCommandPoll=millis();pollCommand();syncQueue();}}uint8_t p=finger.getImage();if(p==FINGERPRINT_OK){p=finger.image2Tz();if(p==FINGERPRINT_OK){p=finger.fingerFastSearch();if(p==FINGERPRINT_OK)recordScan(finger.fingerID);else if(p==FINGERPRINT_NOTFOUND)recordScan(0);}}delay(40);}
