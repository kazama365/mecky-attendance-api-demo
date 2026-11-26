package com.mecky.attendance.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.List;
@Component public class JwtFilter extends OncePerRequestFilter {
 private final JwtService jwt;
 private final com.mecky.attendance.repo.UserRepository users;
 public JwtFilter(JwtService jwt,com.mecky.attendance.repo.UserRepository users){this.jwt=jwt;this.users=users;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String h=req.getHeader("Authorization");
  if(h!=null&&h.startsWith("Bearer "))try{
   Claims c=jwt.parse(h.substring(7));
   var user=users.findByUsername(c.getSubject()).orElseThrow();
   Number version=c.get("passwordVersion",Number.class);
   int tokenVersion=version==null?0:version.intValue();
   if(!user.active||user.passwordVersion!=tokenVersion||!user.role.equals(c.get("role",String.class)))throw new IllegalArgumentException("Session expired");
   var a=new UsernamePasswordAuthenticationToken(user.username,null,List.of(new SimpleGrantedAuthority("ROLE_"+user.role)));
   SecurityContextHolder.getContext().setAuthentication(a);
  }catch(Exception ignored){SecurityContextHolder.clearContext();}
  chain.doFilter(req,res);
 }
}
