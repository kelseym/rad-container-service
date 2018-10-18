# Contributing to the XNAT Container Service

👍🎉 First off, thanks for taking the time to contribute! 🎉👍

We welcome any [bug reports](#report-an-issue), feature requests, or [questions](#ask-a-question). And we super-duper welcome any [pull requests](#make-a-pull-request).

This document is a little sparse now, but will hopefully evolve in the future.

## Report an issue

The easiest way for you is probably to [report an issue on GitHub](https://github.com/NrgXnat/container-service/issues). Alternately, if you have (or want to open) an account on the [XNAT JIRA](https://issues.xnat.org), you could make an issue there; just add it to the [Container Service project](https://issues.xnat.org/projects/CS).

## Ask a question

First, check the [XNAT Discussion Board](https://groups.google.com/forum/#!forum/xnat_discussion). It is possible that someone has already asked your question and gotten an answer, so it could save you a lot of time to search around first. And if no one has asked your question, the discussion board is a great first place to ask.

If you don't want to use the discussion board, you can also ask a question on the same channels as [reporting an issue](#report-an-issue). A GitHub issue will most likely be easier for you than opening an account on our JIRA.

## Run the Tests
The various unit tests can be run with:
```
[container-service]$ ./gradlew unitTest
```

If you have a docker server that you can use for testing, there are some additional tests that can run. Make sure your docker environment is all set up (you can test this by making sure `$ docker version` works). Then run all the tests with
```
[container-service]$ ./gradlew test
```
In order to synchronize your Docker VM clock, you may need to initially run the command as:
```
[container-service]$ docker run --rm --privileged alpine:latest hwclock -s && ./gradlew  clean test
```
We do not have any tests that can integrate with a running XNAT. All of the tests in this library use bespoke databases and mocked interfaces any time the code intends to communicate with XNAT. We welcome your contributions!

## Make a Pull Request
If you want to contribute code, we will be very happy to have it.

The first thing you should know is that we do almost all of our work on the `dev` branch, or on smaller "feature" branches that come from and merge into `dev`. So if you want to do something with the code, you, too, should start a new branch from `dev`.

1. Fork the repo.
1. Clone your fork to your local machine, using HTTP (`git clone https://github.com/<you>/container-service.git`) or SSH (`git clone git@github.com:<you>/container-service.git`).
1. Check out a new branch for your feature, starting from the `dev` branch.
    git checkout -b my-cool-new-feature origin/dev
1. Commit your work there.
1. When you are done (or anytime, really), push your changes up to a branch on your repo.
    git push --set-upstream origin my-cool-new-feature
1. [Open a Pull Request](https://github.com/NrgXnat/container-service/compare). It should go from your feature branch on your fork back to the `dev` branch on `nrgxnat/container-service`.
1. We can review it and go from there.

And thanks again!
