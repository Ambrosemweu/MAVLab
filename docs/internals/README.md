# MAVLab Internals

These documents explain current architecture and behavioral contracts for maintainers and contributors.

- [Architecture overview](overview.md)
- [Runtime lifecycle](runtime-lifecycle.md)
- [Simulation model](simulation-model.md)
- [Control authority](control-authority.md)
- [MAVLink contract](mavlink-contract.md)
- [Glossary](glossary.md)
- [Architecture decisions](decisions/README.md)
- [Documentation governance](documentation-governance.md)

Each durable internals document should answer:

1. What responsibility does the subsystem own?
2. What must remain true?
3. What enters and leaves the boundary?
4. What fails, degrades, or stops?
5. Which source files and tests prove the behavior?
6. Does the statement describe a release, committed `main`, or a draft working tree?