package com.mecky.attendance.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
@Service public class AuditService {
 private final JdbcTemplate jdbc;private final ObjectMapper json;
 public AuditService(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
 public void record(String actor,String action,String entity,String id,Object before,Object after){
  try{jdbc.update("INSERT INTO audit_logs(actor_username,action,entity_name,entity_id,before_json,after_json) VALUES(?,?,?,?,CAST(? AS JSON),CAST(? AS JSON))",actor,action,entity,id,json.writeValueAsString(before),json.writeValueAsString(after));}
  catch(JsonProcessingException e){throw new IllegalArgumentException("Unable to serialize audit entry",e);}
 }
}
