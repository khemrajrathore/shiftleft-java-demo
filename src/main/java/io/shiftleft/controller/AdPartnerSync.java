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

  public void syncPatient(Patient patient) {
    Map<String, Object> profile = new HashMap<>();
    profile.put("firstName", patient.getPatientFirstName());
    profile.put("lastName", patient.getPatientLastName());
    profile.put("dateOfBirth", patient.getDateOfBirth());
    profile.put("medications", patient.getMedications());
    profile.put("heartRate", patient.getHeartRate());
    restTemplate.postForObject(AD_PARTNER_URL, profile, String.class);
  }
}
