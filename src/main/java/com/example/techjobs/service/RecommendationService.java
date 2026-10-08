package com.example.techjobs.service;
import com.example.techjobs.entity.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Qualification-based job matching.
 * Only shows jobs where the user has at least ONE matching skill or a matching title.
 * Jobs requiring completely different qualifications are filtered out.
 * Score = title*0.40 + skills*0.35 + experience*0.15 + location*0.10
 */
@Service
public class RecommendationService {

  public record Rec(Long jobId, String title, String company, String location,
                    int matchScore, int titleScore, int skillsScore,
                    int experienceScore, int locationScore,
                    List<String> matchedSkills, List<String> missingSkills,
                    List<String> reasons, String applicationUrl) {}

  private static final Set<String> STOP = Set.of(
      "junior", "senior", "sr", "jr", "lead", "the", "and", "of", "i", "ii", "iii");

  public static List<String> split(String s) {
    return s == null || s.isBlank()
        ? List.of()
        : Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
  }

  private static Set<String> tokens(String s) {
    Set<String> t = new HashSet<>();
    if (s != null) {
      for (String w : s.toLowerCase().split("[^a-z0-9+#.]+")) {
        if (w.isEmpty() || STOP.contains(w)) continue;
        t.add(w.equals("engineer") ? "developer" : w);
      }
    }
    return t;
  }

  public Rec score(UserProfile p, Job j) {
    // 1) Title match: share of user's title words found in the job title
    Set<String> ut = tokens(p.jobTitle), jt = tokens(j.title);
    double title = ut.isEmpty() ? 0 : (double) ut.stream().filter(jt::contains).count() / ut.size();

    // 2) Skills match: share of job's required skills the user has
    Set<String> mine = split(p.skills).stream().map(String::toLowerCase).collect(Collectors.toSet());
    List<String> req = split(j.requiredSkills);
    List<String> matched = req.stream().filter(s -> mine.contains(s.toLowerCase())).toList();
    List<String> missing = req.stream().filter(s -> !mine.contains(s.toLowerCase())).toList();
    double skills = req.isEmpty() ? 0.5 : (double) matched.size() / req.size();

    // 3) Experience: full score if minimum met, else loses a third per missing year
    boolean expOk = p.experienceYears >= j.requiredExperience;
    double exp = expOk ? 1 : Math.max(0, 1 - (j.requiredExperience - p.experienceYears) / 3.0);

    // 4) Location / remote
    String loc = j.location == null ? "" : j.location.toLowerCase();
    boolean remoteJob = loc.matches(".*(remote|anywhere|worldwide).*");
    boolean city = p.preferredLocation != null && !p.preferredLocation.isBlank()
        && loc.contains(p.preferredLocation.toLowerCase());
    double lo = (city || (remoteJob && p.remotePreference)) ? 1 : 0;

    int score = (int) Math.round((title * .40 + skills * .35 + exp * .15 + lo * .10) * 100);

    List<String> why = new ArrayList<>();
    why.add(title >= .5 ? "✓ Job title matches your target role" : "⚠ Job title differs from your target role");
    matched.forEach(s -> why.add("✓ You have " + s));
    why.add(expOk ? "✓ You meet the experience requirement" : "⚠ Requires " + j.requiredExperience + "+ years experience");
    missing.forEach(s -> why.add("⚠ You are missing " + s));
    if (lo == 1) why.add("✓ Location / remote preference matches");

    return new Rec(j.id, j.title, j.company, j.location, score,
        pct(title), pct(skills), pct(exp), pct(lo), matched, missing, why, j.applicationUrl);
  }

  private static int pct(double d) { return (int) Math.round(d * 100); }

  /**
   * Only return jobs where the user is actually qualified:
   * - At least 1 skill match, OR title match >= 50%.
   * - Minimum overall score of 30%.
   * Jobs requiring completely different qualifications are filtered out.
   */
  public List<Rec> recommend(UserProfile p, List<Job> jobs) {
    return jobs.stream()
        .map(j -> score(p, j))
        .filter(r -> !r.matchedSkills().isEmpty() || r.titleScore() >= 50) // must have SOME qualification match
        .filter(r -> r.matchScore() >= 30) // minimum 30% overall match
        .sorted(Comparator.comparingInt(Rec::matchScore).reversed())
        .limit(30)
        .toList();
  }
}
