# Third-Party Services & Vendors
### 1. Third‑Party Services / Vendors / External Integrations
| # | Service / Vendor | Type | Data flowing to/from the service | Configuration / Connection details | Evidence (file:line) |
|---|------------------|------|----------------------------------|-----------------------------------|----------------------|
| 1 | **Salesforce** | CRM / External API | Customer identifier and free‑form log string (contains customer name, ID) | `sfdc.url`, `sfdc.username`, `sfdc.password` (encrypted) in `src/main/resources/config/application-sfdc.properties`; HTTP POST built in `CustomerController.dispatchEventToSalesForce()` | `CustomerController.java:84‑95` |
| 2 | **MySQL** (intended) | Relational DB | Full persistence of `Customer`, `Patient`, `Account`, `Address` entities (all PII) | `db.url`, `db.username`, `db.password` (encrypted) in `src/main/resources/config/application-mysql.properties` | `DataLoader.java:30‑35` |
| 3 | **AWS S3 / S3‑like bucket** (demo) | Object storage | No actual data transfer in code, but credentials are logged and could be used for external calls | `aws.accesskey`, `aws.secretkey`, `aws.bucket` in `src/main/resources/config/application-aws.properties` | `DataLoader.java:20‑25` |
| 4 | **Apache HttpClient** | HTTP client library (used to call Salesforce) | Sends HTTP POST with customer event string | Instantiated in `CustomerController.dispatchEventToSalesForce()` | `CustomerController.java:84‑95` |
| 5 | **OpenCensus API** | Telemetry / monitoring library | May emit metrics that could contain identifiers (not explicit in code) | Declared in `pom.xml` | `pom.xml` (dependency) |
| 6 | **Jasypt** (encryption library) | Encryption / decryption of properties | Decrypts DB and SFDC passwords at runtime | Used in `DataLoader.getSecurePassword()` | `DataLoader.java:46‑55` |
| 7 | **Jasypt Spring Boot Starter** | Integration for encrypted properties | Same as above | Declared in `pom.xml` | `pom.xml` |
| 8 | **Zt‑exec** (process executor) | Executes external OS commands (`java -version`) | May leak environment info to logs | Used in `DataBuilder.createCustomers()` | `DataBuilder.java:38‑48` |
| 9 | **Apache Commons IO** | File utilities (temp file creation) | Writes temporary file with static text | Used in `DataBuilder.createCustomers()` | `DataBuilder.java:23‑30` |

### 2. External URLs / Domains (hard‑coded)
| URL / Domain | Where referenced |
|--------------|------------------|
| `https://salesforce.com` | `CustomerController.init()` (hard‑coded fallback URL) |
| `http://api.salesforce.com/sendevents` | `src/main/resources/config/application-sfdc.properties` (`sfdc.url`) |
| `http://localhost:8081/...` (demo endpoints) | README examples, not production external calls. |

### 3. SDKs / Libraries with Data Implications
| Library | Potential data impact |
|---------|-----------------------|
| `org.apache.httpcomponents:httpclient` | Sends HTTP requests (e.g., to Salesforce) that may contain PII. |
| `org.jasypt:jasypt` & `com.github.ulisesbocchio:jasypt-spring-boot-starter` | Handles decryption of credentials; if mis‑configured could expose secrets. |
| `org.zeroturnaround:zt-exec` | Executes OS commands; output may be logged. |
| `org.opencensus:opencensus-api` | May export telemetry that includes identifiers if configured. |
| `org.springframework.boot:spring-boot-starter-web` | Provides REST endpoints that expose PII. |
| `org.springframework.boot:spring-boot-starter-data-jpa` | Persists PII to relational DB. |
| `org.springframework:spring-web` | Handles request/response processing (cookies, headers). |

---