# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.1] - 2020-04-29
- Release to Maven Central under com.bisnode.kafka.authorization group. No code changes.

## [0.4.0] - 2020-04-23
- Allow `super.users` to bypass OPA authorizer checks - [@scholzj](https://github.com/scholzj)
- Fix wrong unit provided in docs on cache expiry - [@kelvk](https://github.com/kelvk)

## [0.3.0] - 2019-11-28
- Default cache size increase from 500 to 50000 based on real world usage metrics.
- Don't cache decision on errors as to avoid locking a client out if actually authorized.

## [0.2.0] - 2019-11-21
- Fix connection leak in authorization call.

## [0.1.0] - 2019-11-14
### Added
- First release!
