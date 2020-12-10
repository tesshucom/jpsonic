# Guidelines for Contributing

Jpsonic development is a community project, and contributions are welcomed. Here are a few guidelines you should follow before submitting:

  1.  **License Acceptance** All contributions must be licensed as [GNU GPLv3](https://github.com/airsonic/airsonic/blob/develop/LICENSE.txt) to be accepted. Use [`git commit --signoff`](https://jk.gs/git-commit.html) to acknowledge this.
  2.  **No Breakage** New features or changes to existing ones must not degrade the user experience. This means do not introduce bugs, remove functionality, or make large changes to existing themes/UI without prior discussion in an Issue.
      It is because it hinders Airsonic's follow-up. For the addition of the UI, consider the viewpoint "necessary for Japanese-ready function" and "avoid competition with Airsonic".
  3.  **Coding standards** Language best-practices should be followed, comment generously, and avoid "clever" algorithms. It does not do simple refactoring. It is because it hinders Airsonic's follow-up.
  4.  **I will fix it!** I love you. Plese try [submiting a patch](https://github.com/tesshucom/jpsonic/issues?q=is%3Aissue+is%3Aopen+label%3Apatches-welcome) for an open Issue.
  5.  **Good thought that!** If you would like to have ideas and suggestions on Japanese processing at ease, fill in the comments on the author's site. Let's go easy. I love you. I said twice.

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

