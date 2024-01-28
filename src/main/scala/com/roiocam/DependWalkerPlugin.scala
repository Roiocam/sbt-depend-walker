package com.roiocam

import com.roiocam.TaskDefine.{ScopeKeyMatcher, WalkTask}
import sbt.Keys.*
import sbt.*
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

  def scopedKeyIsMatch(
      currentKey: ScopedKey[_],
      executeTaskMatcher: ScopeKeyMatcher,
      currentProject: ProjectRef
  ): Boolean = {
    val predicate: (ScopedKey[_], ScopedKey[_]) => Boolean = (sk, vk) =>
      executeTaskMatcher.mode match {
        case CheckConfig => sk.scope.config == vk.scope.config
        case CheckTask   => sk.key == vk.key
        case CheckBoth => sk.key == vk.key && sk.scope.config == vk.scope.config
        case _         => false
      }

    currentKey.scope.project match {
      case Select(projectRef) =>
        projectRef == currentProject && predicate(
          currentKey,
          executeTaskMatcher.key
        )
      case _ => false
    }
  }

  def dependWalk(
      dependencies: Set[_ <: ScopedKey[_]],
      expectDependMatcher: ScopeKeyMatcher
  )(implicit cMap: Map[Def.ScopedKey[_], Def.Flattened]): Boolean = {
    val predicate: (ScopedKey[_], ScopedKey[_]) => Boolean = (sk, vk) =>
      expectDependMatcher.mode match {
        case CheckConfig => sk.scope.config == vk.scope.config
        case CheckTask   => sk.key == vk.key
        case CheckBoth => sk.scope.config == vk.scope.config && sk.key == vk.key
        case _         => false
      }

    dependencies.exists { sk =>
      predicate(sk, expectDependMatcher.key) match {
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

  def check(
      currentProject: ProjectRef,
      walkTask: WalkTask
  )(implicit cMap: Map[Def.ScopedKey[_], Def.Flattened]): Unit = {

    val executeTaskOnCurrentProject = cMap.collect {
      case (currentKey, _)
          if scopedKeyIsMatch(
            currentKey,
            walkTask.executeTask,
            currentProject
          ) =>
        currentKey
    }
    // walking dependency tree to  check
    for (key <- executeTaskOnCurrentProject) {
      val depends = cMap.get(key) match {
        case Some(c) => c.dependencies.toSet;
        case None    => Set.empty
      }
      if (!dependWalk(depends, walkTask.expectDepend)) {
        throw new Exception(
          s"Expect depend <${walkTask.expectDepend}> not in the dependency tree of ${walkTask.executeTask}"
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
