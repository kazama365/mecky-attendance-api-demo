package com.mecky.attendance.api;
import com.mecky.attendance.api.ApiDtos.*;
import com.mecky.attendance.domain.*;
import com.mecky.attendance.repo.*;
import com.mecky.attendance.service.AttendanceService;
import com.mecky.attendance.config.WebSocketConfig.EventSocket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.validation.Valid;
import java.util.*;
@RestController @RequestMapping("/api/device") public class DeviceController {
 private final AttendanceService attendance;private final EmployeeRepository employees;private final DeviceCommandRepository commands;private final DeviceRepository devices;private final EventSocket ws;private final String key;
 public DeviceController(AttendanceService a,EmployeeRepository e,DeviceCommandRepository c,DeviceRepository d,EventSocket ws,@Value("${app.device.api-key}") String key){attendance=a;employees=e;commands=c;devices=d;this.ws=ws;this.key=key;}
 private void auth(String value){if(key.isBlank()||!java.security.MessageDigest.isEqual(key.getBytes(),(value==null?"":value).getBytes()))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid device key");}
 @PostMapping("/scan") public Object scan(@RequestHeader(value="X-Device-Key",required=false) String k,@Valid @RequestBody ScanBatch body){auth(k);return Map.of("results",body.scans().stream().map(attendance::process).toList());}
 @PostMapping("/heartbeat") public Object heartbeat(@RequestHeader(value="X-Device-Key",required=false) String k,@RequestBody(required=false) Map<String,String> body){
  auth(k);String name=body==null?"ESP32-01":body.getOrDefault("device","ESP32-01");String hash=sha256(key);Device d=devices.findByName(name).orElseGet(()->new Device(name,hash));d.apiKeyHash=hash;d.status="ONLINE";d.lastSeen=java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Manila"));devices.save(d);ws.publish("{\"type\":\"device.status\",\"name\":\""+name+"\",\"status\":\"ONLINE\"}");return Map.of("status","ONLINE","lastSeen",d.lastSeen);
 }
 @GetMapping("/status") @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')") public Object status(){
  return devices.findAll().stream().map(d->Map.of("name",d.name,"status",d.status,"lastSeen",d.lastSeen==null?"":d.lastSeen.toString())).toList();
 }
 private static String sha256(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @GetMapping("/command") public Object command(@RequestHeader(value="X-Device-Key",required=false) String k){auth(k);return commands.findFirstByStatusOrderByCreatedAtAsc("PENDING").map(c->{c.status="SENT";commands.save(c);return Map.of("id",c.id,"type",c.type,"employeeId",c.employeeId);}).orElseGet(()->Map.of("type","NONE"));}
 @PostMapping("/enroll-result") public Object result(@RequestHeader(value="X-Device-Key",required=false) String k,@Valid @RequestBody EnrollmentResult r){
  auth(k);DeviceCommand c=commands.findById(r.commandId()).orElseThrow();c.status=r.success()?"SUCCESS":"FAILED";c.resultMessage=r.message();commands.save(c);
  if(r.success()){Employee e=employees.findById(c.employeeId).orElseThrow();e.sensorSlotId=r.slotId();employees.save(e);}
  return Map.of("accepted",true);
 }
}
