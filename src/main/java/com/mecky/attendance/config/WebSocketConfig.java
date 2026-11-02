package com.mecky.attendance.config;
import com.mecky.attendance.domain.AppUser;
import com.mecky.attendance.repo.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Configuration @EnableWebSocket public class WebSocketConfig implements WebSocketConfigurer {
 private final EventSocket socket;
 public WebSocketConfig(EventSocket s){socket=s;}
 public void registerWebSocketHandlers(WebSocketHandlerRegistry r){r.addHandler(socket,"/ws").setAllowedOriginPatterns("*");}
 @Component public static class EventSocket extends TextWebSocketHandler {
  private final UserRepository users;
  private final Map<WebSocketSession,SessionIdentity> sessions=new ConcurrentHashMap<>();
  public EventSocket(UserRepository users){this.users=users;}
  public void afterConnectionEstablished(WebSocketSession s){
   Principal principal=s.getPrincipal();
   AppUser user=principal==null?null:users.findByUsername(principal.getName()).filter(u->u.active).orElse(null);
   if(user==null||(!"ADMIN".equals(user.role)&&!"EMPLOYEE".equals(user.role))){try{s.close(CloseStatus.NOT_ACCEPTABLE);}catch(Exception ignored){}return;}
   sessions.put(s,new SessionIdentity(user.role,user.employeeId));
  }
  public void afterConnectionClosed(WebSocketSession s,CloseStatus c){sessions.remove(s);}
  public void publish(String json){publishToAdmins(json);}
  public void publishToAdmins(String json){for(var entry:sessions.entrySet())if("ADMIN".equals(entry.getValue().role))send(entry.getKey(),json);}
  public void publishAttendance(String employeeId,String json){for(var entry:sessions.entrySet()){SessionIdentity identity=entry.getValue();if("ADMIN".equals(identity.role)||("EMPLOYEE".equals(identity.role)&&employeeId.equals(identity.employeeId)))send(entry.getKey(),json);}}
  private void send(WebSocketSession s,String json){try{if(s.isOpen())s.sendMessage(new TextMessage(json));}catch(Exception ignored){sessions.remove(s);}}
  private record SessionIdentity(String role,String employeeId){}
 }
}