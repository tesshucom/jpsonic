# Contributing to Jpsonic

Jpsonic is released under the GPLv3.
All contributions must be licensed as [GNU GPLv3](https://github.com/airsonic/airsonic/blob/develop/LICENSE.txt) to be accepted. Use [`git commit --signoff`](https://jk.gs/git-commit.html) to acknowledge this.
If you would like to contribute something, or want to hack on the code this document should help you get started.

## Code of Conduct

This project adheres to the [Citizen Code of Conduct](https://github.com/tesshucom/jpsonic/blob/fix-contributing.md/CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Using GitHub Issues

We use GitHub issues to track bugs and probrems. 

### Bug report

Be careful when reporting bugs.
 - Double bug report posts will be discarded. Topics that are common to sibling servers other than Jpsonic, not just Jpsonic, are considered double posts.
 - Please help to speed up problem diagnosis by providing as much information as possible. Follow the template.

### Questions

If you have questions about traditional and basic usage please ask on Airsonic community.
Questions about Jpsonic's original features are accepted, but it's probably faster to ask on the [author's site](https://tesshu.com/jpsonic/faq).

### Feature request

Currently we are not accepting requests.
I don't know the future policy yet.

## Working with the Code

Jpsonic is using [Maven](https://maven.apache.org/) to manage its build process.

### Building from Source

If you want to run the testsuite and get a `.war` is everything went fine,
you this command:

```
$ mvn clean package 
```

If you don't care about the result of the testsuite, but only
want a `.war` as quick as possible, you can use this instead:

```
$ mvn clean package -Dmaven.test.skip=true -Dpmd.skip=true
```

If you want to use Jetty instead of Tomcat:

```
$ mvn clean package -Dmaven.test.skip=true -Dpmd.skip=true -Pjetty-embed
```

When using the Java 15 compiler and JVM:

```
$ mvn clean package -Dmaven.test.skip=true -Dpmd.skip=true -Pjetty-embed -Prelease15
```

See [the documentation](https://tesshu.com/update/how-to-compile-jpsonic) for more information.

