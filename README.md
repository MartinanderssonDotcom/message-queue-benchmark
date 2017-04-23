## Introduction

Will provide an undefined number of different implementations of an in-memory
queue service. Purpose is to benchmark different ways to achieve thread-safety
in Java, given a very real application-context.

TODO: Write more stuff.



## Setup

```sh
git clone https://github.com/martinanderssondotcom/queue-service-benchmark
```

Project is built using [Gradle], but, because of a crazy-awesome thing called
[Gradle Wrapper] you don't have to have Gradle installed. In fact, it is
recommended that you use `gradlew` instead of `gradle` when running Gradle tasks
(version dependency).



## Run unit tests

Three guys are provided:

```sh
gradlew test --tests *FirstTest
```

```sh
gradlew test --tests *SecondTest
```

```sh
gradlew test --tests *ThirdTest
```

Or run all:

```sh
gradlew test
```

Running one or all tests yield an HTML report you can find here:

> build/reports/tests/test/index.html



## Run benchmarks

TODO



[Gradle]: <https://gradle.org>
[Gradle Wrapper]: https://docs.gradle.org/current/userguide/gradle_wrapper.html