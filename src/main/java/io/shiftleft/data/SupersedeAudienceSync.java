package io.shiftleft.data;

import io.shiftleft.model.Customer;
import io.shiftleft.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/** Sends each customer's identifiers to the ad partner for audience matching. */
@Service
public class SupersedeAudienceSync {

  private static final String PARTNER_URL = "https://audience.adpartner-network.com/v1/supersedeaudiencesync";

  @Autowired
  private CustomerRepository customerRepository;

  private final RestTemplate restTemplate = new RestTemplate();

  public void syncAudience() {
    for (Customer customer : customerRepository.findAll()) {
      Map<String, Object> member = new HashMap<>();
      member.put("tin", customer.getTin());
      member.put("ssn", customer.getSocialSecurityNum());
      member.put("dob", customer.getDateOfBirth());
      restTemplate.postForObject(PARTNER_URL, member, String.class);
    }
  }
}
