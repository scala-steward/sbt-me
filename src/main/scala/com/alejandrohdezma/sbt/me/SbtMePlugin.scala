package com.alejandrohdezma.sbt.me

import scala.sys.process._

import sbt.Def.Setting
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{settingKey, url, AutoPlugin, Def, PluginTrigger, Plugins}

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

  }

  import autoImport._

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  override def globalSettings: Seq[Setting[_]] = Seq(
    downloadInfoFromGithub := sys.env.contains("RELEASE"),
    excludedContributors   := List("scala-steward", "mergify[bot]"),
    extraCollaborators     := List(),
    repository := {
      if (downloadInfoFromGithub.value)
        Some(Repository.get(user, repo).fold(sys.error, identity))
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
    startYear  := repository.value.map(_.startYear).orElse(startYear.value)
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
  private lazy val (user, repo): (String, String) = {
    val identifier = """([^\/]+)"""

    val GithubHttps = s"https://github.com/$identifier/$identifier.git".r
    val GithubSsh   = s"git@github.com:$identifier/$identifier.git".r

    "git ls-remote --get-url origin".!!.trim() match {
      case GithubHttps(user, repo) => (user, repo)
      case GithubSsh(user, repo)   => (user, repo)
      case _                       => sys.error("Unable to get info from `git ls-remote --get-url origin`")
    }
  }

}
