# Bahmni config: "View in Avni" link on the patient dashboard

Adds an **Avni** section to the Bahmni clinical patient dashboard with a link to the person's page in the Avni web app. The link is built from the Avni subject UUID that the integration writes onto the Bahmni patient (person attribute `avni_subject_uuid`, type `ceddfccf-7913-4a63-b60e-d3114f867e4b`).

| File | Goes into the JSS Bahmni config (`jss-config`) |
|---|---|
| `customDisplayControl/js/avniLink.js` | Append to `openmrs/apps/customDisplayControl/js/customControl.js` |
| `customDisplayControl/views/avniLink.html` | Copy to `openmrs/apps/customDisplayControl/views/avniLink.html` |
| `dashboard-section.json` | Add the `avniLink` entry under `dashboard` → `sections` in `openmrs/apps/clinical/dashboard.json` |

Per environment, change only `avniWebAppUrl` in the section config:

| Environment | `avniWebAppUrl` |
|---|---|
| Prerelease | `https://prerelease.avniproject.org` |
| Production | `https://app.avniproject.org` |

Notes
- Doctors need an Avni web login in the JSSCP org to see the page the link opens.
- After changing config files, users may need a hard refresh (Ctrl+Shift+R) for Bahmni to pick up the new JavaScript.
- Uses Bahmni's custom display control mechanism (module `bahmni.common.displaycontrol.custom`), so no change to Bahmni's own code.
