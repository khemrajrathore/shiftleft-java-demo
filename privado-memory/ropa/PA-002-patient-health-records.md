# Processing Activity: Patient Health Records Management

## 1. Overview

| Field | Value |
|-------|-------|
| **PA Name** | Patient Health Records Management |
| **PA ID** | PA-002 |
| **Business Function** | Healthcare Records |
| **Description** | Storage and retrieval of patient health records including medical vitals, biometric data, and medication information. Records are seeded at application startup and served via a read-only REST endpoint. |
| **Data Controller** | Application Operator (organization deploying this service) |
| **Last Updated** | 2026-05-07 |

## 2. Purpose & Legal Basis

| Purpose | Legal Basis | Justification |
|---------|-------------|---------------|
| Storage and retrieval of patient medical records | Explicit consent (Art. 6(1)(a) / Art. 9(2)(a) GDPR) | Health data is a special category under Art. 9; requires explicit consent or another Art. 9(2) exemption |
| Provision of healthcare services | Art. 9(2)(h) GDPR — healthcare purposes | Processing necessary for medical diagnosis, provision of health care, or treatment |

> ⚠️ **Note:** The codebase does not implement any consent collection mechanism. This is a compliance gap — see Risks section.

## 3. Data Subjects

| Data Subject Type | Description | Estimated Volume |
|-------------------|-------------|-----------------|
| **Patients** | Individuals whose health and medical data is stored in the system | Variable (1 seed record in demo; scales with deployment) |

## 4. Data Elements (Personal Data Processed)

| # | Data Element | Category | Sensitivity | Source | Storage | Transmitted To |
|---|-------------|----------|-------------|--------|---------|---------------|
| 1 | patientId | Identity | High | Auto-generated | HSQLDB / MySQL | API responses, application logs |
| 2 | patientFirstName | Name | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |
| 3 | patientLastName | Name | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |
| 4 | patientDateOfBirth | Identity / Health | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |
| 5 | patientWeight | Health / Biometric | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |
| 6 | patientHeight | Health / Biometric | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |
| 7 | medications | Health / Medical | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |
| 8 | body_temp_deg_c | Health / Vitals | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |
| 9 | heartRate | Health / Vitals | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |
| 10 | pulse_rate | Health / Vitals | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |
| 11 | bpDiastolic | Health / Vitals | High | Seed data / API input | HSQLDB / MySQL | API responses, application logs |

## 5. Data Sources (Ingress)

| Source | Endpoint | Method | Data Collected |
|--------|----------|--------|---------------|
| Seed data (startup) | `DataLoader` → `DataBuilder.createPatients()` | Application startup | Pre-built Patient objects with realistic health data |
| API Client (read-only) | `GET /patients` | HTTP GET | No new data ingested — read-only endpoint |

> **Note:** The current codebase has no create/update/delete endpoints for patients. Data enters only through the seed loader.

## 6. Data Recipients (Egress)

| Recipient | Type | Data Shared | Mechanism | Safeguards |
|-----------|------|-------------|-----------|------------|
| **API consumers** | External clients | Full Patient JSON (all health data fields) | HTTP REST response (`GET /patients`) | **None** — no authentication, no field filtering |
| **Application logs** (SLF4J) | Internal logging | First patient record logged at retrieval (`log.info`) | `PatientController.getPatient()` | **None — health data logged in plaintext** |
| **MySQL database** | Internal persistence | All Patient entity fields | JDBC via Spring Data JPA | Encrypted password only; no column-level encryption |

## 7. Data Storage & Retention

| Storage | Technology | Data Stored | Retention Policy | Encryption |
|---------|-----------|-------------|-----------------|------------|
| Primary database | HSQLDB (dev) / MySQL (prod) | Patient entity (all health/biometric fields) | Not defined in code | No at-rest encryption |
| Application logs | SLF4J / console | Patient health data | Not defined | None |

## 8. Technical & Organizational Measures (TOMs)

| Measure | Status | Details |
|---------|--------|---------|
| Access control | ❌ Missing | `GET /patients` is unauthenticated — anyone can retrieve all patient records |
| Data minimization | ❌ Not applied | Full entity including all health fields returned to any caller |
| Consent management | ❌ Not implemented | No consent collection or tracking mechanism for special category data |
| Encryption at rest | ❌ Not implemented | Health data stored unencrypted in database |
| Encryption in transit | ⚠️ Unknown | No TLS configuration observed in code |
| Audit logging | ⚠️ Partial | Access is logged but log itself contains PII/health data |
| Pseudonymization | ❌ Not implemented | Real identifiers used throughout |

## 9. Cross-Border Transfers

| Transfer | Destination | Safeguard |
|----------|-------------|-----------|
| None identified | Patient data is not sent to external services | N/A |

## 10. Data Processing Agreements

| Third Party | DPA Status |
|-------------|-----------|
| MySQL hosting provider | Not documented |

## 11. DPIA Requirement

> **A Data Protection Impact Assessment (DPIA) is likely required** under Art. 35 GDPR because this processing involves:
> - Special category data (health data) under Art. 9
> - Large-scale processing potential (all patient records returned in bulk)
> - No access controls or safeguards in place

## 12. Risks & Compliance Gaps

| # | Risk | Severity | Mitigation Recommendation |
|---|------|----------|--------------------------|
| 1 | Health data exposed via unauthenticated endpoint | **Critical** | Implement role-based access control; require authentication for `/patients` |
| 2 | No consent mechanism for special category data | **Critical** | Implement explicit consent collection and tracking per Art. 9(2)(a) |
| 3 | Health data logged in plaintext | **High** | Implement log redaction for all health/biometric data |
| 4 | All patient records returned in bulk (no pagination/filtering) | **High** | Implement pagination, field filtering, and purpose-based access |
| 5 | No data retention policy | **High** | Define retention schedule aligned with healthcare regulations |
| 6 | No pseudonymization or anonymization | **Medium** | Implement pseudonymization for non-primary-care uses |
| 7 | No DPIA conducted | **High** | Conduct DPIA before processing health data at scale |
| 8 | No encryption at rest for health data | **High** | Implement column-level encryption for sensitive health fields |

---

*Generated from code analysis of `shiftleft-java-demo` repository.*
