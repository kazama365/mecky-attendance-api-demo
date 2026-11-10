package com.mecky.attendance.api;
import com.mecky.attendance.api.ApiDtos.*;
import com.mecky.attendance.repo.UserRepository;
import com.mecky.attendance.config.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final UserRepository users;private final PasswordEncoder passwords;private final JwtService jwt;
 private final ConcurrentHashMap<String,AttemptWindow> failedAttempts=new ConcurrentHashMap<>();
 public AuthController(UserRepository u,PasswordEncoder p,JwtService j){users=u;passwords=p;jwt=j;}
 @PostMapping("/login") public ResponseEntity<?> login(@Valid @RequestBody LoginRequest r,HttpServletRequest request){
  String key=request.getRemoteAddr()+":"+r.username().toLowerCase(java.util.Locale.ROOT);Instant now=Instant.now();
  if(isLocked(key,now))return ResponseEntity.status(429).body(java.util.Map.of("error","Too many failed login attempts. Try again in 10 minutes."));
  var u=users.findByUsernameIgnoreCase(r.username().trim()).filter(x->passwords.matches(r.password(),x.passwordHash));
  if(u.isPresent()){
   failedAttempts.remove(key);var x=u.get();
   if(!x.active)return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("error","Your account is waiting for admin approval."));
   return ResponseEntity.ok(new LoginResponse(jwt.issue(x.username,x.role),x.role,x.username));
  }
  registerFailure(key,now);return ResponseEntity.status(401).body(java.util.Map.of("error","Invalid username or password"));
 }
 @PostMapping("/logout") public void logout(){}
 private boolean isLocked(String key,Instant now){AttemptWindow w=failedAttempts.get(key);if(w==null)return false;if(w.lockedUntil!=null&&now.isBefore(w.lockedUntil))return true;if(w.lockedUntil!=null&&!now.isBefore(w.lockedUntil)){failedAttempts.remove(key,w);return false;}if(now.isAfter(w.started.plusSeconds(900))){failedAttempts.remove(key,w);return false;}return false;}
 private void registerFailure(String key,Instant now){failedAttempts.compute(key,(k,w)->{if(w==null||now.isAfter(w.started.plusSeconds(900)))w=new AttemptWindow(now);w.failures++;if(w.failures>=5)w.lockedUntil=now.plusSeconds(600);return w;});}
 private static final class AttemptWindow{final Instant started;int failures;Instant lockedUntil;AttemptWindow(Instant started){this.started=started;}}
}
