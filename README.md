# sbt-depend-walker

Walk on the dependency tree of Build to see if a specific task exists.

## Usage

Use the Apache Pekko project as an example

### 1. add plugin

add these line into `project/plugin.sbt`

```sbt
addSbtPlugin("com.roiocam" % "sbt-depend-walker" % "1.0.0")
```

### 2. create Depend-Walker settings

create `PekkoDependWalker` scala class on `project/PekkoDependWalker.scala`

```scala

import Jdk9.CompileJdk9
import com.roiocam.DependWalkerPlugin.autoImport.walkTasks
import com.roiocam.TaskDefine._
import com.roiocam._
import sbt.Keys._
import sbt._

object PekkoDependWalker {


  lazy val walkSettings = Seq(
    walkTasks := Seq(
      WalkTask(
        ScopeKeyMatcher((Compile / packageBin).scopedKey, CheckBoth),
        ScopeKeyMatcher((CompileJdk9 / compile).scopedKey, CheckConfig)
      )
    )
  )

}
```

### 3. enable plugin and apply settings

```diff
lazy val actorTyped = pekkoModule("actor-typed")
  .dependsOn(actor, slf4j)
  .settings(AutomaticModuleName.settings("pekko.actor.typed"))
  .settings(Dependencies.actorTyped)
  .settings(OSGi.actorTyped)
  .enablePlugins(Jdk9)
+ .settings(PekkoDependWalker.walkSettings)
+ .enablePlugins(DependWalkerPlugin)
```


## Output

When depend-walker can not detect expected task in dependency tree, it will print ERROR logs like this:

```log
pekko > dependWalkerCheck
[error] stack trace is suppressed; run last actor-typed / dependWalkerCheck for the full output
[error] stack trace is suppressed; run last remote / dependWalkerCheck for the full output
[error] stack trace is suppressed; run last cluster-sharding / dependWalkerCheck for the full output
[error] stack trace is suppressed; run last stream / dependWalkerCheck for the full output
[error] (actor-typed / dependWalkerCheck) java.lang.Exception: Expect depend <ScopeKeyMatcher(ScopedKey(This / Select(ConfigKey(CompileJdk9)) / This,compile),CheckConfig)> not in the dependency tree of ScopeKeyMatcher(ScopedKey(This / Select(ConfigKey(compile)) / This,packageBin),CheckBoth)
[error] (remote / dependWalkerCheck) java.lang.Exception: Expect depend <ScopeKeyMatcher(ScopedKey(This / Select(ConfigKey(CompileJdk9)) / This,compile),CheckConfig)> not in the dependency tree of ScopeKeyMatcher(ScopedKey(This / Select(ConfigKey(compile)) / This,packageBin),CheckBoth)
[error] (cluster-sharding / dependWalkerCheck) java.lang.Exception: Expect depend <ScopeKeyMatcher(ScopedKey(This / Select(ConfigKey(CompileJdk9)) / This,compile),CheckConfig)> not in the dependency tree of ScopeKeyMatcher(ScopedKey(This / Select(ConfigKey(compile)) / This,packageBin),CheckBoth)
[error] (stream / dependWalkerCheck) java.lang.Exception: Expect depend <ScopeKeyMatcher(ScopedKey(This / Select(ConfigKey(CompileJdk9)) / This,compile),CheckConfig)> not in the dependency tree of ScopeKeyMatcher(ScopedKey(This / Select(ConfigKey(compile)) / This,packageBin),CheckBoth)
[error] Total time: 8 s, completed 2024/1/29 1:06:28
```


## License

Copyright 2024~ Roiocam

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.