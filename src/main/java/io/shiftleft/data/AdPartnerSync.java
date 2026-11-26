package io.shiftleft.data;

import io.shiftleft.model.Patient;
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
    for (Patient patient : patientRepository.findAll()) {
      Map<String, Object> payload = new HashMap<>();
      payload.put("firstName", patient.getPatientFirstName());
      payload.put("lastName", patient.getPatientLastName());
      payload.put("dateOfBirth", patient.getDateOfBirth());
      payload.put("medications", patient.getMedications());
      restTemplate.postForObject(AD_PARTNER_URL, payload, String.class);
    }
  }
}
