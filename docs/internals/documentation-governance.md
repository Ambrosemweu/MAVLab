# Documentation Governance

Audience: Maintainers
Status: Active policy
Last verified: 2026-08-31

## Sources of truth

- Code, config, and tests: mechanical implementation facts.
- `docs/user/`: observable shipped behavior.
- `docs/internals/`: current architecture and technical contracts.
- `docs/operations/`: repeatable maintainer procedures.
- `docs/validation/results/`: immutable execution evidence.
- Git history, releases, and issues: implementation/planning history.
- Published paper and DOI: versioned research evidence.
- Ascend Obsidian vault: private operating context, ownership, strategy, and links to canonical public docs.

A private path cannot be the only authority for behavior public contributors must preserve.

## Status labels

Use **Current**, **Draft**, **Release-specific**, **Historical**, or **Generated**.

## Placement rule

- User notices it → `docs/user/`.
- Contributor preserves an invariant → `docs/internals/`.
- Maintainer repeats it → `docs/operations/`.
- It proves a build was tested → `docs/validation/results/`.
- It explains a consequential choice → `docs/internals/decisions/`.
- It is an old plan or superseded copy → `docs/archive/`.

## Drift rules

1. Do not duplicate current architecture under `mavlab-android/docs/`.
2. Prefer links over copied paragraphs.
3. Generate or source-link version, port, artifact name, and test facts where practical.
4. Contract changes update their documentation in the same change.
5. Published paper source remains tied to its evaluated release; corrections use a revision/addendum.
6. Plans do not become architecture merely because they are detailed.

## Migration note

Legacy root-level documents and divergent copies under `mavlab-android/docs/` are not being bulk-moved while substantial implementation work is uncommitted. Migrate them in bounded changes after current feature work is checkpointed.