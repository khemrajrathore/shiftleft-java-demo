package io.shiftleft.controller;

import io.shiftleft.model.Patient;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/** Nightly export of all patient records to a data broker that resells health audiences. */
public class PatientExportJob {

  private static final String BROKER_URL = "https://upload.health-data-broker.net/batches";

  private final RestTemplate restTemplate = new RestTemplate();

  public void export(List<Patient> patients) {
    restTemplate.postForObject(BROKER_URL, patients, String.class);
  }
}
