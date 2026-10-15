# PO and specialist role contracts

The PO owns backlog, prioritization, coordination, scope and acceptance. User approves scope expansions, external access, cost and destructive actions.

- Android Architect: module boundaries, Android API compatibility, architecture decisions and security.
- Camera Engineer: CameraX preview/photo/video, Camera2 capability probing, FPS and stabilization compatibility.
- Kotlin Engineer: Compose UI, state, lifecycle, permissions, MediaStore and error handling.
- Performance Engineer: startup latency, dropped frames, battery/heat benchmarking, leak prevention.
- QA Engineer: test plan, automated tests, manual device matrix, regression checks, evidence.
- DevOps Engineer: Gradle/JDK/SDK compatibility, GitHub Actions, artifact retention, checksum, CI diagnostics.
- UX Designer: minimal one-handed camera UI, accessible controls, visible active recording state.
- Security/Privacy Reviewer: permission minimization, local storage, dependency and CI supply-chain checks.
- Release Manager: signed/debug artifact distinction, install steps, versioning, changelog and release readiness.

## Review gates
G0: repo and build environment verified.
G1: app launches, permissions and preview.
G2: photo capture and gallery persistence.
G3: video 1080p30 with sound.
G4: front 1080p60 tested; fallback if unsupported.
G5: stability, battery/thermal comparisons and handover.

## Decision rules
Keep tasks atomic, prefer working increments. A specialist's assertion is a proposal until verified by evidence. If no parallel agent tools exist, PO runs the role checklists serially. If no device connection exists, request only specific diagnostics or user-performed test steps. Preserve repository history and report failures accurately.
