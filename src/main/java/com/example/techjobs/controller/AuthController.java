package com.example.techjobs.controller;
import com.example.techjobs.entity.*;
import com.example.techjobs.exception.ApiException;
import com.example.techjobs.repository.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
  public record RegisterReq(@NotBlank String name,@NotBlank @Email String email,@NotBlank @Size(min=6,message="min 6 characters") String password,
    @NotBlank String jobTitle,@NotNull @Min(0) @Max(50) Integer experienceYears,List<String> skills,String qualification,
    String preferredLocation,Boolean remotePreference, String cvType, String cvData){}
  public record LoginReq(@NotBlank String email,@NotBlank String password, Boolean rememberMe){}
  private final UserRepository users; private final ProfileRepository profiles; private final PasswordEncoder enc; private final AuthenticationManager am;
  public AuthController(UserRepository u,ProfileRepository p,PasswordEncoder e,AuthenticationManager a){ users=u;profiles=p;enc=e;am=a; }

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterReq r){
    if(users.findByEmail(r.email()).isPresent()) throw new ApiException(HttpStatus.CONFLICT,"Email already registered");
    AppUser u=new AppUser(); u.name=r.name(); u.email=r.email(); u.password=enc.encode(r.password()); users.save(u);
    UserProfile p=new UserProfile(); p.userId=u.id; p.jobTitle=r.jobTitle(); p.experienceYears=r.experienceYears();
    p.skills=r.skills()==null?"":String.join(", ",r.skills()); p.qualification=r.qualification();
    p.preferredLocation=r.preferredLocation(); p.remotePreference=Boolean.TRUE.equals(r.remotePreference());
    p.cvType=r.cvType(); p.cvData=r.cvData();
    profiles.save(p);
    return ResponseEntity.status(201).body(Map.of("message","Registration successful"));
  }
  @PostMapping("/login")
  public Map<String,Object> login(@Valid @RequestBody LoginReq r,HttpServletRequest req,HttpServletResponse res){
    Authentication a=am.authenticate(new UsernamePasswordAuthenticationToken(r.email(),r.password()));
    SecurityContext ctx=SecurityContextHolder.createEmptyContext(); ctx.setAuthentication(a); SecurityContextHolder.setContext(ctx);
    new HttpSessionSecurityContextRepository().saveContext(ctx,req,res);   // stores the login in the session cookie

    if (Boolean.TRUE.equals(r.rememberMe())) {
      req.getSession().setMaxInactiveInterval(30 * 24 * 60 * 60); // 30 days on server
      Cookie c = new Cookie("JSESSIONID", req.getSession().getId());
      c.setMaxAge(30 * 24 * 60 * 60); // 30 days in browser
      c.setPath("/"); c.setHttpOnly(true); res.addCookie(c);
    }

    return me(a);
  }
  @PostMapping("/logout")
  public Map<String,String> logout(HttpServletRequest req){ HttpSession s=req.getSession(false); if(s!=null) s.invalidate(); SecurityContextHolder.clearContext(); return Map.of("message","Logged out"); }
  @GetMapping("/me")
  public Map<String,Object> me(Principal p){
    AppUser u=users.findByEmail(p.getName()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Not logged in"));
    return Map.of("id",u.id,"name",u.name,"email",u.email,"role",u.role); }
}
