# Processing Activity: Admin Authentication & System Configuration

## 1. Overview

| Field | Value |
|-------|-------|
| **PA Name** | Admin Authentication & System Configuration |
| **PA ID** | PA-003 |
| **Business Function** | System Administration / Security |
| **Description** | Authentication of administrative users via password-based login, session management with serialized auth tokens, user settings persistence via cookies and filesystem, and system configuration loading (AWS, MySQL, Salesforce credentials). |
| **Data Controller** | Application Operator (organization deploying this service) |
| **Last Updated** | 2026-05-07 |

## 2. Purpose & Legal Basis

| Purpose | Legal Basis | Justification |
|---------|-------------|---------------|
| Admin authentication and access control | Legitimate interest (Art. 6(1)(f) GDPR) | Necessary to secure administrative functions |
| User settings persistence | Contract performance / Legitimate interest | Enables user preferences and configuration |
| System credential management | Legitimate interest (Art. 6(1)(f) GDPR) | Required for connecting to third-party infrastructure |

## 3. Data Subjects

| Data Subject Type | Description | Estimated Volume |
|-------------------|-------------|-----------------|
| **Administrators** | Users authenticating to admin endpoints | Small (typically < 10) |
| **System service accounts** | AWS, MySQL, Salesforce service credentials | 3 service accounts |

## 4. Data Elements (Personal Data Processed)

| # | Data Element | Category | Sensitivity | Source | Storage | Transmitted To |
|---|-------------|----------|-------------|--------|---------|---------------|
| 1 | Admin password (hardcoded) | Authentication | High | Hardcoded in `AdminController` | Source code | Compared against request body |
| 2 | AuthToken (serialized) | Authentication | High | Generated at login | HTTP cookie (`auth`) + HTTP session | Client browser (cookie), server memory (session) |
| 3 | Settings cookie data | Configuration | Medium | Client cookie header | Filesystem (`static/` directory) | Response cookie header |
| 4 | AWS access key | Credential | **Critical** | `application-aws.properties` | Config file | Application logs (plaintext!) |
| 5 | AWS secret key | Credential | **Critical** | `application-aws.properties` | Config file | Application logs (plaintext!) |
| 6 | MySQL username | Credential | High | `application-mysql.properties` | Config file | Application logs |
| 7 | MySQL password (encrypted) | Credential | **Critical** | `application-mysql.properties` | Config file (encrypted) | Application logs (decrypted plaintext!) |
| 8 | Salesforce username | Credential | High | `application-sfdc.properties` | Config file | Application logs, HTTP POST to Salesforce |
| 9 | Salesforce password (encrypted) | Credential | **Critical** | `application-sfdc.properties` | Config file (encrypted) | Application logs (decrypted plaintext!) |
| 10 | Jasypt master password | Credential | **Critical** | `application.properties` | Config file / JVM arg | Application logs |
| 11 | PrivadoUser passportNumber | Identity | **Critical** | `PrivadoUser` object in `AdminController.isAdmin()` | In-memory only | Potentially exposed via deserialization |
| 12 | PrivadoUser creditCardNumber | Financial | **Critical** | `PrivadoUser` object in `AdminController.isAdmin()` | In-memory only | Potentially exposed via deserialization |

## 5. Data Sources (Ingress)

| Source | Endpoint | Method | Data Collected |
|--------|----------|--------|---------------|
| Admin user | `POST /admin/login` | HTTP POST (body: `password=...`) | Login password |
| Client cookie | `GET /admin/printSecrets` | HTTP GET (cookie: `auth=...`) | Base64-encoded serialized AuthToken |
| Client cookie | `GET /saveSettings`, `GET /loadSettings` | HTTP GET (cookie header) | Base64-encoded settings + MD5 |
| Config files | Application startup (`DataLoader`) | File read | AWS, MySQL, SFDC credentials |

## 6. Data Recipients (Egress)

| Recipient | Type | Data Shared | Mechanism | Safeguards |
|-----------|------|-------------|-----------|------------|
| **Admin user (browser)** | End user | `auth` cookie (serialized AuthToken), `calculations.csv` file content | HTTP response (cookie + body) | Cookie not marked `Secure` or `HttpOnly` |
| **Application logs** (SLF4J) | Internal | AWS keys, MySQL password (decrypted), SFDC password (decrypted), Jasypt master password | `log.info(...)` in `DataLoader` | **None — all credentials logged in plaintext** |
| **Filesystem** | Local server | Settings data written to `static/` directory | `FileOutputStream` | MD5 validation only (weak) |

## 7. Data Storage & Retention

| Storage | Technology | Data Stored | Retention Policy | Encryption |
|---------|-----------|-------------|-----------------|------------|
| HTTP session | In-memory (Spring) | AuthToken object | Session timeout (not configured) | None |
| HTTP cookie | Client browser | Base64-encoded serialized AuthToken | Browser session / not set | None (Base64 is encoding, not encryption) |
| Filesystem | `static/` directory | User settings data | Not defined; files persist indefinitely | None |
| Config files | Properties files on classpath | AWS/MySQL/SFDC credentials | Indefinite (in source control) | Partial (Jasypt for passwords; keys in plaintext) |

## 8. Technical & Organizational Measures (TOMs)

| Measure | Status | Details |
|---------|--------|---------|
| Authentication strength | ❌ Weak | Hardcoded password (`shiftleftsecret`); no MFA, no account lockout |
| Session security | ❌ Weak | Serialized Java object in cookie; vulnerable to deserialization attacks |
| Cookie security | ❌ Missing | `auth` cookie not marked `Secure`, `HttpOnly`, or `SameSite` |
| Credential storage | ⚠️ Partial | Jasypt encrypts DB/SFDC passwords but master key is hardcoded; AWS keys in plaintext |
| Credential logging | ❌ Failing | All credentials (including decrypted passwords) logged via `log.info` |
| File integrity | ⚠️ Weak | Settings validated with MD5 only (cryptographically broken) |
| Path traversal protection | ❌ Missing | `saveSettings` writes to filesystem without proper path sanitization |

## 9. Cross-Border Transfers

| Transfer | Destination | Safeguard |
|----------|-------------|-----------|
| Salesforce credentials sent to Salesforce API | Salesforce infrastructure (likely US) | HTTPS used but no DPA documented |
| AWS credentials configured | AWS (region unknown) | Not actively used in code but credentials present |

## 10. Risks & Compliance Gaps

| # | Risk | Severity | Mitigation Recommendation |
|---|------|----------|--------------------------|
| 1 | All service credentials logged in plaintext at startup | **Critical** | Remove all credential logging; use structured logging with PII redaction |
| 2 | Hardcoded admin password in source code | **Critical** | Move to identity provider (LDAP, OAuth2, SAML) |
| 3 | Java deserialization vulnerability in auth cookie | **Critical** | Replace serialized Java objects with signed JWT tokens |
| 4 | Jasypt master password hardcoded in properties | **Critical** | Inject via environment variable or secrets manager |
| 5 | Auth cookie lacks security flags | **High** | Set `Secure`, `HttpOnly`, `SameSite=Strict` flags |
| 6 | Arbitrary file write via `/saveSettings` | **High** | Implement path sanitization and restrict write directory |
| 7 | MD5 used for integrity checking | **Medium** | Replace MD5 with SHA-256 or HMAC |
| 8 | No session timeout configured | **Medium** | Configure session timeout (e.g., 30 minutes) |
| 9 | AWS credentials in plaintext config files | **High** | Use IAM roles or AWS Secrets Manager |

---

*Generated from code analysis of `shiftleft-java-demo` repository.*
