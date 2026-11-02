package com.mecky.attendance.api;
import com.mecky.attendance.repo.DeviceRepository;
import org.springframework.web.bind.annotation.*;
import java.time.*;
@RestController @RequestMapping("/api/devices") public class DeviceStatusController {
 private final DeviceRepository devices;
 public DeviceStatusController(DeviceRepository d){devices=d;}
 @GetMapping("/status") public Object status(){return devices.findAll().stream().map(d->java.util.Map.of("name",d.name,"status",d.lastSeen!=null&&d.lastSeen.isAfter(LocalDateTime.now(ZoneId.of("Asia/Manila")).minusSeconds(30))?"ONLINE":"OFFLINE","lastSeen",d.lastSeen==null?"":d.lastSeen.toString())).toList();}
}
