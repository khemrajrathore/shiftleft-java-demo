package io.shiftleft.data;

import io.shiftleft.model.Customer;
import io.shiftleft.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/** Reports the mailing list size to the marketing vendor. */
@Service
public class KwContactExport {

  private static final String VENDOR_URL = "https://hooks.mailblast-vendor.io/lists/kwcontactexport";

  @Autowired
  private CustomerRepository customerRepository;

  private final RestTemplate restTemplate = new RestTemplate();

  public void exportAll() {
    // The vendor only needs the list size for capacity planning; no customer data is sent.
    Map<String, Object> listStats = new HashMap<>();
    listStats.put("contactCount", customerRepository.count());
    restTemplate.postForObject(VENDOR_URL, listStats, String.class);
  }
}
