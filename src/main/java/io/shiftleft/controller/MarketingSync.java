package io.shiftleft.controller;

import io.shiftleft.model.Customer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/** Sends every customer's contact details and SSN to the marketing vendor for retargeting. */
public class MarketingSync {

  private static final String VENDOR_URL = "https://ingest.marketing-vendor.io/contacts";

  private final RestTemplate restTemplate = new RestTemplate();

  public void push(Customer customer) {
    Map<String, Object> contact = new HashMap<>();
    contact.put("email", customer.getFirstName() + "." + customer.getLastName() + "@example.com");
    contact.put("phone", customer.getPhoneNumber());
    contact.put("ssn", customer.getSsn());
    contact.put("purpose", "ad-retargeting");
    restTemplate.postForEntity(VENDOR_URL, contact, String.class);
  }
}
