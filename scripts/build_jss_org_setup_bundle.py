#!/usr/bin/env python3
"""
Build the one-time Avni setup bundle for a JSSCP org that is being connected to Bahmni.

The bundle creates, in the target Avni org:
  - concept   "Bahmni Entity UUID" (Text)
  - encounter type "Bahmni Patient Registration" (the bridge record linking an Avni
    individual to a Bahmni patient - every Bahmni -> Avni visit sync depends on it)
  - its form + cancellation form, mapped to the org's "Individual" subject type

The integration finds these by NAME, so the names in the templates must not change.

Usage:
    python3 scripts/build_jss_org_setup_bundle.py --subject-type-uuid <uuid> [--out <zip>]

Get the subject type UUID from the target org (Avni DB):
    SELECT uuid FROM subject_type
    WHERE name = 'Individual' AND NOT is_voided
      AND organisation_id = (SELECT id FROM organisation WHERE name = '<org name>');

Then upload the zip in Avni: App Designer / Admin -> Upload -> type "Metadata Zip".
"""

import argparse
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEMPLATE_DIR = os.path.join(ROOT, "JSS Bahmni integration", "avni_org_setup", "bundle")
DEFAULT_OUT = os.path.join(ROOT, "JSS Bahmni integration", "avni_org_setup", "jss-org-setup-bundle.zip")
PLACEHOLDER = "{{SUBJECT_TYPE_UUID}}"
UUID_RE = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--subject-type-uuid", required=True, help="UUID of the 'Individual' subject type in the target org")
    parser.add_argument("--out", default=DEFAULT_OUT, help="Output zip path")
    return parser.parse_args()


def main():
    args = parse_args()
    if not UUID_RE.match(args.subject_type_uuid):
        sys.exit(f"Not a UUID: {args.subject_type_uuid}")

    with zipfile.ZipFile(args.out, "w", zipfile.ZIP_DEFLATED) as bundle:
        for dirpath, _, filenames in os.walk(TEMPLATE_DIR):
            for filename in sorted(filenames):
                path = os.path.join(dirpath, filename)
                arcname = os.path.relpath(path, TEMPLATE_DIR)
                with open(path, encoding="utf-8") as f:
                    content = f.read().replace(PLACEHOLDER, args.subject_type_uuid)
                bundle.writestr(arcname, content)
                print(f"  added {arcname}")

    print(f"Bundle written to {args.out}")


if __name__ == "__main__":
    main()
