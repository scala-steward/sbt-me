/*
 * Copyright 2019-2020 Scala Steward <https://github.com/scala-steward>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alejandrohdezma.sbt.github.sbtheader

import sbt.Keys._
import sbt.{Def, _}

import com.alejandrohdezma.sbt.github.SbtGithubPlugin
import com.alejandrohdezma.sbt.github.SbtGithubPlugin.autoImport._
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import de.heikoseeberger.sbtheader.License._
import de.heikoseeberger.sbtheader.{HeaderPlugin, LicenseStyle, SpdxLicense}

/**
 * Populates the `headerLicense` setting from [[https://github.com/sbt/sbt-header sbt-header]]
 * with values extracted from Github by `SbtGithubPlugin`:
 *
 *  - '''Year''': The information stored in `yearRange`.
 *  - '''Copyright Owner''': The information stored in `copyrightOwner`, provided by this own
 *   plugin. Defaults to `organizationName` value if there is no value for `organizationHomepage` or
 *   `organizationName <organizationHomepage>` if it is present.
 *
 *  The `licenseStyle` setting can be used to tweak the style of the autogenerated headers.
 *  Defaults to `Detailed`.
 */
object SbtGithubHeaderPlugin extends AutoPlugin {

  object autoImport {

    val copyrightOwner = settingKey[String] {
      "The name of the copyright owner to be used in file header licenses." +
        " Defaults to `organizationName` value if there is no value for `organizationHomepage` or " +
        "`organizationName <organizationHomepage>` if it is present."
    }

    val licenseStyle = settingKey[LicenseStyle] {
      "The license style to be used. Can be `Detailed` or `SpdxSyntax`. Defaults to Detailed."
    }

  }

  import autoImport._

  override def trigger = allRequirements

  override def requires: Plugins = HeaderPlugin && SbtGithubPlugin

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    licenseStyle := LicenseStyle.Detailed
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    headerLicense := createHeaderLicense.value,
    copyrightOwner := {
      organizationHomepage.value
        .map(url => s"${organizationName.value} <$url>")
        .getOrElse(organizationName.value)
    }
  )

  private val licenseFor: String => Option[SpdxLicense] = {
    case "Apache-2.0"   => Some(ALv2)
    case "MIT"          => Some(MIT)
    case "MPL-2.0"      => Some(MPLv2)
    case "BSD-2-Clause" => Some(BSD2Clause)
    case "BSD-3-Clause" => Some(BSD3Clause)
    case "GPL-3.0"      => Some(GPLv3)
    case "LGPL-3.0"     => Some(LGPLv3)
    case "AGPL-3.0"     => Some(AGPLv3)
    case _              => None
  }

  val createHeaderLicense = Def.setting {
    for {
      (name, _) <- licenses.value.headOption
      license   <- licenseFor(name)
      year      <- yearRange.value
    } yield license(year, copyrightOwner.value, licenseStyle.value)
  }

}
