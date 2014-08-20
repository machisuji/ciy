package ciy

import scala.sys.process._
import scala.concurrent.SyncVar
import java.io.File

trait CI {
  private val running: SyncVar[RunningCommand] = new SyncVar

  def pullCommand: Command
  def testCommand: Command
  def runCommand: Command

  def newRevisionPushed(): Unit = {
    println("[CI] New revision pushed.")

    threadDo() {
      if (pull() && test()) {
        if (running.isSet) {
          val cmd = running.get

          println("[CI] Stopping current application ...")
          cmd.process.destroy()
          cmd.output.close()
          println("[CI] Application stopped")
        }
        run.foreach(running.put)
      }
    }
  }

  def pull(): Boolean = {
    pullCommand.deleteLog()
    pullCommand.run == 0
  }

  def test(): Boolean = {
    testCommand.deleteLog()
    testCommand.run == 0
  }
  def run(): Option[RunningCommand] = {
    runCommand.deleteLog()
    if (beforeRun()) {
      Some(runCommand.start)
    } else {
      None
    }
  }

  def beforeRun(): Boolean = !beforeRunCommands.exists(_.run != 0)
  def beforeRunCommands: Seq[Command] = Seq()

  def threadDo(name: String = "", daemon: Boolean = true)(block: => Unit) {
    val run = new Runnable {
      override def run(): Unit = block
    }
    val thread = new Thread(run)

    if (name != "") {
      thread.setName(name)
    }
    thread.setDaemon(daemon)
    thread.start()
  }

  def threadDo(block: => Unit): Unit = threadDo()(block)
}

object CI extends CI {
  def fail = throw new RuntimeException("Please configure pull, test and run commands.")
  def cwd = sys.env.get("CIY_CWD").getOrElse(throw new RuntimeException("Please set CIY_CWD."))
  def config = YAML.fromFile(new File(cwd, ".ciy.yml").getAbsolutePath).getOrElse(YamlMap.empty)

  def pullCommand = {
    val cfg = config

    Command(
      cmd = cfg.string("pull.cmd").orElse(cfg.string("pull")).getOrElse(fail),
      cwd = cfg.string("pull.cwd").map(expand(_, cwd)).getOrElse(cwd),
      logFile = "pull_output.log")
  }

  def testCommand = {
    val cfg = config

    Command(
      cmd = cfg.string("test.cmd").orElse(cfg.string("test")).getOrElse(fail),
      cwd = cfg.string("test.cwd").map(expand(_, cwd)).getOrElse(cwd),
      logFile = "test_output.log")
  }

  def runCommand = {
    val cfg = config

    Command(
      cmd = cfg.string("run.cmd").orElse(cfg.string("run")).getOrElse(fail),
      cwd = cfg.string("run.cwd").map(expand(_, cwd)).getOrElse(cwd),
      logFile = "run_output.log")
  }

  override def beforeRunCommands = {
    val cfg = config

    val plainCommands = cfg.list[String]("run.before")
      .map(before => before.map(cmd => Command(cmd, cwd = cwd, logFile = "run_output.log")))
      .getOrElse(Seq())
    val commandsWithCwd = cfg.list[YamlMap]("run.before")
      .map(before => before.map(cmd => Command(
        cmd = cmd.string("cmd").getOrElse(fail),
        cwd = cmd.string("cwd").map(expand(_, cwd)).getOrElse(fail),
        logFile = "run_output.log")))
      .getOrElse(Seq())

    plainCommands ++ commandsWithCwd
  }

  private def expand(path: String, cwd: String) =
    path.replaceAll("(\\$cwd)|(\\.)", cwd.replaceAll("/$", ""))
}
