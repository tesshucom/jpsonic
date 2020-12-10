# Getting a .war

Jpsonic is using [Maven](https://maven.apache.org/) to manage its build
process.


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

# Suggesting modifications

**Airsonic**'s source code is hosted on [github](https://github.com/airsonic/airsonic/),
who provides a [lot of documentation](https://help.github.com/en) on how
to contribute to projects hosted there.
Keep in mind that this is a non-funded community-driven project maintained by
a relatively small group of contributors who have many other responsibilities
and demands on their time. Development, maintenance, and administration of the
project is done on a best-effort basis, as time and other constraints permit.

**Jpsonic** is currently not actively accepting change proposals.
There are already many planned features that Jpsonic is trying to achieve, and there is little need to discuss it with you.
If you want to add functionality, please publish the fork.
If you want to redistribute the modified program, please make an appropriate brand change.

# Getting help

The documentation is hosted [here](https://airsonic.github.io/) (you can
contribute to it [here](https://github.com/airsonic/documentation)), and aims
at being comprehensive. You can also use [irc](irc://irc.freenode.net/airsonic)
and [reddit](https://www.reddit.com/r/airsonic/) if you want to discuss or ask
questions.
