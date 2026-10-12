# Greater Art Desktop version policy

Current desktop: **0.1.0 (code 1)**
Release state: **DEVELOPMENT**
Android reference: 884a2d5 / 1.21.38. Desktop and Android versions are independent.

Each committed desktop executable/build/workflow change increments desktop PATCH
and code together. Initial code is 0.1.0/code1. Documentation and verified-artifact
finalization do not consume another version. Never overwrite a versioned artifact.
Full-parity 1.0.0 is reserved until every parity acceptance gate passes.

0.1.0 is an incomplete development build, not full Android parity or production.
Native backend, packaging, clean-machine and license acceptance remain explicit
gates. No installer is published automatically.

- 0.1.0 (code 1) — Independent Compose/JVM project, coordinated session,
  three-pane workspace, scoped recursive library, favorites, real libVLC binding,
  basic concurrent voices, portable Android graph algorithms and initial tests.
