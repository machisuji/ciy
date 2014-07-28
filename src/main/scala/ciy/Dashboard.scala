package ciy

import org.scalatra._
import org.wandledi.wandlet.scala.{Wandlet, Page}
import org.wandledi.scala._

class Dashboard extends CiyStack with Wandlet {

  def httpServletResponse: javax.servlet.http.HttpServletResponse = response // required by Wandlet

  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Say <a href="hello-wandledi">hello to Wandledi</a>.
      </body>
    </html>
  }

  get("/hello-wandledi") {
    val title = "CIy - A tiny, simple CI server."
    val content = "Hello Wandledi!"
    val ip = request.getRemoteAddr

    render(HelloWandledi(title, content, ip))
  }
}

case class HelloWandledi(title: String, content: String, ip: String) extends Page("/hello-wandledi.html") {
  $("title").text = title
  $("h1").text = title

  $("#content") insertLast content
  $("#ip").text = ip
}
