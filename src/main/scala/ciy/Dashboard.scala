package ciy

import org.scalatra._
import org.wandledi.wandlet.scala.{Wandlet, Page}
import org.wandledi.scala._

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

  post("/bbhook") {
    println("pbhook")

    val json = scala.util.parsing.json.JSON.parseFull(request.body)
    println("json: " + json)

    Ok("alright")
  }
}

case class HelloWandledi(title: String, content: String, ip: String) extends Page("/hello-wandledi.html") {
  $("title").text = title
  $("h1").text = title

  $("#content") insertLast content
  $("#ip").text = ip
}
