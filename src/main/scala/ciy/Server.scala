package ciy

import org.eclipse.jetty.servlet._
import org.eclipse.jetty.webapp._

import java.nio.file._
import java.nio.file.StandardWatchEventKinds._
import java.util.concurrent.TimeUnit
import java.net.InetSocketAddress

object Server {
  def main(implicit args: Array[String]): Unit = {
    val war = args.head
    val port = arg("p", "port").map(_.toInt).getOrElse(8080)
    val classpath = arg("c", "classpath")
    val reload = args.exists(a => a == "-r" || a == "--reload")
    val timeout = arg("t", "timeout").map(_.toLong).getOrElse(11500l)
    val context = arg("x", "context").getOrElse("/")

    if (classpath.isDefined && reload) runReloadingServer(war, context, port, classpath.get, timeout)
    else runServer(war, context, port, classpath)
  }

  def runServer(war: String, context: String, port: Int, classpath: Option[String]): Unit = {
    val srv = server(war, context, port, classpath)

    srv.start()
    srv.join()
  }

  def runReloadingServer(war: String, context: String, port: Int, classpath: String, timeout: Long): Unit = {
    var srv: org.eclipse.jetty.server.Server = null

    onChange(classpath, initialCall = true, timeout = timeout) {
      if (srv ne null) {
        println("[info] Change detected. Restarting server ...")
        srv.stop()
      }

      srv = server(war, context, port, Some(classpath))
      srv.start()
      println("[info] waiting for change in classpath ...")
    }
  }

  def server(war: String, context: String = "/", port: Int = 8080, classpath: Option[String] = None) = {
    val server = new org.eclipse.jetty.server.Server(new InetSocketAddress("localhost", port))
    val webapp = new WebAppContext()

    webapp.setContextPath(context)
    webapp.setWar(war)
    classpath.foreach(webapp.setExtraClasspath)

    server.setHandler(webapp)

    server
  }

  def onChange(
    dir: String,
    initialCall: Boolean = false,
    timeout: Long = 10000
  )(block: => Unit): Unit = {
    val path = FileSystems.getDefault().getPath(dir)
    val watcher = FileSystems.getDefault().newWatchService()
    val registrationKey = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)

    if (initialCall) block

    while (true) {
      val ms = System.currentTimeMillis
      waitForChange(watcher, timeout)
      if (System.currentTimeMillis - ms >= timeout) {
        block
      }
    }
  }

  def waitForChange(
    watcher: WatchService,
    timeout: Long,
    lastEvent: Option[Long] = None
  ): Unit = {
    val key = lastEvent
      .map(_ => watcher.poll(timeout, TimeUnit.MILLISECONDS))
      .getOrElse(watcher.take())

    if (key eq null) return ()

    val events = key.pollEvents()

    var i = 0
    val length = events.size()

    while (i < length) {
      val e = events.get(i)

      println("    >> " + e.kind.name + " " + e.context)
      i += 1
    }

    key.reset()

    if (lastEvent.isDefined) {
      if (System.currentTimeMillis - lastEvent.get < timeout) {
        // consecutive change, wait some more until it's quiet
        waitForChange(watcher, timeout, lastEvent = Some(System.currentTimeMillis))
      } else {
        () // no more changes, let's return
      }
    } else {
      // make sure no more changes are coming for now
      waitForChange(watcher, timeout, lastEvent = Some(System.currentTimeMillis))
    }
  }

  def arg(keys: String*)(implicit args: Array[String]): Option[String] =
    args.sliding(2, 1).find {
      case Array(key, _) => keys.contains(key.replaceAll("^\\-\\-?", ""))
    }.map {
      case Array(_, value) => value
    }
}
