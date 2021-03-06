ThisBuild / scmInfo := Some(
  ScmInfo(url("http://example.com"), "scm:git:https://alejandrohdezma@github.com/alejandrohdezma/sbt-github.git")
)
ThisBuild / githubEnabled      := true
ThisBuild / githubOrganization := "different-org"
ThisBuild / githubToken        := Token("1234")
ThisBuild / githubApiEntryPoint := {
  val github = baseDirectory.value / "github"

  github.listFiles.foreach { file =>
    val source  = scala.io.Source.fromFile(file)
    val content = source.mkString.replaceAllLiterally("{{base_directory}}", s"file://$github")

    source.close()

    val bw = new java.io.PrintWriter(file)
    bw.write(content)
    bw.close()
  }

  url(s"file://${github / "entrypoint.json"}")
}

TaskKey[Unit]("check", "Checks all the elements downloaded from the Github API are correct") := {
  assert(organizationName.value == "A Different Organization")
  assert(organizationHomepage.value.contains(url("https://example.com/different")))
  assert(organizationEmail.value.contains("different@example.com"))
}
