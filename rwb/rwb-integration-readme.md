## Purpose

RWB integration service is aimed at identifying Field Users, who meet all the following criteria:
- Didn't create or edit any registrations or encounters in the past X days
- User belong to 'Primary Users' user group
- Should consider users who have in their catchment
  - Atleast one "Workorder", without a "Workorder Endline" encounter OR
  - No "Workorder"
- The Status(Success / Failure) to Nudge a User should be Recorded for Reporting purposes

For users thus identified, we are supposed to Nudge them on their Registered Mobile numbers on Whatsapp, through Glific, for taking action on the Avni Gramin App.

## Design

RWB integration service is built to service multiple Production organisations using a single integration instance. To do this, we make use of RWBContextProvider, a ThreadLocal store, which stores the IntegrationSystemInstance information corresponding to each Production Organisation.

## How to setup a new IntegrationSystemInstance for a new RWB organisation

Integration service provides the ability to download implementation-specific configuration files from one environment/system and upload it to another environment.
“Avni integration” Postman collection for Integration service Bundle export-import is available [here](https://drive.google.com/drive/folders/1XjWYQsLUCJuPxwDbtHvTreHrsF3VQJId). 
Download the postman collection and using Postman App / Webapp, import the postman collection and create required RWB-Prod and RWB-Staging Postman Environments.

### How to Configure the Admin users to use specific working_integration_system
**Please ensure that you edit the admin user to point to the right working_integration_system. This can be done by updating through SQL command, or easier still by logging into the Integration-Admin-app and editing user to set it to required working_integration_system.**

<img src="static/img/Int-admin-user-edit-working-system.png" alt="Reference screenshot for Integration Admin User, Edit Working Integration System" style="width:500px;"/>

Steps for Integration Bundle migration from UAT to Prod are as follows using the “Avni integration” postman collection.

####  Download the integration bundle from UAT org / Staging env org
- Log in to [RWB Staging integration](https://etl-staging.rwb.avniproject.org) using the rwb-int-staging admin user with the “int login” API request
- Invoke the "int current user" API request and confirm that the working_integration_system is as desired
- Invoke “int Bundle export” API request, to download the bundle
- Logout using the “int logout” API request


####  Upload the integration bundle to Prod env target org
- Log in using [RWB Prod integration](https://etl-app.rwb.avniproject.org) using the rwb-int-prod user admin with the  “int login” API request
- Invoke the "int current user" API request and confirm that the working_integration_system is as desired
- Invoke “int Bundle import” API request, attaching the downloaded bundle as payload “bundleZip” field
- Logout with the “int logout” API request

### Update the value for secret properties

Invoke below sql commands with appropriate replacements for updating the secret properties values.

```sql
UPDATE public.integration_system_config SET value = '<user>@<org>'::varchar(10000) WHERE key = 'avni_user' and  integration_system_id = '<rwb_org_system_id>'::integer;
UPDATE public.integration_system_config SET value = 'https://<sub-domain>.rwb.avniproject.org/'::varchar(10000) WHERE key = 'avni_api_url' and  integration_system_id = '<rwb_org_system_id>'::integer;
UPDATE public.integration_system_config SET value = '<password>'::varchar(10000) WHERE key = 'avni_password' and  integration_system_id = '<rwb_org_system_id>'::integer;
```

### Restart the RWB Target env Integration service, where Integration bundle upload and secret updates were done

```
#> sudo systemctl restart avni-int-service_appserver.service
```






