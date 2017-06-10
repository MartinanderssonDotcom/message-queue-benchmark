## Introduction

Will provide an undefined number of different implementations of an in-memory
queue service. Purpose is to benchmark different ways to achieve thread-safety
in Java, given a very real application-context.

Read more in the [research paper].



## Setup

```sh
git clone https://github.com/martinanderssondotcom/queue-service-benchmark
```

Project is built using [Gradle], but, because of a crazy-awesome thing called
[Gradle Wrapper] you don't have to have Gradle installed. In fact, it is
recommended that you use `gradlew` instead of `gradle` when running Gradle tasks
(version dependency).



## Run unit tests

Four queue service implementations are provided and these can be tested
separately:

```sh
gradlew test --tests *ConcurrentQSWithPojoMessageTest
```

```sh
gradlew test --tests *ConcurrentQSWithAtomicMessageTest
```

```sh
gradlew test --tests *ReadWriteLockedQSTest
```

```sh
gradlew test --tests *SynchronizedQSTest
```

Or run all:

```sh
gradlew test
```

Running one or all tests yield an HTML report you can find here:

> build/reports/tests/test/index.html



## Run benchmarks

The Gradle project provides a task named `bench` that launch benchmarks. This
task move a set of supported properties into the Java runtime as system
properties and then run the main method in [`StartJmh`].

The main benchmark harness in focus for this project is
[`QueueServiceBenchmark`]. This class has a number of nested classes that each
declare benchmarks with the only difference being measurement modes.

[`QueueServiceBenchmark`] make use of- and support all [`SystemProperties`]. The
most important one being a regex that dictate which benchmarks to run. Please
see the JavaDoc of each literal found in [`SystemProperties`] to learn how they
are used.




[research paper]: <https://github.com/martinanderssondotcom/queue-service-benchmark/blob/master/Java%20concurrency%20benchmark.pdf>
[Gradle]: <https://gradle.org>
[Gradle Wrapper]: https://docs.gradle.org/current/userguide/gradle_wrapper.html
[`StartJmh`]: https://github.com/Martinanderssondotcom/queue-service-benchmark/blob/master/src/test/java/com/martinandersson/qsb/benchmark/StartJmh.java
[`QueueServiceBenchmark`]: https://github.com/Martinanderssondotcom/queue-service-benchmark/blob/master/src/test/java/com/martinandersson/qsb/benchmark/QueueServiceBenchmark.java
[`SystemProperties`]: https://github.com/Martinanderssondotcom/queue-service-benchmark/blob/master/src/test/java/com/martinandersson/qsb/benchmark/SystemProperties.java