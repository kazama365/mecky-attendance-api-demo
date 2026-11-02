package com.mecky.attendance.service;
import com.mecky.attendance.repo.DeviceRepository;
import com.mecky.attendance.config.WebSocketConfig.EventSocket;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.*;
@Component public class DeviceOfflineJob {
 private final DeviceRepository devices;private final EventSocket ws;
 public DeviceOfflineJob(DeviceRepository d,EventSocket ws){devices=d;this.ws=ws;}
 @Scheduled(fixedRate=10000) public void markOffline(){LocalDateTime cutoff=LocalDateTime.now(ZoneId.of("Asia/Manila")).minusSeconds(30);for(var d:devices.findAll())if(d.lastSeen==null||d.lastSeen.isBefore(cutoff)){if(!"OFFLINE".equals(d.status)){d.status="OFFLINE";devices.save(d);ws.publish("{\"type\":\"device.status\",\"name\":\""+d.name+"\",\"status\":\"OFFLINE\"}");}}}
}
