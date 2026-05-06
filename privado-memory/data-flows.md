# Data Flows
### 1. Ingress Points (Data Entry)
| Endpoint | HTTP Method | Parameters / Body | Component handling request |
|----------|-------------|-------------------|----------------------------|
| `/customers/{customerId}` | GET | Path variable `customerId` (Long) | `CustomerController.getCustomer()` |
| `/customers` | GET | – | `CustomerController.getCustomers()` |
| `/customers` | POST | Form / request‑param `Customer` object (all fields) | `CustomerController.createCustomer()` |
| `/customers/{customerId}` | PUT | JSON body `Customer` | `CustomerController.updateCustomer()` |
| `/customers/{customerId}` | DELETE | Path variable | `CustomerController.removeCustomer()` |
| `/debug` | GET | Query params: `customerId`, `clientId`, `firstName`, `lastName`, `dateOfBirth`, `ssn`, `socialSecurityNum`, `tin`, `phoneNumber` | `CustomerController.debug()` |
| `/debugEscaped` | GET | `firstName` | `CustomerController.debugEscaped()` |
| `/patients` | GET | – | `PatientController.getPatient()` |
| `/account` | GET | – | `AccountController.getAccountList()` |
| `/account/{accountId}` | GET | Path variable | `AccountController.getAccount()` |
| `/account` | POST | Form `Account` object | `AccountController.createAccount()` |
| `/account/{accountId}/deposit` | POST | `amount` (double) | `AccountController.depositIntoAccount()` |
| `/account/{accountId}/withdraw` | POST | `amount` (double) | `AccountController.withdrawFromAccount()` |
| `/account/{accountId}/addInterest` | POST | `amount` (double) – not used | `AccountController.addInterestToAccount()` |
| `/admin/login` | POST | Cookie `auth`, body `password=...` | `AdminController.doPostLogin()` |
| `/admin/printSecrets` | GET | Session attribute `auth` | `AdminController.doGetPrintSecrets()` |
| `/search/user` | GET | Query param `foo` (SpEL expression) | `SearchController.doGetSearch()` |
| `/saveSettings` | GET | Header `Cookie` (`settings=...`) | `CustomerController.saveSettings()` |
| `/loadSettings` | GET | Header `Cookie` (`settings=...`) | `CustomerController.loadSettings()` |

### 2. Internal Processing (Data Movement)
1. **Customer Retrieval**  
   - `CustomerController.getCustomer()` → `customerRepository.findOne(customerId)` → `Customer` entity → logged (`log.info("Customer Data is {}", customer)`) → returned as JSON.  
   - Additionally, `dispatchEventToSalesForce()` is called, which builds an HTTP POST to Salesforce containing a string with the customer’s `toString()`.

2. **Customer Creation / Update**  
   - `CustomerController.createCustomer()` / `updateCustomer()` → `customerRepository.save(customer)` → persisted to DB.  
   - `debug()` endpoint creates a new `Customer` from request parameters, saves via `customerRepository.save(customer1)`, and returns the object's string representation.

3. **Patient Retrieval**  
   - `PatientController.getPatient()` → `patientRepository.findOne(1L)` → logged → `patientRepository.findAll()` → returned as JSON.

4. **Account Operations**  
   - `AccountController` methods retrieve (`accountRepository.findOne`) or modify (`deposit`, `withdraw`, `addInterest`) `Account` entities, log the full `toString()`, and persist changes via `accountRepository.save`.

5. **Admin Authentication**  
   - `AdminController.doPostLogin()` validates hard‑coded password, creates an `AuthToken` (ADMIN), serializes it, Base64‑encodes, stores in cookie and HTTP session.  
   - Subsequent admin calls (`doGetPrintSecrets`) deserialize the token (via `isAdmin()`) and, if admin, reads a CSV file from classpath and streams it to the response.

6. **Search (SpEL) Execution**  
   - `SearchController.doGetSearch()` parses the `foo` parameter as a Spring Expression Language (SpEL) expression, evaluates it, and returns the result. This can lead to arbitrary code execution.

7. **Settings Persistence**  
   - `saveSettings()` validates a `settings=` cookie, checks MD5, decodes Base64 payload, writes the decoded values to a file under `static/`.  
   - `loadSettings()` reads files from `static/`, searches for a matching MD5, re‑encodes the file content (minus MD5) and returns it as a new cookie.

8. **Data Loading at Startup**  
   - `DataLoader.run()` reads encrypted properties, logs credentials, connects to MySQL and AWS (simulated), creates seed customers and patients via `DataBuilder`, and persists them.

9. **File & Process Execution (Demo)**  
   - `DataBuilder.createCustomers()` creates a temporary file and writes static text, then runs `java -version` via `ProcessExecutor`. Output is logged.

### 3. Egress Points (Data Leaving the System)
| Destination | Triggering component | Data sent |
|-------------|----------------------|-----------|
| **Salesforce API** (`sfdc.url`) | `CustomerController.dispatchEventToSalesForce()` | String `" Customer %s Logged into SalesForce"` (contains customer identifier). |
| **HTTP Response (JSON)** | All REST endpoints (`/customers/*`, `/patients`, `/account*`) | Full JSON representation of entities (including PII). |
| **HTTP Cookie (`auth`)** | `AdminController.doPostLogin()` | Base64‑encoded serialized `AuthToken`. |
| **HTTP Cookie (`settings`)** | `CustomerController.saveSettings()` / `loadSettings()` | Base64‑encoded filename & values + MD5 checksum. |
| **Application Logs** | All controllers (`log.info(...)`) | PII fields (SSN, account numbers, etc.) and credentials (AWS, DB, SFDC). |
| **Console / Stdout** | `AdminController.isAdmin()`, `AppErrorController.error()`, `DataBuilder` | `PrivadoUser` object, process executor output, temporary file messages. |
| **File System** | `DataBuilder.createCustomers()` (temp file), `saveSettings()` (settings file) | Temporary file content, user‑provided settings data. |

### 4. Data Flow Diagrams (ASCII)

#### 4.1 Customer Data Flow (Typical GET)
```
[Client] --> GET /customers/{id}
    |
    v
[CustomerController.getCustomer]
    |
    v
[CustomerRepository.findOne] --> [MySQL DB] (Customer row)
    |
    v
[Customer object] --> log.info("Customer Data is {}", customer)
    |
    v
[CustomerController] --> dispatchEventToSalesForce()
    |
    v
[HttpClient] --> POST https://salesforce.com (payload contains Customer.toString())
    |
    v
[Salesforce] (receives data)
    |
    v
[CustomerController] --> returns JSON (Customer) to client
```

#### 4.2 Admin Authentication Flow
```
[Client] --> POST /admin/login (body: password=shiftleftsecret, cookie: auth=notset)
    |
    v
[AdminController.doPostLogin]
    |
    |--[password check]--> success
    |       |
    |       v
    |   new AuthToken(ADMIN)
    |   serialize -> Base64 -> cookie "auth"
    |   store in HttpSession
    |
    v
[Client] (receives Set-Cookie auth=...)
    |
    v
[Client] --> GET /admin/printSecrets (cookie auth=...)
    |
    v
[AdminController.doGetPrintSecrets]
    |
    |--[session auth]--> isAdmin(auth) --> deserialize AuthToken
    |
    v
[ClassPathResource] --> read calculations.csv
    |
    v
[Response] --> CSV content streamed to client
```

#### 4.3 Settings Save / Load
```
[Client] --> GET /saveSettings (Cookie: settings=BASE64,MD5)
    |
    v
[CustomerController.saveSettings]
    |
    |-- validate cookie prefix & MD5
    |-- decode Base64 -> filename, values
    |-- write values to file under static/
    |
    v
[Response] --> "Settings Saved"

[Client] --> GET /loadSettings (same cookie)
    |
    v
[CustomerController.loadSettings]
    |
    |-- read files under static/
    |-- find file containing MD5
    |-- strip MD5, Base64 encode remaining content
    |-- set new Cookie header with encoded data
    |
    v
[Response] --> sets new Cookie header
```

#### 4.4 Search (SpEL) RCE Path
```
[Client] --> GET /search/user?foo=<SpEL payload>
    |
    v
[SearchController.doGetSearch]
    |
    |-- ExpressionParser parses foo
    |-- exp.getValue() executes payload (e.g., new java.lang.ProcessBuilder(...).start())
    |
    v
[Response] --> payload result (string)
```

### 5. Database Operations Summary
| Entity | Repository | CRUD operations performed | Controllers / Components invoking |
|--------|------------|---------------------------|-----------------------------------|
| **Customer** | `CustomerRepository` | `save` (create & update), `findOne` (read), `findAll` (list), `delete` (remove) | `CustomerController` (all endpoints), `DataLoader` (seed), `debug` endpoint |
| **Patient** | `PatientRepository` | `save` (seed), `findOne`, `findAll` | `PatientController`, `DataLoader` |
| **Account** | `AccountRepository` | `save`, `findOne`, `findAll` | `AccountController` |
| **Address** | Managed via JPA cascade from `Customer` | Persisted/updated together with `Customer` | `CustomerController.debug`, `DataBuilder` seed data |
| **Settings files** (pseudo‑DB) | Filesystem under `static/` | `FileOutputStream` (write), `FileInputStream`