package ciy

import org.scalatra._
import org.wandledi.wandlet.scala.{Wandlet, Page}
import org.wandledi.scala._
import java.io.File

class Dashboard extends CiyStack with Wandlet {

  settings.html5 = true

  def httpServletResponse: javax.servlet.http.HttpServletResponse = response // required by Wandlet

  get("/") {
    render(Index())
  }

  get("/pull") {
    Index.showFile(new File(CI.pullCommand.logFile))
  }

  get("/test") {
    Index.showFile(new File(CI.testCommand.logFile))
  }

  get("/run") {
    Index.showFile(new File(CI.runCommand.logFile))
  }

  post("/bbhook") {
    CI.newRevisionPushed()

    Ok("ok")
  }

  get("/redeploy") { // ja ja ja das ist alles noch zum Rumspielen
    CI.newRevisionPushed()

    Ok("ok")
  }
}

case class Index() extends Page("/index.html") {
  import Index._

  val pullLog = new File(CI.pullCommand.logFile)
  val testLog = new File(CI.testCommand.logFile)
  val runLog = new File(CI.runCommand.logFile)

  $("title").text = CI.cwd
  $(".masthead-brand").text = CI.cwd

  $("#last-pull").text = (new java.util.Date(pullLog.lastModified)).toString
  $("#last-test").text = (new java.util.Date(testLog.lastModified)).toString
  $("#last-run").text = (new java.util.Date(runLog.lastModified)).toString

  $("#pull-output").replace(contentsOnly = true, showFile(pullLog))
  $("#test-output").replace(contentsOnly = true, showFile(testLog))
  $("#run-output").replace(contentsOnly = true, showFile(runLog))
}

object Index {
  def showFile(file: File) = {
    val colorCode = "(" + 27.toChar + "\\[\\d+m)"

    if (file.exists)
      io.Source.fromFile(file).getLines.toList.map(line =>
        scala.xml.NodeSeq fromSeq Seq(<span>{line.replaceAll(colorCode, "")}</span>, <br/>)).flatten
    else
      <span>"no content"</span>
  }
}
