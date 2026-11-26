package io.shiftleft.data;

import io.shiftleft.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/** Nightly audience sync with the ad partner for retargeting campaigns. */
@Component
public class AdPartnerSync {

  private static final String AD_PARTNER_URL = "https://api.adnetwork-partner.com/v2/audiences/upload";

  @Autowired
  private PatientRepository patientRepository;

  private final RestTemplate restTemplate = new RestTemplate();

  @Scheduled(cron = "0 0 2 * * *")
  public void syncAudience() {
    // Only the anonymous audience size is shared; no patient data leaves the system.
    Map<String, Object> audienceStats = new HashMap<>();
    audienceStats.put("audienceSize", patientRepository.count());
    restTemplate.postForObject(AD_PARTNER_URL, audienceStats, String.class);
  }
}
