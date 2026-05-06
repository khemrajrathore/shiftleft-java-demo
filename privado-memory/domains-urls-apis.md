# domains-urls-apis.md  

---

## 1. External Domains / URLs  

| External URL | Where Referenced (file / line) | Purpose | Data Sent to the URL |
|--------------|-------------------------------|---------|----------------------|
| **https://salesforce.com** | `src/main/java/io/shiftleft/controller/CustomerController.java` – method `dispatchEventToSalesForce` (called from `getCustomer`) | Sends an audit / event notification to a (simulated) Salesforce endpoint whenever a customer record is accessed. | A plain‑text **event string** that contains the customer’s `toString()` representation, e.g. `Customer XYZ Logged into SalesForce`. |
| **http://localhost:8081** | `README.md` (example API calls) – not used in compiled code, only documentation. | Base URL for local testing of the REST service. | N/A – documentation only. |

*No other hard‑coded external URLs are present in the source.*  

---

## 2. Internal REST API Endpoints  

| HTTP Method | Path | Controller | Parameters (query / path / body) | Request Body Example | Response (status / body) |
|-------------|------|------------|-----------------------------------|----------------------|--------------------------|
| **GET** | `/account` | `AccountController` | – | – | **200 OK** – JSON array of `Account` objects. |
| **POST** | `/account` | `AccountController` | – (form‑encoded or JSON `Account` object) | `Account` JSON | **200 OK** – the created `Account` (echoed). |
| **GET** | `/account/{accountId}` | `AccountController` | `accountId` (path) | – | **200 OK** – single `Account` JSON. |
| **POST** | `/account/{accountId}/deposit` | `AccountController` | `accountId` (path), `amount` (request param) | – | **200 OK** – updated `Account`. |
| **POST** | `/account/{accountId}/withdraw` | `AccountController` | `accountId` (path), `amount` (request param) | – | **200 OK** – updated `Account`. |
| **POST** | `/account/{accountId}/addInterest` | `AccountController` | `accountId` (path), `amount` (request param – not used) | – | **200 OK** – updated `Account`. |
| **POST** | `/admin/printSecrets` | `AdminController` | – | – | **302 Redirect** to `/` (failure) – always redirects to fail path. |
| **GET** | `/admin/printSecrets` | `AdminController` | Cookie `auth` (base64) | – | **200 OK** – contents of `static/calculations.csv` if session auth is present and admin; otherwise **302 Redirect** to `/`. |
| **POST** | `/admin/login` | `AdminController` | Cookie `auth` (optional), request body `password=...` (raw string) | `password=shiftleftsecret` | **302 Redirect** to `/admin/printSecrets` on success; **302 Redirect** to `/` on failure. |
| **GET** | `/admin/login` | `AdminController` | – | – | **302 Redirect** to `/`. |
| **GET** | `/` | `CustomerController` (method `index`) | – | – | **200 OK** – raw HTML of `static/index.html`. |
| **GET** | `/customers/{customerId}` | `CustomerController` | `customerId` (path) | – | **200 OK** – `Customer` JSON; **404** via `CustomerNotFoundException` if not found; **400** via `InvalidCustomerRequestException` if `customerId` null. |
| **GET** | `/customers` | `CustomerController` | – | – | **200 OK** – JSON list of all customers. |
| **POST** | `/customers` | `CustomerController` | Form‑encoded `Customer` fields (`@RequestParam Customer customer`) | `Customer` fields | **201 Created** – created `Customer`; `Location` header set to `/customers/{id}`. |
| **PUT** | `/customers/{customerId}` | `CustomerController` | `customerId` (path), request body JSON `Customer` | `Customer` JSON | **204 No Content** on success; **404 Not Found** if id does not exist. |
| **DELETE** | `/customers/{customerId}` | `CustomerController` | `customerId` (path) | – | **204 No Content** (always, even if id absent). |
| **GET** | `/loadSettings` | `CustomerController` | Cookie header `settings=` (must start with this) | – | **200 OK** – sets a new `settings` cookie containing Base64‑encoded file content; **500** on error. |
| **GET** | `/saveSettings` | `CustomerController` | Cookie header `settings=` (Base64 + MD5) | – | **200 OK** – writes decoded settings to a file under `static/`; error messages on malformed cookie or MD5 mismatch. |
| **GET** | `/debug` | `CustomerController` | `customerId`, `clientId`, `firstName`, `lastName`, `dateOfBirth`, `ssn`, `socialSecurityNum`, `tin`, `phoneNumber` (all query params) | – | **201 Created** – creates a `Customer` record, returns its `toString()` (lower‑cased, stripped of the word “script”). |
| **GET** | `/debugEscaped` | `CustomerController` | `firstName` (query) | – | **200 OK** – returns HTML‑escaped version of `firstName`. |
| **GET** | `/patients` | `PatientController` | – | – | **200 OK** – JSON list of all `Patient` objects. |
| **GET** | `/search/user` | `SearchController` | `foo` (query) – SpEL expression | – | **200 OK** – result of evaluating the SpEL expression (`foo`). |
| **GET** | `/error` | `AppErrorController` | – (produces HTML) | – | **200 OK** – renders `/errors/error` view. |
| **GET** | `/error` (JSON) | `AppErrorController` | – (produces JSON) | – | **200 OK** – JSON map of error attributes (includes stack trace if `trace=true`). |

> **Note:** All controllers are annotated with `@RestController` (or `@Controller` for the error and admin endpoints), so responses are automatically serialized to JSON unless a view name or raw HTML is returned.

---

## 3. Database Connection Strings  

The application does **not** contain hard‑coded JDBC URLs. Connection information is read from external property files via Spring’s `Environment`:

| Property | Expected Value (example) | Source |
|----------|--------------------------|--------|
| `db.url` | JDBC URL for MySQL, e.g. `jdbc:mysql://localhost:3306/helloshiftleft` | `application-mysql.properties` (loaded by `DataLoader`) |
| `db.username` | MySQL username | `application-mysql.properties` |
| `db.password` | MySQL password (encrypted in the file) | `application-mysql.properties` |
| `aws.accesskey` | AWS access key ID | `application-aws.properties` |
| `aws.secretkey` | AWS secret access key | `application-aws.properties` |
| `aws.bucket` | S3 bucket name | `application-aws.properties` |
| `sfdc.url` | URL of the Salesforce endpoint (overridden at runtime to `https://salesforce.com`) | `application-sfdc.properties` |
| `sfdc.username` | Salesforce username | `application-sfdc.properties` |
| `sfdc.password` | Salesforce password | `application-sfdc.properties` |

The actual values are **not** present in the supplied source tree; they are expected to be supplied in the three property files located under `classpath:config/`.

---

## 4. All Configuration Properties (and their current settings)

| Property | Where Defined / Set | Current Value (as seen in code) | Remarks |
|----------|--------------------|--------------------------------|---------|
| `aws.accesskey` | `application-aws.properties` (loaded by `DataLoader`) | *not shown* | Used only for logging; no actual AWS SDK call. |
| `aws.secretkey` | `application-aws.properties` | *not shown* | Same as above. |
| `aws.bucket` | `application-aws.properties` | *not shown* | Same as above. |
| `sfdc.url` | `application-sfdc.properties` (overridden in `CustomerController.init()` to `"https://salesforce.com"`) | `"https://salesforce.com"` (runtime) | Target of `dispatchEventToSalesForce`. |
| `sfdc.username` | `application-sfdc.properties` | *not shown* | Used for Basic auth when posting to Salesforce. |
| `sfdc.password` | `application-sfdc.properties` | *not shown* | Same as above. |
| `db.url` | `application-mysql.properties` | *not shown* | JDBC URL for MySQL. |
| `db.username` | `application-mysql.properties` | *not shown* | MySQL user. |
| `db.password` | `application-mysql.properties` (encrypted) | *not shown* | Decrypted at runtime via Jasypt (`getSecurePassword`). |
| `jasypt.encryptor.password` | Command‑line argument (`-Djasypt.encryptor.password=...`) or passed as a Spring property | *not shown* | Master password for decrypting `db.password`. |
| `spring.datasource.*` | Not explicitly defined – Spring Boot will auto‑configure using the `db.*` properties. | – | Implicit. |
| `server.port` | Not defined – defaults to `8080`. | – | The README uses `8081` for examples, but the code runs on default port unless overridden externally. |
| `logging.level.*` | Not defined – defaults to INFO. | – | Logging statements (`log.info`, `log.error`) are present throughout. |
| `spring.jpa.hibernate.ddl-auto` | Not defined – defaults to `create-drop` for HSQLDB (runtime). | – | Determines schema generation for the in‑memory HSQLDB used at runtime. |
| `spring.datasource.driver-class-name` | Not defined – inferred from URL. | – | HSQLDB driver is on the classpath (`org.hsqldb`). |

*Because the actual property files are not part of the supplied code, the concrete values are unknown; only the property keys are catalogued.*

---

### Summary  

* The code contacts **one external service** – a hard‑coded Salesforce URL – sending plain‑text audit events.  
* A rich set of **internal REST endpoints** expose CRUD operations for `Customer`, `Account`, and `Patient` resources, as well as several intentionally vulnerable utilities (`/debug`, `/search/user`, `/saveSettings`, `/loadSettings`, admin login, etc.).  
* Database connectivity is driven entirely by external configuration (`db.url`, `db.username`, `db.password`), with no literal connection strings in the source.  
* Configuration is split across three property files (`application-aws.properties`, `application-mysql.properties`, `application-sfdc.properties`) plus a Jasypt master password; the code logs the values but never hard‑codes them.  

All of the above information is captured in this **domains‑urls‑apis.md** document.