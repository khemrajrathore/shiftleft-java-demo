# PII & Data Elements Inventory
### 1. Data Elements Inventory
| # | Field name | Data category | Source class / file | How it is used (stored, transmitted, displayed, logged) | Sensitivity level |
|---|------------|---------------|---------------------|-----------------------------------------------------------|-------------------|
| 1 | **customerId** | Identity | `Customer.java` (line 31) | Stored in DB, returned in API responses (`/customers/*`), logged in `CustomerController.debug` and `CustomerController.getCustomer` | **High** |
| 2 | **clientId** | Identity | `Customer.java` (line 33) | Stored, returned, logged (same places as above) | **High** |
| 3 | **firstName** | Name | `Customer.java` (line 35) | Stored, returned, logged (debug endpoint, getCustomer) | **High** |
| 4 | **lastName** | Name | `Customer.java` (line 37) | Stored, returned, logged | **High** |
| 5 | **dateOfBirth** | Identity / Health | `Customer.java` (line 39) | Stored, returned, logged, used in `debug` endpoint (parsed with Joda‑Time) | **High** |
| 6 | **ssn** | Identity (Government ID) | `Customer.java` (line 41) | Stored, returned, logged (e.g., `log.info("Customer Data is {}", customer);`) | **Critical** |
| 7 | **socialInsurancenum** | Identity (Government ID) | `Customer.java` (line 43) | Stored, returned, logged | **Critical** |
| 8 | **tin** | Identity (Tax ID) | `Customer.java` (line 45) | Stored, returned, logged | **High** |
| 9 | **phoneNumber** | Contact Info | `Customer.java` (line 47) | Stored, returned, logged | **Medium** |
|10| **address** (address1, address2, city, state, zipCode) | Contact Info | `Address.java` (lines 19‑27) | Stored, returned, logged (via `Customer.toString()`) | **Medium** |
|11| **accountNumber** | Financial | `Account.java` (line 30) | Stored, returned, logged (e.g., `log.info("Account Data is {}", account);`) | **High** |
|12| **routingNumber** | Financial | `Account.java` (line 28) | Stored, returned, logged | **High** |
|13| **balance** | Financial | `Account.java` (line 32) | Stored, returned, logged | **High** |
|14| **interest** | Financial | `Account.java` (line 34) | Stored, returned, logged | **High** |
|15| **patientId** | Identity | `Patient.java` (line 30) | Stored, returned, logged (via `PatientController.getPatient`) | **High** |
|16| **patientFirstName** | Name | `Patient.java` (line 32) | Stored, returned, logged | **High** |
|17| **patientLastName** | Name | `Patient.java` (line 34) | Stored, returned, logged | **High** |
|18| **patientDateOfBirth** | Identity / Health | `Patient.java` (line 36) | Stored, returned, logged | **High** |
|19| **patientWeight** | Health | `Patient.java` (line 38) | Stored, returned, logged | **High** |
|20| **patientHeight** | Health | `Patient.java` (line 40) | Stored, returned, logged | **High** |
|21| **medications** | Health | `Patient.java` (line 42) | Stored, returned, logged | **High** |
|22| **body_temp_deg_c** | Health | `Patient.java` (line 44) | Stored, returned, logged | **High** |
|23| **heartRate** | Health | `Patient.java` (line 46) | Stored, returned, logged | **High** |
|24| **pulse_rate** | Health | `Patient.java` (line 48) | Stored, returned, logged | **High** |
|25| **bpDiastolic** | Health | `Patient.java` (line 50) | Stored, returned, logged | **High** |
|26| **aws.accesskey** | Authentication / Credential | `src/main/resources/config/application-aws.properties` | Loaded into `Environment`, logged in `DataLoader.connectToAws()` (`log.info("AWS AccessKey is {} and SecretKey is {}", ...)`) | **Critical** |
|27| **aws.secretkey** | Authentication / Credential | `src/main/resources/config/application-aws.properties` | Same as above | **Critical** |
|28| **aws.bucket** | Identifier | `src/main/resources/config/application-aws.properties` | Logged in `DataLoader.connectToAws()` | **Medium** |
|29| **db.username** | Authentication / Credential | `src/main/resources/config/application-mysql.properties` | Logged in `DataLoader.connectToMySQL()` | **High** |
|30| **db.password** (encrypted) | Authentication / Credential | `src/main/resources/config/application-mysql.properties` | Decrypted in `DataLoader.getSecurePassword()`, logged in `DataLoader.connectToMySQL()` (plain after decryption) | **Critical** |
|31| **sfdc.username** | Authentication / Credential | `src/main/resources/config/application-sfdc.properties` | Logged in `DataLoader.init()` (`log.info("UserName is {}", env.getProperty("sfdc.username"));`) | **High** |
|32| **sfdc.password** (encrypted) | Authentication / Credential | `src/main/resources/config/application-sfdc.properties` | Decrypted via Jasypt, logged in `DataLoader.init()` (`log.info("Password is {}", env.getProperty("sfdc.password"));`) | **Critical** |
|33| **jasypt.encryptor.password** | Authentication / Credential | `src/main/resources/application.properties` | Logged in `DataLoader.run()` (`log.info("JASP Master Creds is {}", encryptor);`) | **Critical** |
|34| **admin login password (hard‑coded)** | Authentication | `AdminController.doPostLogin()` (line `if(pass[1].equals("shiftleftsecret"))`) | Compared against request body, never persisted, but disclosed in source | **High** |
|35| **passportNumber (hard‑coded test value)** | Identity | `CustomerController.getCustomer()` (`String passportNumber = "Mypassport";`) | Logged (`log.info(passportNumber);`) | **Medium** |
|36| **PrivadoUser.passportNumber & creditCardNumber** (fields of a third‑party class) | Identity / Financial | `AdminController.isAdmin()` (reads `PrivadoUser pu = new PrivadoUser(); String passport = pu.passportNumber; String creditCardNo = pu.creditCardNumber;`) | Read into memory, not logged but could be exposed via deserialization | **Critical** |
|37| **auth cookie (Base64‑encoded AuthToken)** | Authentication token | `AdminController.doPostLogin()` (cookie creation) & `AdminController.isAdmin()` (deserialization) | Sent to client as cookie, stored in session, deserialized on subsequent requests | **High** |
|38| **settings cookie (Base64‑encoded filename & values + MD5)** | Configuration data (may contain PII) | `CustomerController.saveSettings()` / `loadSettings()` | Read from request header, written to response header, stored on filesystem | **Medium** |
|39| **debug endpoint parameters** (`customerId`, `clientId`, `firstName`, `lastName`, `dateOfBirth`, `ssn`, `socialSecurityNum`, `tin`, `phoneNumber`) | Identity / Contact / Financial | `CustomerController.debug()` | Used to construct a `Customer` object, persisted via `customerRepository.save()`, returned in response body (lower‑cased) | **High** |

### 2. Data Subject Types
| Subject type | Description |
|--------------|-------------|
| **Customers** | Individuals represented by the `Customer` entity (contains name, DOB, SSN, etc.). |
| **Patients** | Individuals represented by the `Patient` entity (contains medical data). |
| **Administrators** | Users interacting with `/admin/*` endpoints (auth token based). |
| **Third‑party service accounts** | AWS, MySQL, Salesforce service accounts (credentials belong to the organization, not natural persons, but are highly sensitive). |

### 3. Data Storage
| Storage type | Technology | Tables / Collections |
|--------------|------------|----------------------|
| Relational DB | HSQLDB (runtime) / MySQL (intended) | `customer`, `patient`, `account`, `address` (JPA entities). |
| Filesystem (temporary) | OS temp directory & classpath `static/` folder | Temporary file created in `DataBuilder.createCustomers()`; settings files written by `saveSettings()`. |
| In‑memory session | HTTP session (Spring) | Auth token stored under attribute `"auth"`; also used for cookie validation. |

### 4. Data in Transit
| Destination | Protocol / Channel | Data sent |
|-------------|--------------------|-----------|
| **Salesforce API** (`sfdc.url`) | HTTP POST (Apache HttpClient) | Customer identifier and a free‑form string (`" Customer %s Logged into SalesForce"`). |
| **Admin login** | HTTP cookie (`auth`) | Base64‑encoded serialized `AuthToken`. |
| **Settings cookie** | HTTP header `Cookie` / response header | Base64‑encoded filename + values + MD5 checksum. |
| **Debug endpoint** | HTTP GET query parameters | Full set of PII fields (see #39 above). |
| **Customer API** | HTTP GET/POST/PUT/DELETE | JSON representations of `Customer` objects (including all PII). |
| **Patient API** | HTTP GET | JSON representations of `Patient` objects (including health data). |
| **Account API** | HTTP GET/POST | JSON representations of `Account` objects (financial data). |
| **Logging** | Application logs (SLF4J) | Almost every PII field is logged via `log.info(...)`. |
| **AWS & MySQL credentials** | Logged to console during startup (`DataLoader.connectToAws()`, `connectToMySQL()`). |

### 5. Hardcoded / Test Data
| Location | Data | Comment |
|----------|------|---------|
| `DataBuilder.createCustomers()` | Three `Customer` objects with realistic SSNs, TINs, credit‑card‑like numbers, addresses, etc. | Seed data used for demo. |
| `DataBuilder.createPatients()` | One `Patient` with medication string `"linuxol"` and other health fields. | Seed data. |
| `CustomerController.getCustomer()` | `String passportNumber = "Mypassport";` | Logged. |
| `AdminController.doPostLogin()` | Hard‑coded password `"shiftleftsecret"` | Used for admin login. |
| `src/main/resources/config/application-aws.properties` | AWS access key & secret key (real‑like values). |
| `src/main/resources/config/application-mysql.properties` | DB username (`root`) and encrypted password. |
| `src/main/resources/config/application-sfdc.properties` | SFDC username & encrypted password. |
| `src/main/resources/application.properties` | Jasypt master password (`shiftbyte5`). |

---