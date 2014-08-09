# CIy

A simple little CI server.

## Build

CIy uses gradle 2 for its build.

## Important Tasks

Here are the most important tasks you should know of.

### Bundling the application

`./gradle war` will build a war archive which you can run using the servlet container of your choice.

### Running with embedded Jetty

`./gradle jettyRun`

If you want to run it with automatic code reloading use `gradle jettyDev`.

### Open Scala console with CIy classpath

`./gradle console`

### Recompiling the code (without bundling)

`./gradle classes`

## Configuration

### Java Home

When you run `./gradle` the java home will be assigned to the contents of the file `.java-home` if present.
This way you can use different Java versions to run gradle with.
The minimum required version is Java 7. If your default Java is 6 you can simply point `.java-home` to a JDK 7 to be able to run CIy.

### Working Directory

CIy needs to be configured with a working directory to run in.
This is done in the environment variable `CIY_CWD`.
Meaning you could start it like this:

    CIY_CWD=/path/to/my/project ./gradle jettyRun

### Project

Target projects can be configured through a file called `.ciy.yml`.
In that file you can configure the 3 available commands pull, test and run.

    pull: "./simple command"
    test:
      cmd: "./sbt test"
      cwd: "/path/to/test/working/directory"
    run:
      cmd: "./sbt run"
      before:
        - "npm install"
        - {cmd: "grunt", cwd: "/path/to/where/package.json/is"}

You can either give a string which will be just a command to be executed in CIy's working directory.
Alternatively you can use another map which may accept further keys such as `cwd` to have another
working directory for a single command.
If you do not provide a `cwd` the default working directory passed to CIy (`CIY_CWD`) is used.

The before key accepts a list of commands to be executed before the project is run.
Again you can either provide a string for just a command or a map for a command along with a custom working directory.
