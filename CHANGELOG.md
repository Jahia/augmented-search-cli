# Changelog

All notable changes to the Augmented Search CLI module are documented in this file.

## [Unreleased]

### Fixed
- Async event sending (`--async`) now logs completion and surfaces failures instead of silently discarding the `CompletableFuture` (errors and completion were previously unobservable).
- Test mode (`--test`) no longer aliases and mutates the caller's event list when generating the bulk batch.
- `as:remove` now logs a warning when a synthetic removal event is sent for a path that no longer resolves to a node, so operators understand why removals fire against non-existent paths.
- External-provider path resolution is now prefix-safe (was matching the mount point anywhere in the path).
- `execute()` now logs a warning when a path resolves to neither a Jackrabbit nor an external content provider, instead of returning silently.

### Added
- First unit-test suite (JUnit 4 + Mockito) covering event building, synchronous/test-mode sending, and the `as:remove` synthetic-event fallback.
- README now documents the previously-undocumented `as:add` command and the required `path` argument for each command.
