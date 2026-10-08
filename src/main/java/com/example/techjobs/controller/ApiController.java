package com.example.techjobs.controller;
import com.example.techjobs.entity.*;
import com.example.techjobs.exception.ApiException;
import com.example.techjobs.repository.*;
import com.example.techjobs.service.*;
import com.example.techjobs.service.RecommendationService.Rec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;
/** Profile, jobs, applications and admin endpoints. The user always comes from the session, never from a request parameter. */
@RestController @RequestMapping("/api")
public class ApiController {
  public record ProfileDto(@NotBlank String jobTitle,@NotNull @Min(0) @Max(50) Integer experienceYears,List<String> skills,
    String qualification,String preferredLocation,Boolean remotePreference){}
  public record ApplyReq(@NotNull Long jobId){}
  private final UserRepository users; private final ProfileRepository profiles; private final JobRepository jobs;
  private final ApplicationRepository apps; private final RecommendationService rec; private final JobService jobService;
  public ApiController(UserRepository u,ProfileRepository p,JobRepository j,ApplicationRepository a,RecommendationService r,JobService s){
    users=u;profiles=p;jobs=j;apps=a;rec=r;jobService=s; }

  private AppUser me(Principal p){ return users.findByEmail(p.getName()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Not logged in")); }
  private UserProfile profileOf(Principal p){ return profiles.findByUserId(me(p).id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Profile not found")); }
  private Job job(Long id){ return jobs.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Job not found")); }

  // ----- profile -----
  @GetMapping("/profile") public ProfileDto profile(Principal p){
    UserProfile x=profileOf(p);
    return new ProfileDto(x.jobTitle,x.experienceYears,RecommendationService.split(x.skills),x.qualification,x.preferredLocation,x.remotePreference); }
  @PutMapping("/profile") public ProfileDto update(Principal p,@Valid @RequestBody ProfileDto d){
    UserProfile x=profileOf(p); x.jobTitle=d.jobTitle(); x.experienceYears=d.experienceYears();
    x.skills=d.skills()==null?"":String.join(", ",d.skills()); x.qualification=d.qualification();
    x.preferredLocation=d.preferredLocation(); x.remotePreference=Boolean.TRUE.equals(d.remotePreference()); profiles.save(x); return d; }

  // ----- jobs -----
  @GetMapping("/jobs/recommended") public List<Rec> recommended(Principal p){ return rec.recommend(profileOf(p),jobs.findAll()); }
  @GetMapping("/jobs/{id}") public Map<String,Object> details(Principal p,@PathVariable Long id){
    Job j=job(id); return Map.of("job",j,"match",rec.score(profileOf(p),j)); }
  @PostMapping({"/jobs/sync","/admin/jobs/sync"}) public Map<String,Integer> sync(){ return jobService.sync(); }

  // ----- applications -----
  @PostMapping("/applications") public ResponseEntity<?> apply(Principal p,@Valid @RequestBody ApplyReq r){
    AppUser u=me(p); job(r.jobId());
    if(apps.existsByUserIdAndJobId(u.id,r.jobId())) throw new ApiException(HttpStatus.CONFLICT,"You already applied to this job");
    JobApplication a=new JobApplication(); a.userId=u.id; a.jobId=r.jobId(); apps.save(a);
    return ResponseEntity.status(201).body(Map.of("message","Application submitted successfully","status",a.status)); }
  @GetMapping("/applications") public List<Map<String,Object>> myApps(Principal p){
    List<Map<String,Object>> out=new ArrayList<>();
    for(JobApplication a:apps.findByUserId(me(p).id)){ Job j=jobs.findById(a.jobId).orElse(null);
      out.add(Map.of("id",a.id,"jobId",a.jobId,"title",j==null?"(removed)":j.title,"company",j==null?"":j.company,"appliedAt",a.appliedAt.toString(),"status",a.status)); }
    return out; }

  // ----- admin (ROLE_ADMIN enforced in SecurityConfig) -----
  @GetMapping("/admin/jobs") public List<Job> allJobs(){ return jobs.findAll(); }
  @PostMapping("/admin/jobs") public ResponseEntity<Job> add(@Valid @RequestBody Job b){
    b.id=null; b.source="MANUAL"; if(b.externalJobId==null) b.externalJobId=UUID.randomUUID().toString(); return ResponseEntity.status(201).body(jobs.save(b)); }
  @PutMapping("/admin/jobs/{id}") public Job edit(@PathVariable Long id,@Valid @RequestBody Job b){
    Job o=job(id); b.id=id; b.externalJobId=o.externalJobId; b.source=o.source; b.createdAt=o.createdAt; return jobs.save(b); }
  @DeleteMapping("/admin/jobs/{id}") public Map<String,String> del(@PathVariable Long id){
    job(id); apps.deleteAll(apps.findAll().stream().filter(a->a.jobId.equals(id)).toList()); jobs.deleteById(id); return Map.of("message","Job deleted"); }
  @GetMapping("/admin/users") public List<AppUser> allUsers(){ return users.findAll(); }
  @GetMapping("/admin/applications") public List<JobApplication> allApps(){ return apps.findAll(); }

  /** Detailed application list for admin — includes full applicant profile + job info */
  @GetMapping("/admin/applications/detailed")
  public List<Map<String,Object>> allAppsDetailed(){
    List<Map<String,Object>> out = new ArrayList<>();
    for(JobApplication a : apps.findAll()){
      Map<String,Object> row = new LinkedHashMap<>();
      row.put("id", a.id);
      row.put("appliedAt", a.appliedAt.toString());
      row.put("status", a.status);

      // Applicant info
      AppUser u = users.findById(a.userId).orElse(null);
      if(u != null){
        row.put("applicantName", u.name);
        row.put("applicantEmail", u.email);
        // Profile
        UserProfile p = profiles.findByUserId(u.id).orElse(null);
        if(p != null){
          row.put("applicantJobTitle", p.jobTitle);
          row.put("applicantExperience", p.experienceYears);
          row.put("applicantSkills", RecommendationService.split(p.skills));
          row.put("applicantQualification", p.qualification);
          row.put("applicantLocation", p.preferredLocation);
          row.put("applicantRemote", p.remotePreference);
        }
      } else {
        row.put("applicantName", "(deleted)");
        row.put("applicantEmail", "");
      }

      // Job info
      Job j = jobs.findById(a.jobId).orElse(null);
      if(j != null){
        row.put("jobId", j.id);
        row.put("jobTitle", j.title);
        row.put("jobCompany", j.company);
        row.put("jobLocation", j.location);
        row.put("jobSkills", j.requiredSkills);
        row.put("jobExperience", j.requiredExperience);
      } else {
        row.put("jobId", a.jobId);
        row.put("jobTitle", "(deleted)");
        row.put("jobCompany", "");
      }
      out.add(row);
    }
    return out;
  }
}
