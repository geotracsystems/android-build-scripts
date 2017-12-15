# Library Versioning

## Library Side

|Branch|Published Version|Example|
|---|---|---|
|develop|\<major\>.x-develop-SNAPSHOT|1.x-develop-SNAPSHOT|
|feature/\<JIRA key\>-\<descriptor\>|\<JIRA key\>-SNAPSHOT|GAT-999-SNAPSHOT|
|release/\<version\>|\<version\>-SNAPSHOT|1.0-SNAPSHOT|
|hotfix/\<version\>|\<version\>-SNAPSHOT|1.0.1-SNAPSHOT|
|master|\<version\>|1.0|
|Any other \*/\<branch name\>|**Fails to build** - create a task|quick-fulfilment-fix|

## App Side

On the app side, decisions on which concrete library to use will be made based on the current branch. Each app will define its desired version, which will be used for releases, but a dynamic version will be used on develop to make sure the most recent library is also being used during development.

Which version of the library is used will depend on the branch the app is currently building on. For an app that depends on library version \<major\>.\<minor\>.\<revision\>:

|Branch|Library Version|Example|
|---|---|---|
|develop|\<major\>.x-develop-SNAPSHOT|1.x-develop-SNAPSHOT|
|release/\<version\> hotfix/\<version\> master|\<major\>.\<minor\>.\<revision\>|1.2.1|
|Any other \*/\<branch name\>|\<major\>.x-develop-SNAPSHOT|1.x-develop-SNAPSHOT|

Release branches are likely very short-lived on a library, and are never automatically pointed to by an app's branch. However, testing against a specific release branch's snapshots can be done by temporarily setting an app's version to something like 1.1-SNAPSHOT instead of 1.0 and not asking for a dependency version from _lib\_common.gradle_.

Features are never pointed to directly. If you are working on a project where the library changes must be made at the same time as the app changes, the library and app must reside in subprojects of the same root Gradle project. Then, use [Gradle dependency substitution](https://docs.gradle.org/current/userguide/dependency_management.html#sec:module_to_project_substitution) to use the current development version. If that is not the case, the library changes should be testable within the original project and should be code reviewed and merged into develop before they can be used in another project.