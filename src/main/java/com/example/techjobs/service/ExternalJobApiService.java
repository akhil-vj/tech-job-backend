package com.example.techjobs.service;
import com.example.techjobs.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;
/** The ONLY class that talks to the external provider. To add another provider, write another fetch() that returns ExternalJobDTOs. */
@Service
public class ExternalJobApiService {
  public record ExternalJobDTO(String id,String source,String title,String company,String location,String description,
                               String salary,List<String> tags,String url,String type,String posted){}
  @Value("${job.api.url:}") private String url;
  @Value("${job.api.key:}") private String key;

  public List<ExternalJobDTO> fetch(){
    if(url.isBlank()) throw new ApiException(HttpStatus.BAD_GATEWAY,"job.api.url is not configured");
    try{
      RestClient.RequestHeadersSpec<?> spec=RestClient.create().get().uri(url);
      if(!key.isBlank()) spec=spec.header("Authorization","Bearer "+key);
      JsonNode root=spec.retrieve().body(JsonNode.class);
      List<ExternalJobDTO> out=new ArrayList<>();
      for(JsonNode n:root.path("jobs")){            // Remotive response format
        List<String> tags=new ArrayList<>(); n.path("tags").forEach(t->tags.add(t.asText()));
        out.add(new ExternalJobDTO(n.path("id").asText(),"REMOTIVE",n.path("title").asText(),n.path("company_name").asText(),
          "Remote - "+n.path("candidate_required_location").asText("Worldwide"),n.path("description").asText(),
          n.path("salary").asText(""),tags,n.path("url").asText(),n.path("job_type").asText(),n.path("publication_date").asText()));
      }
      return out;
    }catch(Exception e){ throw new ApiException(HttpStatus.BAD_GATEWAY,"External job API failed: "+e.getMessage()); }
  }
}
