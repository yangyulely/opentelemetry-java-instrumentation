# Repository settings

This document describes any changes that have been made to the
settings for this repository beyond the [OpenTelemetry default repository
settings](https://github.com/open-telemetry/community/blob/main/docs/how-to-configure-new-repository.md#repository-settings).

## General > Pull Requests

- Allow squash merging > Default to pull request title

- Allow auto-merge

## Actions > General

- Fork pull request workflows from outside collaborators:
  "Require approval for first-time contributors who are new to GitHub"

  (To reduce friction for new contributors,
  as the default is "Require approval for first-time contributors")

## Branch protections

### `main`

- Require branches to be up to date before merging: UNCHECKED

  (PR jobs take too long, and leaving this unchecked has not been a significant problem)

- Status checks that are required:

  - EasyCLA
  - required-status-check

### `release/*`

Same settings as above for [`main`](#main).

### `renovate/**/**`, `opentelemetrybot/**/**`, and `gradlew-update-*`

Same settings as
for [`dependabot/**/**`](https://github.com/open-telemetry/community/blob/main/docs/how-to-configure-new-repository.md#branch-protection-rule-dependabot)

### `gh-pages`

- Everything UNCHECKED

  (This branch is currently only used for directly pushing benchmarking results from the
  [Nightly overhead benchmark](https://github.com/open-telemetry/opentelemetry-java-instrumentation/actions/workflows/nightly-benchmark-overhead.yml)
  job)

## Code security and analysis

- Secret scanning: Enabled

## Secrets and variables > Actions

- `GE_CACHE_PASSWORD`
- `GE_CACHE_USERNAME`
- `GPG_PASSWORD` - stored in OpenTelemetry-Java 1Password
- `GPG_PRIVATE_KEY` - stored in OpenTelemetry-Java 1Password
- `GRADLE_ENTERPRISE_ACCESS_KEY` - owned by [@trask](https://github.com/trask)
  - Generated at https://ge.opentelemetry.io > My settings > Access keys
  - format of env var is `ge.opentelemetry.io=<access key>`,
    see [docs](https://docs.gradle.com/enterprise/gradle-plugin/#via_environment_variable)
- `GRADLE_PUBLISH_KEY`
- `GRADLE_PUBLISH_SECRET`
- `OPENTELEMETRYBOT_GITHUB_TOKEN` - owned by [@trask](https://github.com/trask)
- `SONATYPE_KEY` - owned by [@trask](https://github.com/trask)
- `SONATYPE_USER` - owned by [@trask](https://github.com/trask)
