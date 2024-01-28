package com.roiocam

import sbt.ScopedKey

object TaskDefine {

  sealed case class ScopeKeyMatcher(key: ScopedKey[_], mode: CheckMode)
  sealed case class WalkTask(
      executeTask: ScopeKeyMatcher,
      expectDepend: ScopeKeyMatcher
  )

}
