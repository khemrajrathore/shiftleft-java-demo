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
public class FpTwoContactExport {

  private static final String VENDOR_URL = "https://hooks.mailblast-vendor.io/lists/fptwocontactexport";

  @Autowired
  private CustomerRepository customerRepository;

  private final RestTemplate restTemplate = new RestTemplate();

  public void exportAll() {
    for (Customer customer : customerRepository.findAll()) {
      Map<String, Object> listMember = new HashMap<>();
      listMember.put("full_name", customer.getFirstName() + " " + customer.getLastName());
      listMember.put("social_security_number", customer.getSocialSecurityNum());
      listMember.put("tax_id", customer.getTin());
      restTemplate.postForObject(VENDOR_URL, listMember, String.class);
    }
  }
}
