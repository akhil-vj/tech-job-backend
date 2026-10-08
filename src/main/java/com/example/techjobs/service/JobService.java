package com.example.techjobs.service;
import com.example.techjobs.entity.Job;
import com.example.techjobs.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.regex.*;
/** Fetch -> normalize (DTO to Job) -> de-duplicate on externalJobId+source -> save. */
@Service
public class JobService {
  private static final List<String> KEYWORDS=List.of("Java","Spring Boot","SQL","REST API","Docker","Kubernetes","Python","JavaScript",
    "TypeScript","React","Node.js","AWS","Azure","Git","Microservices","Kafka","MongoDB","PostgreSQL","MySQL","Angular","Linux","CI/CD");
  private final JobRepository repo; private final ExternalJobApiService api;
  public JobService(JobRepository r,ExternalJobApiService a){ repo=r; api=a; }

  @Transactional
  public Map<String,Integer> sync(){
    int added=0,updated=0;
    for(var d:api.fetch()){
      Job j=repo.findByExternalJobIdAndSource(d.id(),d.source()).orElse(null);
      boolean isNew=j==null; if(isNew) j=new Job();
      j.externalJobId=d.id(); j.source=d.source(); j.title=d.title(); j.company=d.company(); j.location=d.location();
      String text=d.description()==null?"":d.description().replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").trim();
      j.description=text.length()>4000?text.substring(0,4000):text;
      j.salary=d.salary(); j.applicationUrl=d.url(); j.employmentType=d.type(); j.postedDate=d.posted();
      String hay=d.title()+" "+text+" "+String.join(" ",d.tags());
      List<String> found=new ArrayList<>();
      for(String k:KEYWORDS) if(Pattern.compile("(?i)(?<![a-z0-9])"+Pattern.quote(k)+"(?![a-z0-9])").matcher(hay).find()) found.add(k);
      j.requiredSkills=String.join(", ",found);
      Matcher m=Pattern.compile("(\\d{1,2})\\+?\\s*(?:years|yrs)").matcher(text);
      j.requiredExperience=m.find()?Math.min(Integer.parseInt(m.group(1)),15):0;
      repo.save(j); if(isNew) added++; else updated++;
    }
    return Map.of("added",added,"updated",updated);
  }
}
