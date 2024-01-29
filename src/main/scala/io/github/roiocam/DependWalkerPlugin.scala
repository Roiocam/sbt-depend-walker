package io.github.roiocam

import TaskDefine.{ScopeKeyMatcher, WalkTask}
import sbt.Keys._
import sbt._
import sbt.internal.BuildStructure
import sbt.plugins.JvmPlugin

object DependWalkerPlugin extends AutoPlugin {

  import autoImport._

  override def requires = JvmPlugin
  override def trigger = NoTrigger

  object autoImport {
    lazy val dependWalkerCheck =
      taskKey[Unit]("Report which jars are in each scope.")
    lazy val walkTasks: SettingKey[Seq[WalkTask]] =
      settingKey[Seq[WalkTask]]("Depend Walker task")

  }

  /** Used to match whether two ScopedKey are eligible
    * @param currentKey
    * @param executeTaskMatcher
    * @param currentProject
    * @return
    */
  def scopedKeyIsMatch(sk: ScopedKey[_], matcher: ScopeKeyMatcher): Boolean = {
    val expectConfig = matcher.key.scope.config
    val expectKey = matcher.key.key
    matcher.mode match {
      case CheckConfig => sk.scope.config == expectConfig
      case CheckTask   => sk.key == expectKey
      case CheckBoth   => sk.key == expectKey && sk.scope.config == expectConfig
      case _           => false
    }
  }

  /** A method use to walking on the dependency tree.
    * @param dependencies
    * @param expectDependMatcher
    * @param cMap
    * @return
    */
  def dependWalk(
      dependencies: Set[_ <: ScopedKey[_]],
      expectDependMatcher: ScopeKeyMatcher
  )(implicit cMap: Map[Def.ScopedKey[_], Def.Flattened]): Boolean = {
    dependencies.exists { sk =>
      scopedKeyIsMatch(sk, expectDependMatcher) match {
        case true => true
        case false =>
          cMap.get(sk).exists { flattened =>
            val dependencies = flattened.dependencies.toSet
            dependencies.nonEmpty && dependWalk(
              dependencies,
              expectDependMatcher
            )
          }
      }
    }
  }

  /** Check dependency exist in the tree.
    *
    * @param currentProject
    *   check project
    * @param walkTask
    *   job define
    * @param cMap
    *   global project settings.
    */
  def check(
      currentProject: ProjectRef,
      walkTask: WalkTask
  )(implicit cMap: Map[Def.ScopedKey[_], Def.Flattened]): Unit = {
    val executeTaskOnCurrentProject = cMap.flatMap { case (currentKey, _) =>
      for {
        project <- currentKey.scope.project.toOption
        if project == currentProject && scopedKeyIsMatch(
          currentKey,
          walkTask.executeTask
        )
      } yield currentKey
    }
    // walking dependency tree to  check
    for (key <- executeTaskOnCurrentProject) {
      println(
        s"Begin check depend on: (${currentProject.project} / ${key.scope.config} / ${key.key})"
      )
      val depends = cMap.get(key) match {
        case Some(c) => c.dependencies.toSet;
        case None    => Set.empty
      }
      val expectDepend = walkTask.expectDepend
      if (dependWalk(depends, expectDepend)) {
        println(
          s"Depend (${expectDepend.key.scope.config} / ${expectDepend.key.key}) verified in the tree of (${currentProject.project} / ${key.scope.config} / ${key.key})"
        )
      } else {
        throw new Exception(
          s"Expect depend (${expectDepend.key.scope.config} / ${expectDepend.key.key}) not in the dependency tree of (${walkTask.executeTask.key.scope.config} / ${walkTask.executeTask.key.key})"
        )
      }
    }
  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    walkTasks := Seq.empty,
    dependWalkerCheck := {
      implicit val display = Project.showContextKey(state.value)
      val structure: BuildStructure = Project.extract(state.value).structure
      val currentProjectRef = thisProjectRef.value

      // Crawl all configurations of the current project
      val comp = Def.compiled(structure.settings, true)(
        structure.delegates,
        structure.scopeLocal,
        display
      )
      implicit val cMap = Def.flattenLocals(comp)

      walkTasks.value.foreach(ct => check(currentProjectRef, ct))

    }
  )
}
