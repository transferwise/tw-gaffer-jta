# Gaffer 1 phase commit JTA.

![Apache 2](https://img.shields.io/hexpm/l/plug.svg)
![Java 11](https://img.shields.io/badge/Java-11-blue.svg)
![Maven Central](https://badgen.net/maven/v/maven-central/com.transferwise.common/tw-gaffer-jta)
[![Owners](https://img.shields.io/badge/team-AppEng-blueviolet.svg?logo=wise)](https://transferwise.atlassian.net/wiki/spaces/EKB/pages/2520812116/Application+Engineering+Team) [![Slack](https://img.shields.io/badge/slack-sre--guild-blue.svg?logo=slack)](https://app.slack.com/client/T026FB76G/CLR1U8SNS)
> Use the `@application-engineering-on-call` handle on Slack for help.
---

Meant to replace default Spring Transaction Manager mainly for performance reasons. But also allows to write pool-of-1-compatible
code and reduces the probability of half done 1PC scenarios.

## Configuration in Spring Boot Service

Add `com.transferwise.common:tw-gaffer-jta` dependency.

It will create the necessary transaction manager objects and wrap all data sources exposed as beans with Gaffer's `DataSourceImpl`.

You can configure each datasource via `GafferJtaProperties` class.

## License
Copyright 2021 TransferWise Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
