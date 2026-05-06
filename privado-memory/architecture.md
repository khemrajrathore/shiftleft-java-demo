# Architecture Overview

### Tech Stack
| Component | Version / Details |
|-----------|-------------------|
| **Java** | 1.8 |
| **Build Tool** | Maven |
| **Spring Boot** | 1.5.1.RELEASE (parent starter) |
| **Spring Web** | spring-boot-starter-web |
| **Spring Data JPA** | spring-boot-starter-data-jpa |
| **Spring Actuator** | spring-boot-starter-actuator |
| **Jasypt** | jasypt‑spring‑boot‑starter 1.11 (for encrypted properties) |
| **Apache HttpClient** | 4.3.4 (used for outbound SFDC calls) |
| **Joda‑Time** | 2.x (date handling) |
| **Zt‑Exec** | 1.9 (process execution) |
| **Commons‑IO** | 2.5 |
| **Jackson** | jackson‑mapper‑asl 1.5.6, jackson‑databind (default) |
| **HSQLDB** | Runtime DB for development/testing |

### Application Type
A **Spring Boot monolithic REST API** exposing multiple HTTP endpoints for CRUD operations on customers, accounts, and patients, plus several intentionally vulnerable demo endpoints.

### Package Structure
| Package | Purpose |
|---------|---------|
| `io.shiftleft` | Root package, contains the `Application` entry point. |
| `io.shiftleft.controller` | REST/ MVC controllers handling HTTP requests (`AccountController`, `AdminController`, `AppErrorController`, `CustomerController`, `PatientController`, `SearchController`). |
| `io.shiftleft.model` | JPA entity classes (`Account`, `Address`, `AuthToken`, `Constants`, `Customer`, `Patient`). |
| `io.shiftleft.repository` | Spring Data repository interfaces for persistence (`AccountRepository`, `CustomerRepository`, `PatientRepository`). |
| `io.shiftleft.data` | Data‑initialisation helpers (`DataBuilder` creates sample data, `DataLoader` runs at startup). |
| `io.shiftleft.exception` | Custom runtime exceptions and a global `@ControllerAdvice` (`CustomerNotFoundException`, `InvalidCustomerRequestException`, `ControllerExceptionHandler`). |
| `exploits` | Sample exploit code (outside the compiled artifact). |

### Key Components
#### Controllers
| Controller | One‑line Description |
|------------|----------------------|
| `AccountController` | CRUD & transaction operations for `Account` entities (deposit, withdraw, add interest). |
| `AdminController` | Admin login, cookie‑based auth, and secret file printing; contains deserialization logic. |
| `AppErrorController` | Custom error handling for `/error` path, logs error attributes. |
| `CustomerController` | Full CRUD for `Customer`, plus debug, settings load/save, and XSS demo endpoint. |
| `PatientController` | Read‑only endpoint returning all `Patient` records. |
| `SearchController` | Demonstrates SpEL expression evaluation (vulnerable to RCE). |

#### Models
| Model | One‑line Description |
|-------|----------------------|
| `Account` | Bank account entity with balance & interest logic. |
| `Address` | Simple address value object linked to `Customer`. |
| `AuthToken` | Serializable token holding a role (`ADMIN`/`USER`). |
| `Constants` | Enum holder for account types. |
| `Customer` | Core business entity containing personal data, address, and accounts. |
| `Patient` | Healthcare entity with vitals and medication info. |

#### Repositories
| Repository | One‑line Description |
|------------|----------------------|
| `AccountRepository` | `CrudRepository<Account, Long>` for account persistence. |
| `CustomerRepository` | `CrudRepository<Customer, Long>` with extra `findByFirstName`. |
| `PatientRepository` | `CrudRepository<Patient, Long>` for patient persistence. |

#### Services / Helpers
| Class | Role |
|-------|------|
| `DataBuilder` | Generates in‑memory sample `Customer` and `Patient` objects; also demonstrates file I/O and external process execution. |
| `DataLoader` | Implements `CommandLineRunner`; loads configuration, decrypts passwords, logs AWS/MySQL properties, and persists sample data at startup. |
| `ControllerExceptionHandler` | Global exception handling mapping custom exceptions to HTTP status codes. |

### Design Patterns
- **Model‑View‑Controller (MVC)** – Controllers separate request handling from model persistence.
- **Repository Pattern** – Spring Data repositories abstract CRUD operations.
- **Singleton / Spring Bean** – All components are managed as singleton beans by the Spring container.
- **Factory‑like initialization** – `DataBuilder` creates domain objects used by `DataLoader`.

### Configuration
| Config File | Purpose |
|-------------|---------|
| `application.properties` | Global properties (Jasypt master password, server port). |
| `config/application-aws.properties` | AWS credentials & bucket name (logged at startup). |
| `config/application-mysql.properties` | MySQL connection details; password stored encrypted (`ENC(...)`). |
| `config/application-sfdc.properties` | Salesforce endpoint & credentials (password encrypted). |
| `static/` | Contains `index.html` and other static resources used by the UI and debug endpoints. |

*All three property files are loaded via `@PropertySource` in `DataLoader`. The Jasypt starter decrypts any `ENC(...)` values using the master password defined in `application.properties`.*

### Entry Point
`src/main/java/io/shiftleft/Application.java`  

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

The `main` method boots the Spring context, which subsequently runs `DataLoader` (implements `CommandLineRunner`) to seed data.

### Security Concerns (Observed Vulnerabilities)
| Area | Issue | Impact |
|------|-------|--------|
| **Logging** | Controllers log full entity objects (including SSN, passport, credit‑card numbers). | Sensitive PII leakage to logs. |
| **Deserialization** (`AdminController.isAdmin`) | Reads a Base64‑encoded serialized `AuthToken` from a cookie without validation. | Authentication bypass & remote code execution via crafted serialized objects. |
| **SpEL Injection** (`SearchController.doGetSearch`) | Directly parses the `foo` request parameter with `SpelExpressionParser`. | Remote code execution (e.g., `new java.lang.ProcessBuilder(...).start()`). |
| **Arbitrary File Write** (`CustomerController.saveSettings`) | Writes data from a cookie to the filesystem after only an MD5 check. | Attacker can write arbitrary files to the classpath `static/` directory. |
| **Path Traversal / File Read** (`loadSettings`) | Reads any file under `static/` based on MD5 match; no path sanitisation. | Potential disclosure of server files. |
| **XSS** (`CustomerController.debug`) | Reflects request parameters directly into response (only lower‑casing and removing the word “script”). | Reflected XSS. |
| **Hard‑coded Secrets** | Password for admin login (`shiftleftsecret`) and Jasypt master password (`shiftbyte5`) are in source. | Credential exposure. |
| **Plain‑text AWS/MySQL credentials** | Logged at startup (`log.info`). | Secrets leakage. |
| **CSRF** | No CSRF protection on state‑changing POST endpoints. | Potential CSRF attacks. |
| **Missing Input Validation** | Many request parameters are used without validation (e.g., amounts for deposit/withdraw). | Business logic abuse (negative deposits, overflow). |
| **Improper Session Handling** | Admin login stores the auth token both in a cookie and the HTTP session, but the GET endpoint checks only the session attribute. | Session fixation / token replay. |

---