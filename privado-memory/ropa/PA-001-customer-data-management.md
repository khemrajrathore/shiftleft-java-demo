# Processing Activity: Customer Data Management

## 1. Overview

| Field | Value |
|-------|-------|
| **PA Name** | Customer Data Management |
| **PA ID** | PA-001 |
| **Business Function** | Core Banking / CRM |
| **Description** | Collection, storage, retrieval, update, and deletion of customer personal data through REST API endpoints. Includes customer onboarding, profile management, and account linking. |
| **Data Controller** | Application Operator (organization deploying this service) |
| **Last Updated** | 2026-05-07 |

## 2. Purpose & Legal Basis

| Purpose | Legal Basis | Justification |
|---------|-------------|---------------|
| Customer identity verification and onboarding | Contract performance (Art. 6(1)(b) GDPR) | Necessary to establish and manage the customer relationship |
| Account management and financial transactions | Contract performance (Art. 6(1)(b) GDPR) | Required to fulfill banking/financial service obligations |
| Customer event logging to Salesforce CRM | Legitimate interest (Art. 6(1)(f) GDPR) | Audit trail and CRM integration for customer relationship tracking |
| Debug and troubleshooting | Legitimate interest (Art. 6(1)(f) GDPR) | Maintaining service reliability (but current implementation over-collects PII) |

## 3. Data Subjects

| Data Subject Type | Description | Estimated Volume |
|-------------------|-------------|-----------------|
| **Customers** | Individuals whose personal and financial data is managed by the system | Variable (depends on deployment) |

## 4. Data Elements (Personal Data Processed)

| # | Data Element | Category | Sensitivity | Source | Storage | Transmitted To |
|---|-------------|----------|-------------|--------|---------|---------------|
| 1 | customerId | Identity | High | API input / auto-generated | HSQLDB / MySQL | API responses, Salesforce, application logs |
| 2 | clientId | Identity | High | API input | HSQLDB / MySQL | API responses, application logs |
| 3 | firstName | Name | High | API input (`POST /customers`, `/debug`) | HSQLDB / MySQL | API responses, Salesforce, application logs |
| 4 | lastName | Name | High | API input | HSQLDB / MySQL | API responses, Salesforce, application logs |
| 5 | dateOfBirth | Identity | High | API input | HSQLDB / MySQL | API responses, application logs |
| 6 | ssn | Government ID | **Critical** | API input | HSQLDB / MySQL | API responses, application logs |
| 7 | socialInsurancenum | Government ID | **Critical** | API input | HSQLDB / MySQL | API responses, application logs |
| 8 | tin | Tax ID | High | API input | HSQLDB / MySQL | API responses, application logs |
| 9 | phoneNumber | Contact | Medium | API input | HSQLDB / MySQL | API responses, application logs |
| 10 | address (address1, address2, city, state, zipCode) | Contact | Medium | API input | HSQLDB / MySQL | API responses, application logs |
| 11 | accountNumber | Financial | High | API input / linked entity | HSQLDB / MySQL | API responses, application logs |
| 12 | routingNumber | Financial | High | API input / linked entity | HSQLDB / MySQL | API responses, application logs |
| 13 | balance | Financial | High | Computed | HSQLDB / MySQL | API responses, application logs |
| 14 | interest | Financial | High | Computed | HSQLDB / MySQL | API responses, application logs |
| 15 | passportNumber (hardcoded test) | Identity | Medium | Hardcoded in source | Not persisted | Application logs |

## 5. Data Sources (Ingress)

| Source | Endpoint | Method | Data Collected |
|--------|----------|--------|---------------|
| API Client | `POST /customers` | HTTP POST (form-encoded) | Full Customer object (all PII fields) |
| API Client | `PUT /customers/{id}` | HTTP PUT (JSON body) | Updated Customer fields |
| API Client | `GET /debug` | HTTP GET (query params) | customerId, clientId, firstName, lastName, dateOfBirth, ssn, socialSecurityNum, tin, phoneNumber |
| API Client | `POST /account` | HTTP POST | Account object (accountNumber, routingNumber, balance) |
| API Client | `POST /account/{id}/deposit` | HTTP POST | Transaction amount |
| API Client | `POST /account/{id}/withdraw` | HTTP POST | Transaction amount |
| Seed data | `DataLoader` / `DataBuilder` | Application startup | Pre-built Customer and Account objects with realistic test PII |

## 6. Data Recipients (Egress)

| Recipient | Type | Data Shared | Mechanism | Safeguards |
|-----------|------|-------------|-----------|------------|
| **Salesforce** (`https://salesforce.com`) | Third-party CRM | Customer identifier + event string (contains `Customer.toString()` with PII) | HTTP POST via Apache HttpClient | Basic auth with encrypted credentials (Jasypt) |
| **API consumers** | External clients | Full Customer/Account JSON (all PII fields) | HTTP REST responses | None observed (no field-level filtering) |
| **Application logs** (SLF4J) | Internal logging | All PII fields including SSN, account numbers, credentials | `log.info(...)` calls | **None — PII logged in plaintext** |
| **MySQL database** | Internal persistence | All Customer, Account, Address entities | JDBC via Spring Data JPA | Encrypted password (Jasypt); no column-level encryption observed |

## 7. Data Storage & Retention

| Storage | Technology | Data Stored | Retention Policy | Encryption |
|---------|-----------|-------------|-----------------|------------|
| Primary database | HSQLDB (dev) / MySQL (prod) | Customer, Account, Address entities | Not defined in code | Password encrypted via Jasypt; no at-rest encryption observed |
| Filesystem | OS temp directory | Temporary files created by `DataBuilder` | Not defined; files not explicitly deleted | None |
| Application logs | SLF4J / console | PII, credentials, financial data | Not defined | None |

## 8. Technical & Organizational Measures (TOMs)

| Measure | Status | Details |
|---------|--------|---------|
| Encryption in transit | ⚠️ Partial | No TLS enforcement observed in code; Salesforce call uses `https://` |
| Encryption at rest | ❌ Not implemented | No database-level or column-level encryption |
| Access control | ❌ Weak | No authentication on customer/account endpoints; admin uses hardcoded password |
| Input validation | ❌ Missing | No validation on most request parameters |
| Data minimization | ❌ Not applied | Full entity objects returned/logged including all PII |
| Logging hygiene | ❌ Failing | SSN, account numbers, credentials logged in plaintext |
| Credential management | ⚠️ Partial | Jasypt encrypts some passwords but master key is hardcoded |
| CSRF protection | ❌ Missing | No CSRF tokens on state-changing endpoints |

## 9. Cross-Border Transfers

| Transfer | Destination | Safeguard |
|----------|-------------|-----------|
| Salesforce API | Salesforce infrastructure (likely US) | No transfer mechanism documented (SCCs, adequacy decision, etc.) |
| AWS S3 (configured but not active) | AWS region (unknown) | No transfer mechanism documented |

## 10. Data Processing Agreements

| Third Party | DPA Status |
|-------------|-----------|
| Salesforce | Not documented |
| AWS | Not documented |
| MySQL hosting provider | Not documented |

## 11. Risks & Compliance Gaps

| # | Risk | Severity | Mitigation Recommendation |
|---|------|----------|--------------------------|
| 1 | SSN and government IDs logged in plaintext | **Critical** | Implement log masking/redaction for all PII fields |
| 2 | No authentication on customer/account APIs | **Critical** | Add OAuth2/JWT authentication layer |
| 3 | Full PII sent to Salesforce without data minimization | **High** | Send only necessary identifiers, not full `toString()` |
| 4 | No retention policy defined | **High** | Define and implement data retention schedules |
| 5 | Hardcoded credentials in source code | **High** | Move to secrets manager (Vault, AWS Secrets Manager) |
| 6 | No DPAs with third-party processors | **High** | Execute DPAs with Salesforce, AWS, MySQL hosting provider |
| 7 | No cross-border transfer safeguards | **Medium** | Implement SCCs or verify adequacy decisions |
| 8 | Debug endpoints accept and persist arbitrary PII | **High** | Disable debug endpoints in production |

---

*Generated from code analysis of `shiftleft-java-demo` repository.*
