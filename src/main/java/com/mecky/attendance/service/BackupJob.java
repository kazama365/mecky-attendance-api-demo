package com.mecky.attendance.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import java.nio.file.*;
import java.time.*;
import java.util.*;
@Component @Profile("!demo") public class BackupJob {
 @Value("${app.backup.directory}") String directory;
 @Scheduled(cron="0 0 2 * * *",zone="Asia/Manila") public void backup(){
  String exe=System.getenv().getOrDefault("MYSQLDUMP_PATH","mysqldump");String user=System.getenv().getOrDefault("BACKUP_DB_USERNAME","mecky_backup");String pass=System.getenv().getOrDefault("BACKUP_DB_PASSWORD","");
  try{Path dir=Paths.get(directory);Files.createDirectories(dir);Path file=dir.resolve("mecky-"+LocalDate.now(ZoneId.of("Asia/Manila"))+".sql");
   ProcessBuilder builder=new ProcessBuilder(exe,"--user="+user,"--single-transaction","--no-tablespaces","--routines","--triggers","mecky_attendance").redirectOutput(file.toFile());builder.environment().put("MYSQL_PWD",pass);Process p=builder.start();if(p.waitFor()!=0){Files.deleteIfExists(file);throw new IllegalStateException("mysqldump failed");}
   try(var files=Files.list(dir)){files.filter(x->x.getFileName().toString().startsWith("mecky-")).sorted(Comparator.comparing((Path x)->x.getFileName().toString()).reversed()).skip(30).forEach(x->{try{Files.deleteIfExists(x);}catch(Exception ignored){}});}
  }catch(Exception ex){System.err.println("Database backup failed: "+ex.getMessage());}
 }
}
