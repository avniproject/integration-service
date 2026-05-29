# Plan: Configurable Payment Rates via Avni PaymentRateChart

## Context

Payment amounts are hardcoded in SQL queries — every rate change requires editing SQL directly on the Avni prod DB, which is risky. The new approach: rates are stored as Avni encounters (`PaymentRate`) with an effective date, so the DI/client team can update them in Avni with proper history tracking. The integration service reads the current effective rate at the start of each job run. The same data powers the Avni payment summary dashboard.

---

## Part 1 — Avni Setup (user to implement in Avni app)

### Location
| Field | Value |
|---|---|
| LocationType | Account-Details |
| Location Name | PanIndia |
| Type | Account-Details |

### Subject Type
| Field | Value |
|---|---|
| Name | PaymentRateChart |
| Location Applicable | Account-Details |
| Type | Individual |

### Encounter Type
| Field | Value |
|---|---|
| Name | PaymentRate |
| Subject Type | PaymentRateChart |

### PaymentRate Form Fields
| Field Name | Type | Notes |
|---|---|---|
| `Effective Date` | Date | Date from which all rates in this entry apply |
| `Odisha Rate` | Numeric | Rate in INR per approved submission for od_IN users |
| `Andhra Pradesh Rate` | Numeric | Rate in INR per approved submission for te_IN users |

> One encounter = one rate revision event. Both states are always updated together — no risk of one state being missed.

### Initial Data Entry
Register one `PaymentRateChart` subject at PanIndia. Add one `PaymentRate` encounter:
- Effective Date: (start date of the program), Odisha Rate: 300, Andhra Pradesh Rate: 500

---

## Part 2 — Avni Custom Query (add to `custom_query` on Avni prod DB)

**Name:** `dil_current_payment_rates`

Fetches the single most recent `PaymentRate` encounter with Effective Date ≤ today. Returns two rows — one per state — so the integration service can look up rate by locale.

> Note: Placeholder field references (`<odisha_rate_obs>`, `<ap_rate_obs>`, `<effective_date_obs>`) must be replaced with actual concept UUIDs/observation references after the PaymentRate form is created in Avni.

```sql
WITH latest AS (
    SELECT <odisha_rate_obs>     AS od_rate,
           <ap_rate_obs>         AS ap_rate
    FROM encounter e
        JOIN encounter_type et   ON et.id = e.encounter_type_id AND et.is_voided = false
        JOIN individual i        ON i.id = e.individual_id      AND i.is_voided = false
        JOIN subject_type st     ON st.id = i.subject_type_id
    WHERE et.name                = 'PaymentRate'
      AND st.name                = 'PaymentRateChart'
      AND e.is_voided             = false
      AND e.organisation_id      = :org_id
      AND <effective_date_obs>   <= CURRENT_DATE
    ORDER BY <effective_date_obs> DESC
    LIMIT 1
)
SELECT 'od_IN' AS locale, od_rate::TEXT AS rate FROM latest
UNION ALL
SELECT 'te_IN' AS locale, ap_rate::TEXT AS rate FROM latest
```

**Result columns:**
- Column 0: `locale` (`od_IN` or `te_IN`)
- Column 1: `rate` (numeric as text, e.g. `"300"`)

**Simpler than per-state partitioning** — one `LIMIT 1` on the latest encounter, no `ROW_NUMBER`/`PARTITION BY` needed.

---

## Part 3 — Integration Service Code Changes

### 3a. `avni/src/main/java/org/avni_integration_service/avni/domain/CustomQueryRequest.java`

Add a constructor that takes only a query name (no flow_id needed for the rate query):

```java
public CustomQueryRequest(String name) {
    this.name = name;
    this.queryParams = null;
}
```

### 3b. New: `wati/src/main/java/org/avni_integration_service/wati/service/WatiRateService.java`

Spring `@Service` that:
1. Calls `avniQueryRepository.invokeCustomQuery(new CustomQueryRequest("dil_current_payment_rates"))`
2. Parses the response rows into `Map<String, Integer>` (locale → rate)
3. Exposes `void loadRates()` (called at job start) and `int getRate(String locale)` (called per row, returns rate or fallback default)

```java
@Service
public class WatiRateService {
    private Map<String, Integer> ratesByLocale = new HashMap<>();

    public void loadRates() {
        // fetch from Avni, populate ratesByLocale
    }

    public int getRate(String locale) {
        return ratesByLocale.getOrDefault(locale, 0);
    }
}
```

### 3c. `wati/src/main/java/org/avni_integration_service/wati/job/AvniWatiMainJob.java`

At the start of each scheduled run, call `watiRateService.loadRates()` before processing flows.

### 3d. `wati/src/main/java/org/avni_integration_service/wati/worker/WatiFlowWorker.java`

Inject `WatiRateService`. Modify `buildParametersJson` to:
- For param `amount`: use `watiRateService.getRate(locale)` as the value (flat amount for weekly_survey)
- For param `payment_amount`: find `approved_count` in the result row, compute `approved_count × watiRateService.getRate(locale)`

```java
private String buildParametersJson(List<Object> row, String[] paramNames, String locale) {
    // existing logic ...
    if ("amount".equals(paramName)) {
        value = String.valueOf(watiRateService.getRate(locale));
    } else if ("payment_amount".equals(paramName)) {
        int approvedIdx = Arrays.asList(paramNames).indexOf("approved_count");
        if (approvedIdx >= 0 && row.get(3 + approvedIdx) != null) {
            int approvedCount = Integer.parseInt(row.get(3 + approvedIdx).toString());
            value = String.valueOf(approvedCount * watiRateService.getRate(locale));
        }
    }
    // ...
}
```

Update the call site in `processRow` to pass `locale`.

---

## Part 4 — `docs/dil-wati-staging-setup.sql`

Add the `dil_current_payment_rates` custom query INSERT (with the finalized SQL once Avni form is created).

---

## What Changes for Support Team Going Forward

To update rates:
1. Open Avni → PaymentRateChart subject → add a new `PaymentRate` encounter
2. Fill in: Effective Date, Odisha Rate, Andhra Pradesh Rate
3. No code change, no SQL editing — the integration service picks it up on the next job run (fetches latest encounter with Effective Date ≤ today)

---

## Key Files

| File | Change |
|---|---|
| `avni/.../CustomQueryRequest.java` | Add no-flowId constructor |
| `wati/.../WatiRateService.java` | New service — fetch rates from Avni |
| `wati/.../AvniWatiMainJob.java` | Call `loadRates()` at job start |
| `wati/.../WatiFlowWorker.java` | Use `WatiRateService` in `buildParametersJson` |
| `docs/dil-wati-staging-setup.sql` | Add `dil_current_payment_rates` query INSERT |

---

## Verification

1. Create a test PaymentRate encounter in Avni staging with a known cost
2. Run the integration job — check `wati_message_request` records show the correct `payment_amount` and `amount` values
3. Add a newer PaymentRate encounter with a different cost and future effective date — confirm old rate still used until that date, then new rate kicks in after
4. Run `./gradlew :wati:test` — add unit tests for `WatiRateService.getRate()` and the override logic in `buildParametersJson`
