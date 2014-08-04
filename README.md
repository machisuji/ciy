# CIy

A simple little CI server.

## Build

CIy uses gradle 2 for its build.

## Important Tasks

Here are the most important tasks you should know of.

### Bundling the application

`gradle war` will build a war archive which you can run using the servlet container of your choice.

### Running with embedded Jetty

`gradle jettyRun`

If you want to run it with automatic code reloading use `gradle jettyDev`.

### Open Scala console with CIy classpath

`gradle -q scalaConsole`

### Recompiling the code (without bundling)

`gradle classes`
