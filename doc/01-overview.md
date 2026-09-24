# 01 — Overview

What LmiHL7 is, who uses it, and where it sits in the hospital's plumbing.

- [The product](#the-product)
- [Who uses it](#who-uses-it)
- [The clinical workflow the app models](#the-clinical-workflow-the-app-models)
- [Where the data comes from](#where-the-data-comes-from-eventually)
- [What HL7 actually means here](#what-hl7-actually-means-here)
- [Phase 1 scope](#phase-1-scope)
- [Vocabulary](#vocabulary)

---

## The product

LmiHL7 is a **radiology workstation, shrunk to a phone**.

A radiologist's day is a queue: studies get ordered, patients get scanned, images arrive, someone
reads them and writes a report, and the report has to be signed before it counts. The desktop PACS
workstation handles the reading. This app handles everything around it — knowing what's waiting,
what's urgent, what's overdue, and signing off from wherever you are.

The design brief for it was: *"next-generation radiology doctor workstation, redesigned for mobile"*
— not *"generic hospital app"*. That shows up in the UI as density (a lot of clinical facts per
screen), restraint (colour carries meaning, not decoration), and speed.

---

## Who uses it

**Dr. Sarah Chen**, the seeded demo user, is the persona: a diagnostic radiologist, on call, working
across three facilities. She cares about, in order:

1. Is there anything critical I haven't seen?
2. How much is left today?
3. What's next?
4. What needs my signature?

That ordering is literally the section order of the Today screen. It isn't a layout preference — it's
the clinical triage order, and the UI is not allowed to argue with it.

---

## The clinical workflow the app models

```
Order placed          a clinician requests an exam        → ORDER      (HL7 ORM)
Patient scheduled     an appointment is booked            → APPOINTMENT (HL7 SIU)
Patient arrives       checked in, scanned                 → STUDY
Images acquired       study lands in PACS
Radiologist reads     writes findings + impression        → REPORT
Report signed         becomes the legal record            → RESULT     (HL7 ORU)
```

Every domain model in the app maps onto one of those nouns. If you're ever unsure what a model is
for, this is the chain to check it against.

Two things carry urgency through the whole chain:

- **Priority** — `STAT` (now), `URGENT`, `ROUTINE`. A STAT order should be visible from across a room.
- **Critical result** — a finding that can't wait for normal turnaround (an acute stroke, a
  haemorrhage). The ordering clinician has to be told directly, and that communication is recorded.
  The Today screen gives critical results their own card above everything else.

---

## Where the data comes from (eventually)

```
Hospital HIS / RIS
      ↓   HL7 v2 messages (ADT, SIU, ORM, ORU)
Mirth Connect                 interface engine — parses, routes, acknowledges
      ↓
PostgreSQL                    the normalised clinical store
      ↓
AWS REST API                  the only thing the phone talks to
      ↓   HTTPS + JWT
LmiHL7 (this app)
```

**The app never touches PostgreSQL or Mirth.** Not now, not later. A database credential shipped
inside an APK is a leaked credential, and PHI behind a JDBC string reachable from a phone is not a
defensible architecture. Everything goes through the API. See
[09-backend-integration.md](09-backend-integration.md).

---

## What HL7 actually means here

HL7 v2 is the messaging standard hospitals use to move clinical events between systems. The four
message types this app cares about:

| Type | Means | Shows up in the app as |
|---|---|---|
| **ADT** | Admit / Discharge / Transfer — patient demographics and movement | Patient records |
| **SIU** | Scheduling — appointment booked, moved, cancelled | Schedule screen |
| **ORM** | Order — an exam has been requested | Worklist → Orders |
| **ORU** | Observation Result — a report is available | Worklist → Results, Results screen |

Mirth Connect receives these, acknowledges them (`ACK`) or rejects them (`NAK`), and writes the
result into PostgreSQL.

**This matters to the app in exactly one place**: the *More → HL7 Integration* screen, which is an
operational readout — message volume, ACK rate, per-channel health, recent messages. It's for
whoever is watching the interfaces, not for the radiologist. That's why it's tucked under More and
not on the dashboard. A doctor does not need to know the SIU channel is degraded; an integration
engineer does.

---

## Phase 1 scope

**In:** every screen, fully interactive, backed by a realistic demo dataset. Dark and light themes.
Adaptive layout. Full state handling (loading / empty / error / offline).

**Out, deliberately:**

- No real network calls. Retrofit and the full endpoint contract are written and compiled against,
  but nothing is wired.
- No Room database. Models are designed so it drops in later.
- No real authentication. The JWT plumbing has its shape; there is no token.
- No real biometric lock. The preference is stored; nothing is gated by it.
- No PACS image viewing. There's a documented hand-off seam, no invented URL.
- No FCM / WebSocket. Event types are defined as a sealed interface.

The demo data is **not** a placeholder to be embarrassed about — it's the point of this phase. It
lets people use the app and react to the workflow before a line of backend exists. See
[06-data-layer.md](06-data-layer.md) for how it's structured and how it gets removed.

---

## Vocabulary

Worth knowing before you read the code.

| Term | Meaning |
|---|---|
| **MRN** | Medical Record Number — the hospital's patient identifier |
| **Accession number** | Unique id for one imaging order/study, e.g. `ACC-2026-19014` |
| **CPT code** | Billing/procedure code, e.g. `70553` = MRI brain with and without contrast |
| **Modality** | The imaging machine type — CT, MR, XR, US, MG, NM, PT |
| **Indication** | Why the exam was ordered, in the referrer's words |
| **Findings** | What the radiologist observed |
| **Impression** | What it means — the conclusion. The part clinicians actually read. |
| **Preliminary** | A read exists but isn't final |
| **Sign-off** | The attending's signature that makes a report the legal record |
| **STAT** | Immediately |
| **MWL** | Modality Worklist — the feed that tells a scanner who's coming |
| **PACS** | Picture Archiving and Communication System — where the images live |

---

Next: [02-architecture.md](02-architecture.md)
