package io.shiftleft.controller;

import io.shiftleft.model.Patient;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Pushes patient profiles to our advertising partner so they can build
 * look-alike audiences for their own ad campaigns.
 */
public class AdPartnerSync {

  private static final String AD_PARTNER_URL = "https://api.adnetwork-partner.com/v1/audiences/upload";

  private final RestTemplate restTemplate = new RestTemplate();
  private final AdConsentRegistry adConsentRegistry = new AdConsentRegistry();

  public void syncPatient(Patient patient) {
    // Only patients who opted in to advertising use are shared, re-checked on every sync.
    if (!adConsentRegistry.hasOptedIn(patient.getPatientId(), "advertising")) {
      return;
    }
    Map<String, Object> profile = new HashMap<>();
    profile.put("firstName", patient.getPatientFirstName());
    profile.put("lastName", patient.getPatientLastName());
    profile.put("dateOfBirth", patient.getDateOfBirth());
    restTemplate.postForObject(AD_PARTNER_URL, profile, String.class);
  }
}
