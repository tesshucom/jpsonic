# Security Policy

## Reporting a Vulnerability
Please report security issues via GitHub Security Advisories.
Do not open public issues for security-related reports.

## Supported Versions
Only the latest released version is supported.
Older versions are not maintained.

## Security Response
Reported issues will be evaluated based on exploitability and impact
within the actual execution paths of the application.
Not all dependency-reported CVEs require fixes;
each case is assessed individually.

## Vulnerability Evaluation Criteria

### Jpsonic Core

Jpsonic uses the latest available LTS version of the JVM.
Dependencies are kept up to date, and CVE suppressions or library-level
patch management are performed on a regular basis.

The project aims to keep builds free of known security warnings
through continuous dependency review.

### CVE Handling for Docker Images

Jpsonic provides Docker images based on multiple distributions
for development, research, and risk diversification purposes.

Each base image has different CVE reporting and evaluation characteristics:

- Alpine: Minimal package set with a reduced attack surface
- UBI9 (RHEL-based): CVEs are evaluated by Red Hat with exploitability context
- Ubuntu: CVEs are reported broadly, including theoretical or non-reachable cases

Due to these differences, CVE handling policies may vary by base image.
Alpine and UBI9 images aim to minimize reported warnings where feasible,
while Ubuntu-based images may retain unresolved CVEs depending on relevance.
