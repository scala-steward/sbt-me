package com.alejandrohdezma.sbt.me

import java.time.Year

import sbt.Def.Setting
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import com.alejandrohdezma.sbt.me.github.Repository

/**
 * This plugin automatically enables reloading on sbt source changes and
 * adds POM-related settings like description, organization, license, homepage...
 *
 * All the settings values are downloaded from the repository and current user
 * information from the Github API.
 *
 * This will only happen during the release stage in Travis CI, since its only
 * needed during this phase.
 */
@SuppressWarnings(Array("scalafix:DisableSyntax.=="))
object SbtMePlugin extends AutoPlugin {

  object autoImport {

    type Contributors = github.Contributors
    val Contributors = github.Contributors

    type Collaborators = github.Collaborators
    val Collaborators = github.Collaborators

    type Collaborator = github.Collaborator
    val Collaborator = github.Collaborator

    val contributors = settingKey[Contributors](
      "List of contributors downloaded from Github"
    )

    val collaborators = settingKey[Collaborators](
      "List of collaborators downloaded from Github"
    )

    val extraCollaborators = settingKey[List[Lazy[Collaborator]]] {
      "Extra collaborators that should be always included (independent of whether they are contributors or not)"
    }

    val excludedContributors = settingKey[List[String]] {
      "ID (Github login) of the contributors that should be excluded from the list, like bots"
    }

    val repository = settingKey[Option[Repository]] {
      "Repository information downloaded from Github"
    }

    val downloadInfoFromGithub = settingKey[Boolean] {
      "Whether sbt-me should download information from Github or not"
    }

    val yearRange = settingKey[Option[String]] {
      "Range of years in which the project has been active"
    }

  }

  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  override def buildSettings: Seq[Setting[_]] = Seq(
    downloadInfoFromGithub := sys.env.contains("RELEASE"),
    excludedContributors   := List("scala-steward", "mergify[bot]"),
    extraCollaborators     := List(),
    repository := {
      if (downloadInfoFromGithub.value)
        Some(Repository.get(info.value._1, info.value._2).fold(sys.error, identity))
      else None
    },
    contributors := repository.value.fold(Contributors(Nil)) {
      _.contributors(excludedContributors.value).fold(sys.error, identity)
    },
    collaborators := repository.value.fold(Collaborators(Nil)) {
      _.collaborators(contributors.value.list.map(_.login))
        .fold(sys.error, identity)
        .include(extraCollaborators.value.map(_.value))
    },
    developers := collaborators.value.developers,
    homepage   := repository.value.map(r => url(r.url)).orElse(homepage.value),
    licenses   := repository.value.map(_.licenses).getOrElse(licenses.value),
    startYear  := repository.value.map(_.startYear).orElse(startYear.value),
    yearRange := startYear.value.collect {
      case start if start == Year.now.getValue => s"$start"
      case start                               => s"$start-${Year.now.getValue}"
    }
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    description := repository.value.map(_.description).getOrElse(description.value),
    organizationName := repository.value
      .flatMap(_.organization)
      .flatMap(_.fold(sys.error, identity).name)
      .getOrElse(organizationName.value),
    organizationHomepage := repository.value
      .flatMap(_.organization)
      .flatMap(_.fold(sys.error, identity).url)
      .map(sbt.url)
      .orElse(organizationHomepage.value)
  )

  /** Gets the Github user and repository from the git remote info */
  private val info = Def.setting {
    val identifier = """([^\/]+)"""

    val Connection = s"scm:git:https://github.com/$identifier/$identifier.git".r

    scmInfo.value.map(_.connection) match {
      case Some(Connection(owner, repo)) => (owner, repo)
      case None                          => sys.error("`scmInfo` is mandatory for this plugin to work")
      case Some(s)                       => sys.error(s"Invalid `scmInfo` connection value: $s")
    }
  }

}
