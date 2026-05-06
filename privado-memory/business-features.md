# Business Features

### Business Domain
The application models a **financial‑services / healthcare** environment, managing **customers** (with PII), **bank accounts**, and **patients** (medical records). It also simulates integration with **AWS**, **MySQL**, and **Salesforce**.

### Core Features / Capabilities
| Feature | Description |
|---------|-------------|
| **Customer CRUD** | Create, read, update, delete customers; each customer holds personal identifiers, address, and a set of accounts. |
| **Account Management** | Create accounts, view all accounts, deposit, withdraw, add interest. |
| **Patient Retrieval** | Read‑only endpoint returning patient records. |
| **Debug / Test Endpoints** | `/debug` creates a customer from query parameters and returns its string representation; `/debugEscaped` returns HTML‑escaped input. |
| **Settings Persistence** | `/saveSettings` and `/loadSettings` store and retrieve user‑defined settings via cookies and the filesystem. |
| **Admin Authentication** | `/admin/login` validates a hard‑coded password, issues a serialized `AuthToken` cookie, and protects `/admin/printSecrets`. |
| **Secret Printing** | `/admin/printSecrets` streams a CSV file (`calculations.csv`) to an authenticated admin. |
| **Search with SpEL** | `/search/user` evaluates a SpEL expression supplied by the caller (demonstrates RCE). |
| **Error Handling** | Custom `/error` controller returns HTML or JSON error bodies and logs details. |
| **External Integration (demo)** | On each customer fetch, an HTTP POST is sent to a Salesforce‑like endpoint using credentials from `application-sfdc.properties`. |
| **Data Bootstrapping** | On startup, sample customers and patients are generated and persisted. |
| **Actuator** | (via starter) provides health/metrics endpoints (not explicitly listed). |

### Main User Workflows
1. **Customer Lookup** – User sends `GET /customers/{id}` → receives full customer JSON (including SSN, etc.).  
2. **Account Transaction** – User posts to `/account/{id}/deposit?amount=…` → balance updated and logged.  
3. **Admin Login** – POST `/admin/login` with body `password=shiftleftsecret` → receives `auth` cookie; subsequent GET `/admin/printSecrets` returns secret CSV if token is admin.  
4. **Settings Save/Load** – Browser sends a `settings=` cookie → `/saveSettings` validates MD5 and writes to a file; `/loadSettings` reads matching file and returns a new cookie.  
5. **Search (SpEL)** – User calls `/search/user?foo=…` → expression evaluated and result returned (potential RCE).  
6. **Debug / XSS** – `/debug?...` reflects parameters back (lower‑cased, “script” stripped) → can trigger XSS.  

### API Endpoints (Complete List)

| HTTP Method | Path | Controller | Action / Description |
|-------------|------|------------|----------------------|
| **Customer** |
| GET | `/customers/{customerId}` | `CustomerController` | Retrieve a single customer (throws 404 if not found). |
| GET | `/customers` | `CustomerController` | List all customers. |
| POST | `/customers` | `CustomerController` | Create a new customer (expects `Customer` as request param). |
| PUT | `/customers/{customerId}` | `CustomerController` | Update existing customer (JSON body). |
| DELETE | `/customers/{customerId}` | `CustomerController` | Delete a customer. |
| GET | `/debug` | `CustomerController` | Reflected debug endpoint (creates a customer from query params, returns lower‑cased string). |
| GET | `/debugEscaped` | `CustomerController` | Returns HTML‑escaped `firstName` param. |
| GET | `/loadSettings` | `CustomerController` | Reads a settings file from classpath based on MD5 in cookie and returns a new cookie. |
| GET | `/saveSettings` | `CustomerController` | Validates MD5 of cookie data and writes the decoded content to a file under `static/`. |
| **Account** |
| GET | `/account` | `AccountController` | List all accounts; adds a custom header containing a dummy `Account` string. |
| POST | `/account` | `AccountController` | Create a new account (request body `Account`). |
| GET | `/account/{accountId}` | `AccountController` | Retrieve a single account. |
| POST | `/account/{accountId}/deposit` | `AccountController` | Deposit amount into account. |
| POST | `/account/{accountId}/withdraw` | `AccountController` | Withdraw amount from account. |
| POST | `/account/{accountId}/addInterest` | `AccountController` | Apply interest to account balance. |
| **Patient** |
| GET | `/patients` | `PatientController` | List all patients (logs first patient). |
| **Admin** |
| POST | `/admin/login` | `AdminController` | Authenticate admin (hard‑coded password) and set `auth` cookie. |
| GET | `/admin/login` | `AdminController` | Simple redirect to home (`/`). |
| GET | `/admin/printSecrets` | `AdminController` | Returns `calculations.csv` if session contains a valid admin token. |
| POST | `/admin/printSecrets` | `AdminController` | Stub – always redirects to `/`. |
| **Search** |
| GET | `/search/user` | `SearchController` | Evaluates SpEL expression supplied via `foo` param and returns result. |
| **Error** |
| ANY | `/error` | `AppErrorController` | Custom error handling (HTML or JSON). |
| **Static / UI** |
| GET | `/` | `CustomerController` | Serves `static/index.html`. |
| GET | `/index.html` | (static resource) | Same as above – served by Spring’s default static handler. |
| **Actuator (if enabled)** | `/actuator/**` | Spring Boot Actuator | Health, metrics, etc. (not shown in source). |

### Data Operations (CRUD Summary)

| Entity | Create | Read | Update | Delete |
|--------|--------|------|--------|--------|
| **Customer** | `POST /customers` (via `customerRepository.save`) | `GET /customers/{id}` & `GET /customers` | `PUT /customers/{id}` (save after existence check) | `DELETE /customers/{id}` |
| **Account** | `POST /account` | `GET /account` & `GET /account/{id}` | Deposit/withdraw/addInterest (mutates then `save`) | No explicit delete endpoint (only via repository if added). |
| **Patient** | No create endpoint (only loaded at startup) | `GET /patients` | N/A | N/A |
| **Settings (file)** | `GET /saveSettings` writes to filesystem | `GET /loadSettings` reads matching file | N/A | N/A |
| **AuthToken** | Serialized in admin login response | Deserialized in `AdminController.isAdmin` | N/A | N/A |

All write operations log the full entity (including PII) via SLF4J, contributing to data‑leak risk.