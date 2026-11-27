# Carbonio Workstream Collaboration CE

[![Contributors][contributors-badge]][contributors]
[![Activity][activity-badge]][activity]
[![License][license-badge]](COPYING)
[![Project][project-badge]][project]

Backend of Carbonio Workstream Collaboration: chats and meetings. Community edition.

- Installation: [Carbonio CE guide](https://docs.zextras.com/carbonio-ce/html/install/toc.html)
- Messages and events reference: [`docs/`](docs/)

### Stale meeting participant cleanup

`carbonio-ws-collaboration-configs` seeds these Consul keys under `carbonio-ws-collaboration/meeting/`:

| Consul key suffix | Docker environment variable | Default | Meaning |
| --- | --- | --- | --- |
| `stale-participant-threshold-in-hours` | `STALE_PARTICIPANT_THRESHOLD_IN_HOURS` | `12` | Minimum age of the last participant row update, not time since disconnect |
| `stale-participant-cleanup-interval-in-hours` | `STALE_PARTICIPANT_CLEANUP_INTERVAL_IN_HOURS` | `12` | Delay between the end of one sweep and the start of the next |

Use positive hours, including decimals that convert to whole seconds. For a test deployment, a threshold of `0.02` (72 seconds) and an interval of `0.005` (18 seconds) let you exercise both sweeps quickly. Restart WSC to apply changes; the first sweep runs one minute after startup.

[contributors-badge]: https://img.shields.io/github/contributors/zextras/carbonio-ws-collaboration-ce "Contributors"
[contributors]: https://github.com/zextras/carbonio-ws-collaboration-ce/graphs/contributors "Contributors"
[activity-badge]: https://img.shields.io/github/commit-activity/m/zextras/carbonio-ws-collaboration-ce "Activity"
[activity]: https://github.com/zextras/carbonio-ws-collaboration-ce/pulse "Activity"
[license-badge]: https://img.shields.io/badge/license-AGPL--3.0--only-blue "License"
[project-badge]: https://img.shields.io/badge/project-carbonio-informational "Project Carbonio"
[project]: https://www.zextras.com/carbonio/ "Project Carbonio"
