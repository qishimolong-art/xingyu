# Repository Guidelines

## Project Structure & Module Organization
This is a pnpm + Turbo monorepo.

- `apps/` contains the runnable frontends: `web-antd`, `web-antdv-next`, `web-ele`, `web-naive`, and `web-tdesign`.
- `packages/` contains shared workspace packages such as `@core`, `constants`, `stores`, `utils`, `styles`, `locales`, and `types`.
- `internal/` holds repo tooling and shared build config packages like `vite-config`, `tsconfig`, `lint-configs`, and `tailwind-config`.
- `docs/` contains documentation assets; `.changeset/` stores release notes.
- `scripts/` contains maintenance and release helpers.

## Build, Test, and Development Commands
Use `pnpm` only (`package.json` enforces it).

- `pnpm install` or `pnpm bootstrap`: install dependencies.
- `pnpm dev` or `pnpm dev:antd`: start all apps or a specific app locally.
- `pnpm build`: build the workspace with Turbo.
- `pnpm check`: run dependency, circular, type, and spelling checks.
- `pnpm lint` / `pnpm format`: run repo linting and formatting.
- `pnpm test:unit`: run Vitest unit tests.
- `pnpm test:e2e`: run Playwright end-to-end tests.

## Coding Style & Naming Conventions
Formatting is standardized by `.editorconfig`: 2-space indentation, LF line endings, UTF-8, trailing whitespace trimmed, and single quotes preferred. Keep paths and package names aligned with existing workspace names, for example `apps/web-naive` and `packages/utils`. Use descriptive TypeScript/Vue file names and follow local component naming patterns. Run the repo format/lint commands before opening a PR.

## Testing Guidelines
Unit tests use Vitest (`vitest.config.ts`) and should live next to the code they cover or in a nearby `__tests__`-style location if the package already uses one. Name tests clearly, such as `button.spec.ts` or `use-auth.test.ts`. Prefer focused tests for shared packages and UI behavior. Run `pnpm test:unit` for unit coverage and `pnpm test:e2e` for flows that cross pages or apps.

## Commit & Pull Request Guidelines
Commit messages follow Conventional Commits via commitlint and `czg`, so use prefixes like `feat:`, `fix:`, `chore:`, or `docs:`. Keep commits small and scoped. PRs should include a short summary, linked issue or task reference when available, and screenshots or screen recordings for UI changes. Mention any affected app under `apps/` and any shared package under `packages/` or `internal/`.

## Agent Notes
When changing shared code, check downstream apps for impact. Avoid unrelated refactors, and prefer workspace scripts over ad hoc commands.
