# Security Vulnerabilities & Privacy Risks Report
*Application:* **HelloShiftLeft** (Spring Boot 1.5.1)  
*Date:* 2026‑05‑06  

---

## Table of Contents
1. [Vulnerabilities](#vulnerabilities)  
   1.1. [Deserialization (Critical)](#deserialization-critical)  
   1.2. [Remote Code Execution – SpEL Injection (Critical)](#rce-spel-critical)  
   1.3. [Authentication Bypass (Critical)](#auth-bypass-critical)  
   1.4. [Arbitrary File Write (High)](#arbitrary-file-write-high)  
   1.5. [Reflected XSS (High)](#reflected-xss-high)  
   1.6. [Sensitive Data Leakage to Logs (High)](#sensitive-data-leakage-high)  
   1.7. [Excessive Logging (Medium)](#excessive-logging-medium)  
   1.8. [Security Misconfigurations (Medium)](#security-misconfigurations-medium)  
2. [Privacy Risks](#privacy-risks)  
3. [Recommendations & Remediation Summary](#recommendations)

---

## Vulnerabilities

| # | Name / Type | Severity | Affected File & Method | Description | Remediation |
|---|-------------|----------|------------------------|-------------|-------------|
| 1 | **Deserialization Vulnerability** | Critical | `src/main/java/io/shiftleft/controller/AdminController.java` – `doPostLogin` (and helper `isAdmin`) | The `auth` cookie is Base64‑decoded and deserialized without a whitelist. An attacker can supply a crafted serialized object that triggers arbitrary code execution (e.g., via gadget chains). | • Replace Java serialization with a signed JWT or other safe token format.<br>• If serialization must be used, enforce a strict class whitelist (`ObjectInputFilter`).<br>• Never deserialize data from untrusted sources. |
| 2 | **Remote Code Execution (SpEL Injection)** | Critical | `src/main/java/io/shiftleft/controller/SearchController.java` – `doGetSearch` | User‑controlled `foo` parameter is parsed by `SpelExpressionParser` and executed (`exp.getValue()`). This allows execution of arbitrary Java code (e.g., `new java.lang.ProcessBuilder(...).start()`). | • Do **not** evaluate raw user input with SpEL.<br>• If SpEL is required, use a whitelist of allowed expressions and disable method invocation (`StandardEvaluationContext.setMethodResolvers(Collections.emptyList())`).<br>• Validate/sanitize the `foo` parameter. |
| 3 | **Authentication Bypass** | Critical | `src/main/java/io/shiftleft/controller/AdminController.java` – `doPostLogin` | The login endpoint accepts a hard‑coded password (`shiftleftsecret`). If a valid admin cookie is already present, the method sets the session attribute without re‑validating the password, allowing an attacker to reuse or forge a cookie. | • Store passwords hashed with a strong algorithm (bcrypt, Argon2).<br>• Remove hard‑coded secrets; move them to a vault.<br>• Require proper credential verification on every request, not just cookie presence. |
| 4 | **Arbitrary File Write** | High | `src/main/java/io/shiftleft/controller/CustomerController.java` – `saveSettings` | The method trusts a `settings=` cookie, decodes Base64, validates an MD5 checksum, then writes the decoded content to a file under `static/`. No path sanitisation → attacker can craft a filename like `../../../../tmp/malicious.txt` and write arbitrary files. | • Restrict file writes to a dedicated, sandboxed directory.<br>• Validate the filename against a whitelist or use `java.nio.file.Path.normalize()` and check it stays inside the intended folder.<br>• Prefer server‑side storage (DB) over client‑controlled files. |
| 5 | **Reflected XSS** | High | `src/main/java/io/shiftleft/controller/CustomerController.java` – `debug` | Parameters (`firstName`, `lastName`, etc.) are concatenated into a `Customer` object and the object's `toString()` is returned directly. The response is not HTML‑escaped, allowing injection of `<script>` tags (demonstrated in README). | • Encode all user‑supplied data before reflecting it (`HtmlUtils.htmlEscape` or a templating engine).<br>• Implement a Content‑Security‑Policy (CSP) header.<br>• Remove the hidden `/debug` endpoint from production. |
| 6 | **Sensitive Data Leakage to Logs** | High | Multiple files (e.g., `AccountController.getAccountList`, `CustomerController.getCustomer`, `DataLoader.run`, `PatientController.getPatient`) | Logging statements (`log.info`) output full PII/PHI such as SSN, credit‑card numbers, AWS keys, DB passwords, and full customer/account objects. Logs may be stored unprotected and accessible to attackers. | • Redact or mask sensitive fields before logging.<br>• Use a structured logger with field‑level masking.<br>• Store logs in a secure, access‑controlled system (e.g., SIEM). |
| 7 | **Excessive Logging** | Medium | Same files as above | Verbose `info`/`debug` logs expose internal state, request headers, and even the master Jasypt password (`JASP Master Creds`). This increases the attack surface and makes log analysis noisy. | • Adopt log‑level hygiene: `INFO` for high‑level events, `DEBUG` only in development.<br>• Remove or mask debug statements that expose internal configuration. |
| 8 | **Security Misconfigurations** | Medium | `pom.xml` and various configuration classes | • Out‑of‑date dependencies (Spring Boot 1.5.1, Jackson 1.5.6, HttpClient 4.3.4) contain known CVEs.<br>• Hard‑coded secrets (`shiftleftsecret`, AWS keys logged, Jasypt master password printed).<br>• `@EnableEncryptableProperties` is used but the master password is logged (`log.info("JASP Master Creds is {}", encryptor);`). | • Upgrade to supported Spring Boot 3.x and latest library versions.<br>• Remove all hard‑coded secrets; fetch them from a secret manager (AWS Secrets Manager, HashiCorp Vault).<br>• Disable debug logging of configuration values in production. |
| 9 | **Missing Access Controls** | High | `AdminController` endpoints (`/admin/printSecrets`, `/admin/login`) and `CustomerController.debug` | Admin endpoints rely solely on the presence of a deserialized `AuthToken` with `isAdmin()`. No role‑based access control (RBAC) framework, no CSRF protection, and no rate limiting. | • Integrate Spring Security with proper authentication (e.g., OAuth2/JWT) and authorization annotations (`@PreAuthorize("hasRole('ADMIN')")`).<br>• Add CSRF tokens for state‑changing requests.<br>• Implement rate limiting / brute‑force protection. |
|10| **Insecure File Handling (Temp Files)**| Low | `src/main/java/io/shiftleft/data/DataBuilder.java` – `createCustomers` | Creates a temporary file in the default temp directory without restricting permissions; could be exploited in a multi‑tenant environment. | • Use `Files.createTempFile` with restrictive permissions (`PosixFilePermission`).<br>• Delete temp files after use. |

---

## Privacy Risks

| Risk | Affected Area | Why It Matters | Suggested Mitigation |
|------|----------------|----------------|----------------------|
| **PII/PHI Exposure via API** | `/customers/*`, `/patients`, `/account/*` | Endpoints return full objects containing SSN, passport numbers, credit‑card numbers, medical data. | Implement field‑level filtering (e.g., DTOs) and return only necessary data. |
| **Cookie‑Based Sensitive Data** | `auth` cookie (Base64‑encoded serialized `AuthToken`) | Cookie can be intercepted and reused; contains role information that grants admin rights. | Use HttpOnly, Secure flags; sign/encrypt the token; rotate secrets regularly. |
| **Logging of Secrets** | `DataLoader`, `AccountController`, `CustomerController` | Logs contain AWS access keys, DB passwords, Jasypt master password, and full customer records. | Redact secrets before logging; move secrets to a vault; configure log‑masking. |
| **File‑Based Preference Storage** | `/saveSettings` & `/loadSettings` | Preferences are stored on the server’s filesystem based on a client‑controlled cookie, potentially leaking user data. | Store preferences server‑side (DB) and protect with proper ACLs. |
| **Debug Endpoint Data Dump** | `/debug` | Returns the entire `Customer` object (including PII) and reflects request parameters. | Remove or protect the endpoint; require authentication; sanitize output. |

---

## Recommendations & Remediation Summary

1. **Replace Java Serialization** – Move to signed JWTs or encrypted tokens; never deserialize untrusted data.
2. **Sanitize & Validate All Input** – Especially for SpEL, file paths, and query parameters.
3. **Implement Proper Authentication & Authorization** – Use Spring Security with BCrypt‑hashed passwords, role‑based checks, CSRF protection, and secure cookies.
4. **Secure Logging** – Mask/redact PII/credentials, lower log verbosity in production, and store logs securely.
5. **Upgrade Dependencies** – Bring Spring Boot, Jackson, HttpClient, and other libraries to supported versions to obtain security patches.
6. **Remove Hard‑Coded Secrets** – Store passwords, API keys, and other credentials in a secret manager; never log them.
7. **Restrict File System Access** – Validate filenames, confine writes to a sandbox directory, and set strict file permissions.
8. **Apply Content‑Security‑Policy & XSS Defenses** – Escape all reflected data, use templating engines, and add CSP headers.
9. **Add Rate Limiting & Brute‑Force Protection** – Prevent credential‑guessing attacks on `/admin/login`.
10. **Conduct Regular Security Audits** – Include static analysis, dependency‑check scans, and penetration testing as part of CI/CD.

--- 

*End of Report*