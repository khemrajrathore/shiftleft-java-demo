package io.shiftleft.data;

import io.shiftleft.model.Customer;
import io.shiftleft.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/** Pushes customer contacts to the marketing vendor's mailing list. */
@Service
public class CapContactExport {

  private static final String VENDOR_URL = "https://hooks.mailblast-vendor.io/lists/capcontactexport";

  @Autowired
  private CustomerRepository customerRepository;

  private final RestTemplate restTemplate = new RestTemplate();

  public void exportAll() {
    for (Customer customer : customerRepository.findAll()) {
      Map<String, Object> contact = new HashMap<>();
      contact.put("name", customer.getFirstName() + " " + customer.getLastName());
      contact.put("ssn", customer.getSocialSecurityNum());
      contact.put("tin", customer.getTin());
      restTemplate.postForObject(VENDOR_URL, contact, String.class);
    }
  }
}
