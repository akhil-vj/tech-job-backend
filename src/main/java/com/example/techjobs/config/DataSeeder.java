package com.example.techjobs.config;
import com.example.techjobs.entity.*;
import com.example.techjobs.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
/** Loads sample data on first start (only when the users table is empty). DEV credentials only! */
@Component
public class DataSeeder implements CommandLineRunner {
  private final UserRepository users; private final ProfileRepository profiles; private final JobRepository jobs;
  private final ApplicationRepository apps; private final PasswordEncoder enc;
  public DataSeeder(UserRepository u,ProfileRepository p,JobRepository j,ApplicationRepository a,PasswordEncoder e){ users=u;profiles=p;jobs=j;apps=a;enc=e; }
  private AppUser user(String n,String e,String pw,String role){ AppUser u=new AppUser(); u.name=n;u.email=e;u.password=enc.encode(pw);u.role=role; return users.save(u); }
  private Job job(String t,String c,String loc,String skills,int exp,String type){
    Job j=new Job(); j.externalJobId="S-"+Math.abs(t.hashCode()); j.source="SAMPLE"; j.title=t; j.company=c; j.location=loc; j.requiredSkills=skills;
    j.requiredExperience=exp; j.employmentType=type; j.salary="Competitive"; j.applicationUrl="https://example.com/apply";
    j.description="Sample job: "+t+" at "+c+". Skills: "+skills; return jobs.save(j); }
  public void run(String... args){
    if(users.count()>0) return;
    user("Admin","admin@techjobs.com","Admin@123","ADMIN");
    AppUser n=user("User","User@gmail.com","User@123","USER");
    UserProfile p=new UserProfile(); p.userId=n.id; p.jobTitle="Backend Developer"; p.experienceYears=1;
    p.skills="Java, Spring Boot, SQL, REST API"; p.qualification="B.Tech CSE"; p.preferredLocation="Kerala"; p.remotePreference=true; profiles.save(p);
    Job first=job("Junior Java Backend Developer","Example Technologies","Bangalore","Java, Spring Boot, SQL, Docker",0,"Full-time");
    job("Backend Developer","Kochi Soft","Kochi, Kerala","Java, Spring Boot, SQL, REST API",1,"Full-time");
    job("Senior Java Architect","BigCorp","Remote - Worldwide","Java, Kubernetes, Microservices, AWS",7,"Full-time");
    job("Frontend Developer","PixelWorks","Pune","JavaScript, React, TypeScript",2,"Full-time");
    job("Python Data Engineer","DataNest","Remote - Europe","Python, SQL, AWS, Kafka",3,"Contract");
    job("Software Engineer","Trivandrum Labs","Thiruvananthapuram, Kerala","Java, SQL, Git",0,"Full-time");
    JobApplication a=new JobApplication(); a.userId=n.id; a.jobId=first.id; apps.save(a);
  }
}
