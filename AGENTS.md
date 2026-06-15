# AGENTS.md

Guidance for AI coding agents (Claude Code, Google Jules, and any future tool)
working on this repository. `CLAUDE.md` is a symlink to this file — both names
point at the same content so each agent finds what it expects to read.

## Project overview

Meridian is a Minecraft 1.21.1 Fabric mod — a complete enchanting overhaul
with five enchanting stats (Eterna, Quanta, Arcana, Rectification, Clues),
25+ themed shelf blocks, enchantment libraries, salvage tomes, anvil
upgrades, and 75 original enchantments. Java 21, Fabric Loader 0.16.10,
Loom 1.9. The feature surface is documented in [`README.md`](README.md) and
[meridian.rfizzle.com](https://meridian.rfizzle.com). Work is tracked in
GitHub Issues — see the [Development lifecycle](#development-lifecycle)
section below.

## Suite standards (Concord)

This mod is a member of Concord, the Vanilla+ collection. Suite-wide standards live in
the [concord repo](https://github.com/rfizzle/concord) — checked out at `../concord/`
in the local workspace. Normative for this repo:

- [API-STANDARD.md](https://github.com/rfizzle/concord/blob/master/API-STANDARD.md) — the `api` package conventions (conforms to v1)
- [HUD-STANDARD.md](https://github.com/rfizzle/concord/blob/master/HUD-STANDARD.md) — HUD slot, stacking, accessors (no HUD slot, by design — see design/DESIGN.md)
- [DESIGN-SYSTEM.md](https://github.com/rfizzle/concord/blob/master/design/DESIGN-SYSTEM.md) — palette, typography, logo rules
- [REPO-LAYOUT.md](https://github.com/rfizzle/concord/blob/master/REPO-LAYOUT.md) — where non-code files live (migrated 2026-06-11)

## Build commands

```bash
./gradlew build                    # compile + test + jar
./gradlew test                     # JUnit tests only
./gradlew runGametest              # Fabric gametests (headless server)
./gradlew runClient                # launch dev client
./gradlew runServer                # launch dev server
./gradlew runDatagen               # regenerate data into src/main/generated
./gradlew verifyDatagenIdempotent  # check datagen output is committed & clean
./gradlew genSources               # decompile MC sources for IDE nav
```

Run a single JUnit test:
`./gradlew test --tests "com.rfizzle.meridian.SomeTest"`

Read [`.ai/skills/mc-gradle-builds/SKILL.md`](.ai/skills/mc-gradle-builds/SKILL.md)
**before running any Gradle command** — it covers how to avoid wasted reruns
from partial output capture.

## Source layout

Loom's `splitEnvironmentSourceSets()` is enabled — three source sets:

| Source set | Root | Purpose |
|---|---|---|
| `main` | `src/main/java` | Server + common logic. Entrypoint: `Meridian.java` |
| `client` | `src/client/java` | Client-only code. Entrypoint: `MeridianClient.java` |
| `gametest` | `src/gametest/java` | Fabric gametests (run with `runGametest`). Has `main` on its classpath but is NOT included in the jar. Also hosts the datagen entrypoint (`MeridianDataGenerator`, run with `runDatagen`). |

JUnit tests go in the standard `src/test/java` directory. The test classpath
includes `fabric-loader-junit` but excludes `fabric-api` — tests that need
Fabric APIs must use gametests instead.

Generated resources land in `src/main/generated` (wired into `main`'s
resources). They are committed; after touching any datagen provider, run
`runDatagen` and commit the diff (`verifyDatagenIdempotent` enforces this).

## Key conventions

- **Mod ID:** `meridian` — use `Meridian.id("path")` to create
  `ResourceLocation`s. Never construct `ResourceLocation` directly with the
  mod ID inlined.
- **Mappings:** Official Mojang mappings (not Yarn). Use Mojang class/method
  names everywhere (`CompoundTag`, not `NbtCompound`; `Level`, not `World`).
- **Assets:** Meridian has its own custom assets at `assets/meridian/`
  (textures, models, sounds).
- **Mixin config:** `meridian.mixins.json` in `src/main/resources`. Mixin
  package: `com.rfizzle.meridian.mixin`. Access widener:
  `meridian.accesswidener`.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/)
  with a topical scope naming the feature area: `feat(enchanting): …`,
  `fix(shelves): …`, `refactor(library): …`, `ci(review): …`,
  `build(test): …`, `chore(ai): …`, `docs(readme): …`. Allowed types:
  `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `build`, `ci`, `perf`,
  `style`. Subject line in imperative mood, no trailing period, ≤72 chars.
  Reference the issue in the body footer: `Closes #42` (or `Refs #42` for
  partial work).

## Compat integrations

The mod has optional integrations (all `modCompileOnly` — not bundled):

- **Mod Menu** — config screen entry via `ModMenuIntegration`
- **Cloth Config** — settings GUI builder
- **Jade / WTHIT** — tooltip overlays
- **EMI / REI / JEI** — recipe viewer support
- **Trinkets** — wearable item slots

Compat classes live under `com.rfizzle.meridian.compat.<modid>`.

## Where things live

| Path | Purpose |
|---|---|
| `README.md` | Project overview and feature summary. |
| `site/` | Structured website content (source for [meridian.rfizzle.com](https://meridian.rfizzle.com)), rendered by the shared Concord template. |
| `design/DESIGN.md` | Brand, palette, asset specs — the pre-implementation "why & what". |
| GitHub Issues | Active work — feature requests, bugs, in-flight specs. |
| `.ai/skills/` | Domain skills — read these before working in their subject area. |
| `.github/workflows/` | Thin trigger stubs — workflow logic, default CI prompts, and [review criteria](https://github.com/rfizzle/concord/blob/master/.ai/review-criteria.yml) live in [rfizzle/concord](https://github.com/rfizzle/concord). |

<!-- concord:skills:start -->
## Working with domain skills

The suite's `mc-*` domain skills live under `.ai/skills/`, vendored from concord
and refreshed with `make sync-skills`. The full list — each skill's one-line
summary and the situation that should make you pull it in — is the generated
catalog at [`.ai/skills/CATALOG.md`](.ai/skills/CATALOG.md). It is always in step
with the skills actually vendored here, so consult it rather than a hand-kept
table.

Claude Code auto-loads these via the `.claude/skills` symlink; Google Jules,
OpenCode, and any other agent should read the relevant `SKILL.md` directly
**before** working in its subject area.
<!-- concord:skills:end -->

<!-- concord:lifecycle:start -->
## Development lifecycle

1. **Issue opened** using the feature or bug template under `.github/ISSUE_TEMPLATE/`.
2. **Triage** — human discussion in the issue.
3. **`needs-spec` label** added → `.github/workflows/claude-spec.yml` fires,
   Claude posts a structured implementation spec as an issue comment
   (prompt: concord's default `spec-writer.md`, unless a repo-local
   `.ai/prompts/spec-writer.md` override exists).
4. **Human review** — spec edited or approved.
5. **`jules` label** added (remove `needs-spec`) → Jules picks up the issue
   and opens a draft PR.
6. **PR opened** → `claude-code-review.yml` posts a structured ✓/⚠/✗ review
   (categories from concord's default `review-criteria.yml`, unless a
   repo-local `.ai/review-criteria.yml` override exists). `ci.yml` runs the
   full build, unit tests + gametests, and uploads coverage + results to
   Codecov.
7. **Human review + merge.**

`@claude <message>` in any issue or PR comment also invokes Claude for ad-hoc
help via `.github/workflows/claude.yml`.
<!-- concord:lifecycle:end -->

<!-- concord:pr-conventions:start -->
## Pull requests & commits

When you open a pull request for an issue:

- **Title** — Conventional Commits with a topical scope, matching the issue's
  normalized title (e.g. `feat(render): add glyph atlas cache`). Imperative
  mood, lower-case, no trailing period.
- **Body** — open with a short plain-language summary of what changed and why,
  then link the source issue with `Closes #<n>` so it auto-closes on merge and
  the code review can pull the issue's spec for context. Use `Refs #<n>` only
  when the PR deliberately leaves part of the issue for later.
- **Commits** — Conventional Commits using the same scope vocabulary. Group the
  edits for one logical change together rather than scattering fixup commits.
- Run the project's build and tests before opening the PR, and open it only
  once the build is green.
<!-- concord:pr-conventions:end -->

<!-- concord:version-scheme:start -->
## Version scheme

Version is computed from git tags at build time (`build.gradle`,
`computeModVersion()`). Base version is in `gradle.properties` as
`mod_version`. Tagged commits produce clean versions; post-tag commits append
`+<commits>.g<sha>`.
<!-- concord:version-scheme:end -->
