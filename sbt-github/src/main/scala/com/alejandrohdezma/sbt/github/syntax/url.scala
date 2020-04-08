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

package com.alejandrohdezma.sbt.github.syntax

import sbt.URI
import sbt.URL

object url {

  implicit class UrlOps(private val url: URL) extends AnyVal {

    /**
     * Adds a query param with the given `key`/`value` pair to
     * this `URL` ad returns it.
     */
    @SuppressWarnings(Array("scalafix:Disable.toURI"))
    def withQueryParam(key: String, value: String): URL = {
      val uri = url.toURI

      val query = Option(uri.getQuery)
        .map(_ + s"&$key=$value")
        .getOrElse(s"$key=$value")

      new URI(
        uri.getScheme,
        uri.getAuthority,
        uri.getPath,
        query,
        uri.getFragment
      ).toURL
    }

  }

}
