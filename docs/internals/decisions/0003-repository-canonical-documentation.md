# ADR-0003: Repository-canonical technical documentation

Status: Accepted
Date recorded: 2026-08-31

## Context

MAVLab facts existed in the repository, Obsidian, historical plans, duplicate Android docs, release notes, and a paper. Contributors could not access private paths, and copies drifted.

## Decision

The public repository is canonical for current product behavior, architecture, protocol, build, validation, and release runbooks. Ascend’s Obsidian vault remains canonical for private operating context, ownership, strategy, and decision tracking, and links back to repository docs.

## Consequences

- Contributors can access the contracts they must preserve.
- Obsidian links rather than redefining mechanical facts.
- Historical plans are archived and labeled.
- Paper claims remain tied to evaluated releases.
- Contract changes include documentation changes.

## Replace when

Only reconsider if a public documentation platform supersedes the repository while preserving versioned, reviewable, contributor-accessible source.