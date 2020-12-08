# Getting a .war

Airsonic is using [Maven](https://maven.apache.org/) to manage its build
process. Any version above 3.3+ should do the job.

If you want to run the testsuite and get a `.war` is everything went fine,
you this command:

```
$ mvn clean package 
```

If you don't care about the result of the testsuite, but only
want a `.war` as quick as possible, you can use this instead:

```
$ mvn -Dmaven.test.skip=true clean package 
```


# Suggesting modifications

Airsonic's source code is hosted on [github](https://github.com/airsonic/airsonic/),
who provides a [lot of documentation](https://help.github.com/en) on how
to contribute to projects hosted there.

Keep in mind that this is a non-funded community-driven project maintained by
a relatively small group of contributors who have many other responsibilities
and demands on their time. Development, maintenance, and administration of the
project is done on a best-effort basis, as time and other constraints permit.

# Getting help

The documentation is hosted [here](https://airsonic.github.io/) (you can
contribute to it [here](https://github.com/airsonic/documentation)), and aims
at being comprehensive. You can also use [irc](irc://irc.freenode.net/airsonic)
and [reddit](https://www.reddit.com/r/airsonic/) if you want to discuss or ask
questions.
