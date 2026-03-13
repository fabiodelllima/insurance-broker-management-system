# ADR-002: Static Analysis and Code Formatting Toolchain

| Field       | Value                          |
|-------------|--------------------------------|
| **Status**  | Accepted                       |
| **Date**    | 2026-03-12                     |
| **Deciders**| F. (Lead Developer)            |

## Context

The IBMS project follows enterprise development practices as a solo-developer project
targeting the Brazilian insurance brokerage market. Maintaining consistent code style and
catching bugs early is critical when there is no second pair of eyes in code review. The
codebase uses Spring Boot 3.5 with Java 17, Maven as the build tool, and Lombok for
boilerplate reduction.

Without automated enforcement, code style drifts over time and subtle bugs (null
dereferences, resource leaks, race conditions) accumulate as technical debt. In the Python
ecosystem, tools like Ruff, Mypy, and Pyright fill these roles; Java requires a different
but analogous set of tools.

## Decision

We adopt a three-layer static analysis toolchain integrated into the Maven build lifecycle
and enforced via Git hooks.

### Layer 1 — Formatting: google-java-format (via fmt-maven-plugin)

Applies the AOSP (Android Open Source Project) style variant of google-java-format: 4-space
indentation and 100-column line width. This aligns with the conventions dominant in the
Brazilian enterprise Java market (Spring Framework, JDK source, Apache Foundation, major
banks and fintechs), reducing friction for developers joining the project. The formatter is
authoritative — Checkstyle formatting rules that overlap with it are suppressed to avoid
conflicts.

- Plugin: `com.spotify.fmt:fmt-maven-plugin:2.29`
- Style: `aosp` (4-space indent, 100-col lines)
- Maven phase: `validate` (check), manual invocation (format)

### Layer 2 — Linting: Checkstyle (Google Checks)

Enforces structural and naming conventions that go beyond formatting: Javadoc requirements,
naming patterns, import ordering semantics, modifier order, and design rules. Uses the
built-in `google_checks.xml` ruleset with a suppressions file to avoid conflicts with
Layer 1.

- Plugin: `maven-checkstyle-plugin:3.6.0`
- Checkstyle version: `12.1.0` (last version supporting JDK 17; 13.x requires JDK 21)
- Maven phase: `validate`
- Suppressions: `Indentation`, `LineLength`, `CustomImportOrder` (handled by formatter)

### Layer 3 — Bug Detection: SpotBugs (bytecode analysis)

Analyzes compiled bytecode to detect null dereferences, resource leaks, concurrency issues,
and other bug patterns that neither the compiler nor the linter can catch. Operates on a
different abstraction level — analogous to what Pyright provides over Mypy in the Python
ecosystem.

- Plugin: `spotbugs-maven-plugin:4.9.8.2`
- Maven phase: `verify`
- Effort: `Max`
- Threshold: `Medium`
- Exclusions: Lombok-generated `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` patterns

### Deferred: Error Prone

Error Prone (Google) is a compile-time static analyzer that catches bugs the Java compiler
misses. It currently requires JDK 21+ to run. Since the project builds on JDK 17, Error
Prone will be added when the build JDK is upgraded. This does not affect the target bytecode
version — Error Prone supports `--release 17` cross-compilation.

### Git Hooks

Enforcement is split across two hooks to balance fast feedback with thorough validation:

- **pre-commit**: auto-format via `fmt:format` (re-stages changed files) + `checkstyle:check`.
  Fast, catches style issues before they enter history.
- **pre-push**: `./mvnw verify` — full pipeline including compilation, tests, and SpotBugs.
  Prevents broken code from reaching the remote.

Hooks live in `.githooks/` and are activated via `git config core.hooksPath .githooks`.
A `format.sh` script at the repository root provides a shortcut for manual formatting.

### EditorConfig

An `.editorconfig` file at the repository root ensures consistent whitespace and encoding
across editors and IDEs. Java files use 4-space indentation to match the AOSP formatter
style. XML files use 4 spaces (Maven convention). YAML files use 2 spaces (YAML standard).

## Consequences

### Positive

- Every commit is guaranteed to be consistently formatted — no style drift, no review
  friction on formatting.
- 4-space indentation matches the prevailing convention in the target market, reducing
  onboarding friction.
- Structural code quality issues are caught at `validate` phase, before compilation.
- Bytecode-level bugs are caught at `verify` phase, before code reaches the remote.
- The three tools are complementary with no overlap: formatter handles layout, Checkstyle
  handles conventions, SpotBugs handles bugs.
- All tools run via Maven plugins with no external dependencies beyond the JDK.
- Pre-commit auto-formats instead of just failing, reducing developer friction.
- Git hooks enforce locally; CI enforces on the server — defense in depth.

### Negative

- Pre-commit adds ~5-10 seconds per commit (auto-format + Checkstyle).
- Pre-push adds the full build time (~30-60 seconds including tests and SpotBugs).
- Developers can bypass hooks with `--no-verify` (mitigated by CI enforcement).
- Checkstyle version is pinned to 12.1.0 until JDK 21 migration.
- SpotBugs EI_EXPOSE_REP exclusion is broad; should be narrowed when Lombok usage
  patterns stabilize.

### Neutral

- Error Prone adoption is deferred, not rejected. The decision will be revisited when
  the build migrates to JDK 21.
- AOSP style differs from Google style only in indentation (4 vs 2 spaces) and line
  length (100 vs 100 — same). All other formatting rules are identical.

## Alternatives Considered

| Alternative                    | Reason for Rejection                                          |
|--------------------------------|---------------------------------------------------------------|
| Google style (2-space indent)  | Uncommon in BR enterprise market; would create friction for new developers |
| Palantir Java Format           | Less adoption than Google; 120-col lines diverge from Checkstyle defaults |
| PMD                            | Overlaps significantly with Checkstyle; adds noise without proportional value as a fourth tool |
| SonarQube / SonarCloud         | Overkill for solo developer at POC/MVP stage; revisit when team grows |
| No hooks (CI-only enforcement) | Allows broken commits into history; slower feedback loop      |
| All checks in pre-commit       | Too slow (~60s) for comfortable commit workflow               |
| Check-only pre-commit          | Requires manual re-format + re-stage; auto-format is less friction |

## References

- Google Java Style Guide: https://google.github.io/styleguide/javaguide.html
- AOSP Java Code Style: https://source.android.com/docs/setup/contribute/code-style
- fmt-maven-plugin: https://github.com/spotify/fmt-maven-plugin
- Checkstyle: https://checkstyle.org/
- SpotBugs: https://spotbugs.github.io/
- Error Prone: https://errorprone.info/
- Michael Nygard, "Documenting Architecture Decisions" (2011)
