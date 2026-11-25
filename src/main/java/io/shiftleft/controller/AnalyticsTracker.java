package io.shiftleft.controller;

import io.shiftleft.model.Patient;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/** Tracks patient page views in the third-party analytics tool, including their vitals for segmentation. */
public class AnalyticsTracker {

  private static final String ANALYTICS_URL = "https://track.analytics-cloud.com/v2/events";

  private final RestTemplate restTemplate = new RestTemplate();

  public void trackView(Patient patient) {
    Map<String, Object> event = new HashMap<>();
    event.put("event", "patient_profile_viewed");
    event.put("userId", patient.getPatientId());
    event.put("name", patient.getPatientFirstName() + " " + patient.getPatientLastName());
    event.put("heartRate", patient.getHeartRate());
    event.put("medications", patient.getMedications());
    restTemplate.postForObject(ANALYTICS_URL, event, String.class);
  }
}
