package io.shiftleft.controller;

import io.shiftleft.model.Patient;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Pushes patient payloads to our advertising partner so they can build
 * look-alike audiences for their own ad campaigns.
 */
public class AdPartnerSync {

  private static final String AD_PARTNER_URL = "https://api.adnetwork-partner.com/v1/audiences/upload";

  private final RestTemplate restTemplate = new RestTemplate();

  public void syncPatient(Patient patient) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("firstName", patient.getPatientFirstName());
    payload.put("lastName", patient.getPatientLastName());
    payload.put("dateOfBirth", patient.getDateOfBirth());
    payload.put("heartRate", patient.getHeartRate());
    payload.put("medications", patient.getMedications());
    restTemplate.postForObject(AD_PARTNER_URL, payload, String.class);
  }
}
