package com.example.techjobs.exception;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
/** One consistent error shape: {status, message, timestamp}. */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private ResponseEntity<Map<String,Object>> err(HttpStatus s,String m){
    return ResponseEntity.status(s).body(Map.of("status",s.value(),"message",m,"timestamp",LocalDateTime.now().toString())); }
  @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e){ return err(e.status,e.getMessage()); }
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> val(MethodArgumentNotValidException e){
    var f=e.getBindingResult().getFieldError(); return err(HttpStatus.BAD_REQUEST,f==null?"Invalid data":f.getField()+": "+f.getDefaultMessage()); }
  @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<?> bad(Exception e){ return err(HttpStatus.BAD_REQUEST,"Malformed request body"); }
  @ExceptionHandler(BadCredentialsException.class) ResponseEntity<?> cred(Exception e){ return err(HttpStatus.UNAUTHORIZED,"Invalid email or password"); }
  @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> dup(Exception e){ return err(HttpStatus.CONFLICT,"Duplicate or conflicting data"); }
  @ExceptionHandler(Exception.class) ResponseEntity<?> any(Exception e){ return err(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected server error"); }
}
