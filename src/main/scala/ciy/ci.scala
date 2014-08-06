package ciy

import scala.sys.process._
import scala.concurrent.SyncVar
import java.io.File

trait CI {
  private val process: SyncVar[(Process, FileProcessLogger)] = new SyncVar

  def pullCommand: SimpleCommand
  def testCommand: SimpleCommand
  def runCommand: CommandWithLog

  def flushLog(): Unit = {
    if (process.isSet) {
      process.get._2.flush()
    }
  }

  def newRevisionPushed(): Unit = {
    println("[CI] New revision pushed.")

    threadDo() {
      if (pull() && test()) {
        if (process.isSet) {
          println("[CI] Stopping current application ...")
          process.get._1.destroy()
          process.get._2.close()
          println("[CI] Application stopped")
        }
        run.foreach(process.put)
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
  def run(): Option[(Process, FileProcessLogger)] = {
    runCommand.deleteLog()
    if (beforeRun()) {
      Some(runCommand.start)
    } else {
      None
    }
  }

  def beforeRun(): Boolean = !beforeRunCommands.exists(_.run != 0)
  def beforeRunCommands: Seq[SimpleCommand] = Seq()

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
  val cwd = sys.env.get("CIY_CWD").getOrElse(throw new RuntimeException("Please set CIY_CWD."))
  val cfg = YAML.fromFile(new File(cwd, "ciy.yml").getAbsolutePath).getOrElse(YamlMap.empty)

  def pullCommand = SimpleCommand(
    cmd = cfg.string("pull.cmd").getOrElse(fail),
    cwd = cfg.string("pull.cwd").getOrElse(cwd),
    logFile = Some("pull_output.log"))

  def testCommand = SimpleCommand(
    cmd = cfg.string("test.cmd").getOrElse(fail),
    cwd = cfg.string("test.cwd").getOrElse(cwd),
    logFile = Some("test_output.log"))

  def runCommand = CommandWithLog(
    cmd = cfg.string("run.cmd").getOrElse(fail),
    cwd = cfg.string("run.cwd").getOrElse(cwd),
    logFile = "run_output.log")

  override def beforeRunCommands = cfg.map("run.before").map(before =>
    before.keys.toSeq.flatMap(key => cfg.map(s"run.before.$key")).map(cmd =>
      SimpleCommand(
        cmd = cmd.string("cmd").getOrElse(fail),
        cwd = cmd.string("cwd").getOrElse(cwd),
        logFile = Some("run_output.log")))).getOrElse(Seq())
}
