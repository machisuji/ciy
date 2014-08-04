package ciy

import org.scalatra._
import org.wandledi.wandlet.scala.{Wandlet, Page}
import org.wandledi.scala._
import java.io.File

class Dashboard extends CiyStack with Wandlet {

  def httpServletResponse: javax.servlet.http.HttpServletResponse = response // required by Wandlet

  get("/") {
    println("index")

    <html>
      <body>
        <h1>Hello, world!</h1>
        Say <a href="hello-wandledi">hello to Wandledi</a>.
      </body>
    </html>
  }

  get("/hello-wandledi") {
    println("hello wandledi")

    val title = "CIy - A tiny, simple CI server."
    val content = "It is now! " + (new java.util.Date().toString)
    val ip = request.getRemoteAddr

    render(HelloWandledi(title, content, ip))
  }

  get("/pull") {
    showFile(CI.pullCommand.logFile.map(new File(_)), "Pull Log")
  }

  get("/test") {
    showFile(CI.testCommand.logFile.map(new File(_)), "Test Log")
  }

  get("/run") {
    CI.flushLog()
    showFile(Some(new File(CI.runCommand.logFile)), "Run Log")
  }

  post("/bbhook") {
    CI.newRevisionPushed()

    Ok("ok")
  }

  get("/redeploy") { // ja ja ja das ist alles noch zum Rumspielen
    CI.newRevisionPushed()

    Ok("ok")
  }

  def showFile(file: Option[File], title: String) = {
    val colorCode = "(" + 27.toChar + "\\[\\d+m)"
    <html>
      <body>
        <h1>{ title }</h1>
        {
          if (file.exists(_.exists))
            io.Source.fromFile(file.get).getLines.map(
              line => <div style="width: 100%;"><span>{line.replaceAll(colorCode, "")}</span></div>)
          else
            <p>"no content"</p>
        }
      </body>
    </html>
  }
}

case class HelloWandledi(title: String, content: String, ip: String) extends Page("/hello-wandledi.html") {
  $("title").text = title
  $("h1").text = title

  $("#content") insertLast content
  $("#ip").text = ip
}
