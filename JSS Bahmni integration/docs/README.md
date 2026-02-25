# JSS Avni-Bahmni Integration - Documentation Hub

Welcome! This folder contains all documentation for the Avni-Bahmni integration project. The documentation is organized by purpose to help you find what you need quickly.

---

## 📁 Documentation Structure

```
docs/
├── 01-Project-Overview/           ← Scoping & project definition
├── 02-Status-Reports/             ← Executive summary, project status
├── 03-Technical-Documentation/    ← Architecture, configuration, technical details
├── 04-Process-Guides/             ← Step-by-step implementation guides
├── 05-Reference/                  ← Known issues, troubleshooting
└── README.md                       ← You are here
```

---

## 🎯 Quick Start by Role

### 👔 Project Managers / Leadership
Start with the **Status Reports** folder:
1. **[02-Status-Reports/01-executive-summary.md](02-Status-Reports/01-executive-summary.md)** (5 min) - One-page status overview
2. **[02-Status-Reports/02-integration-status-summary.md](02-Status-Reports/02-integration-status-summary.md)** (10 min) - Detailed project report with timeline & resources

### 👨‍💻 Developers / Technical Team
1. **Start Here:** [03-Technical-Documentation/01-avni-bahmni-integration.md](03-Technical-Documentation/01-avni-bahmni-integration.md) - Architecture overview
2. **Then:** [03-Technical-Documentation/02-configuration-reference.md](03-Technical-Documentation/02-configuration-reference.md) - How to set up
3. **For Details:** [03-Technical-Documentation/03-technical-guide.md](03-Technical-Documentation/03-technical-guide.md) - Technical reference

### 🛠️ DevOps / Database Administrators
- **[03-Technical-Documentation/02-configuration-reference.md](03-Technical-Documentation/02-configuration-reference.md)** - Database setup & constants
- **[02-Status-Reports/02-integration-status-summary.md](02-Status-Reports/02-integration-status-summary.md)** - What migrations to run

### 🔍 Troubleshooting / Debugging
1. **[05-Reference/01-blocking-issues.md](05-Reference/01-blocking-issues.md)** - Known issues & solutions
2. **[../../memory/MEMORY.md](../../memory/MEMORY.md)** - CSV format tips, critical learnings, error reference

---

## 📚 Browse by Folder

### 📋 [01-Project-Overview](01-Project-Overview/)
**Project scoping and requirements definition**

| Document | Purpose |
|----------|---------|
| [01-scoping-overview.md](01-Project-Overview/01-scoping-overview.md) | High-level project scope and integration approach |
| [02-scoping-complete.md](01-Project-Overview/02-scoping-complete.md) | Complete entity lists and scope details |
| [03-forms-scoping.md](01-Project-Overview/03-forms-scoping.md) | Forms and data elements in scope |

**Best For:** Understanding what's included in the integration, project boundaries

---

### 📊 [02-Status-Reports](02-Status-Reports/)
**Project status, progress tracking, and deliverables**

| Document | Purpose |
|----------|---------|
| [01-executive-summary.md](02-Status-Reports/01-executive-summary.md) | 1-page status with timeline & health indicators |
| [02-integration-status-summary.md](02-Status-Reports/02-integration-status-summary.md) | Comprehensive progress report with all tasks |
| [03-implementation-summary.md](02-Status-Reports/03-implementation-summary.md) | What code has been implemented, testing status |

**Best For:** Project tracking, reporting to stakeholders, understanding progress

---

### 🔧 [03-Technical-Documentation](03-Technical-Documentation/)
**Architecture, configuration, and implementation details**

| Document | Purpose |
|----------|---------|
| [01-avni-bahmni-integration.md](03-Technical-Documentation/01-avni-bahmni-integration.md) | System architecture, components, data flow |
| [02-configuration-reference.md](03-Technical-Documentation/02-configuration-reference.md) | Database constants, setup requirements |
| [03-technical-guide.md](03-Technical-Documentation/03-technical-guide.md) | Detailed technical implementation guide |
| [04-mapping-configuration.md](03-Technical-Documentation/04-mapping-configuration.md) | How to configure field mappings |

**Best For:** Understanding system design, developers onboarding, deployment setup

---

### 📖 [04-Process-Guides](04-Process-Guides/)
**Step-by-step implementation procedures**

| Document | Purpose |
|----------|---------|
| [01-forms-extraction.md](04-Process-Guides/01-forms-extraction.md) | How to extract form definitions from Avni |
| [02-anc-to-bahmni-conversion.md](04-Process-Guides/02-anc-to-bahmni-conversion.md) | Practical example: Converting ANC form to Bahmni |
| [03-bahmni-csv-structure.md](04-Process-Guides/03-bahmni-csv-structure.md) | Bahmni CSV upload format and requirements |
| [04-complete-workflow.md](04-Process-Guides/04-complete-workflow.md) | End-to-end integration workflow |
| [05-bahmni-to-avni-conversion.md](04-Process-Guides/05-bahmni-to-avni-conversion.md) | Converting from Bahmni back to Avni |

**Best For:** Implementing new forms, understanding the conversion process, step-by-step procedures

---

### 🚨 [05-Reference](05-Reference/)
**Known issues, troubleshooting, and reference materials**

| Document | Purpose |
|----------|---------|
| [01-blocking-issues.md](05-Reference/01-blocking-issues.md) | Current known issues and their status |
| [02-documentation-index.md](05-Reference/02-documentation-index.md) | Archive: Older documentation references |

**Best For:** Debugging, understanding workarounds, checking known issues

---

## 🔗 Additional Resources

### 💾 Artifacts (in parent folder)
- **CSV dumps** - Sample CSV files for Bahmni imports
- **avni_bundle** - Avni form bundles with metadata
- **bahmni_import** - Bahmni import materials
- **testing** - Test files and fixtures

### 📝 Memory & Learnings
- **[../../memory/MEMORY.md](../../memory/MEMORY.md)** - Critical learnings across sessions:
  - Bahmni CSV format requirements
  - Concept class discovery
  - Answer mapping solutions
  - Common errors and fixes
  - Key UUIDs reference

---

## 📊 Current Project Status

| Status | Component |
|--------|-----------|
| ✅ FIXED | HTTP Encoding (UTF-8 charset) |
| ✅ FIXED | Provider Configuration (UUID) |
| ✅ WORKING | 22/25 ANC Observations |
| 🔴 BLOCKED | Patient Lookup (identifier mismatch) |
| ✅ CREATED | 6+ Database Migrations |
| ⏳ PENDING | Operational Documentation |

**Timeline to Production:** 18-27 days (3.5-5.5 weeks)

---

## ❓ Common Questions

### "I need to present status to leadership - where do I start?"
→ Read [02-Status-Reports/01-executive-summary.md](02-Status-Reports/01-executive-summary.md) first, then [02-integration-status-summary.md](02-Status-Reports/02-integration-status-summary.md)

### "I'm a new developer - how do I understand the system?"
→ Start with [03-Technical-Documentation/01-avni-bahmni-integration.md](03-Technical-Documentation/01-avni-bahmni-integration.md), then [02-configuration-reference.md](03-Technical-Documentation/02-configuration-reference.md)

### "How do I implement a new form integration?"
→ Follow [04-Process-Guides/02-anc-to-bahmni-conversion.md](04-Process-Guides/02-anc-to-bahmni-conversion.md) as an example

### "What's failing and how do I debug it?"
→ Check [05-Reference/01-blocking-issues.md](05-Reference/01-blocking-issues.md) for known issues, then [../../memory/MEMORY.md](../../memory/MEMORY.md) for common errors

### "What are the Bahmni CSV upload requirements?"
→ See [04-Process-Guides/03-bahmni-csv-structure.md](04-Process-Guides/03-bahmni-csv-structure.md) and [../../memory/MEMORY.md](../../memory/MEMORY.md#bahmni-csv-upload-format-critical-for-form-metadata)

### "How do I set up the database for integration?"
→ Read [03-Technical-Documentation/02-configuration-reference.md](03-Technical-Documentation/02-configuration-reference.md) and [02-Status-Reports/02-integration-status-summary.md](02-Status-Reports/02-integration-status-summary.md)

---

## 📋 Document Versions

| Document | Last Updated | Purpose |
|----------|-------------|---------|
| 01-executive-summary.md | 2026-02-24 | Project status snapshot |
| 02-integration-status-summary.md | 2026-02-24 | Detailed progress report |
| 01-avni-bahmni-integration.md | 2026-02-23 | Technical architecture |
| 03-implementation-summary.md | 2026-02-20 | Implementation details |
| 02-configuration-reference.md | 2026-02-20 | Database setup |
| 01-blocking-issues.md | 2026-02-22 | Known issues |

---

## 🚀 Next Steps

1. **Find the doc for your role** - Use the "Quick Start by Role" section above
2. **Read the relevant folder** - Browse the appropriate section for your task
3. **Check memory/MEMORY.md** - If you hit issues or need to implement CSV uploads
4. **Ask questions** - Refer specific documents in conversations

---

**For questions, feedback, or to suggest documentation improvements, reach out to the development team.**

Last Updated: 2026-02-24
