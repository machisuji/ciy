package ciy

import scala.sys.process._
import scala.concurrent.SyncVar
import java.io.File

class CI {
  private val process: SyncVar[(Process, FileProcessLogger)] = new SyncVar

  def flushLog(): Unit = {
    if (process.isSet) {
      process.get._2.flush()
    }
  }

  def newRevisionPushed(): Unit = {
    println("[CI] New revision pushed.")

    threadDo() {
      if (pull() && testBuild()) {
        if (process.isSet) {
          println("[CI] Stopping current application ...")
          process.get._1.destroy()
          process.get._2.close()
          println("[CI] Application stopped")
        }
        process put runApp()
      }
    }
  }

  def pull(): Boolean = {
    println("[CI] Checking out latest revision.")
    println("[CI] " + pullCommand)

    val logger = new FileProcessLogger(pullLogFile)
    val pb = Process(pullCommand, pullCwd)
    val process = pb.run(logger)

    val result = process.exitValue == 0

    if (result) {
      println("[CI] Checkout successful.")
    } else {
      println("[CI] Checkout failed.")
    }

    logger.flush()
    logger.close()

    result
  }

  def pullCommand = "echo insert pull command here"
  def pullCwd = new File(".")
  def pullLogFile = new File("pull_output.log")

  def testBuild(): Boolean = {
    // substitue for actual implementation
    println("[CI] Testing build ...")
    println("[CI] " + testCommand)

    val logger = new FileProcessLogger(testLogFile)
    val pb = Process(testCommand, testCwd)
    val process = pb.run(logger)

    val result = process.exitValue == 0

    if (result) {
      println("[CI] Test successful")
    } else {
      println("[CI] Test failed")
    }

    logger.flush()
    logger.close()

    result
  }

  def testCommand = "echo insert test command here"
  def testCwd = new File(".")
  def testLogFile = new File("test_output.log")

  def runApp(): (Process, FileProcessLogger) = {
    println("[CI] Running app ...")
    println("[CI] " + runCommand)

    val logger = new FileProcessLogger(runLogFile)
    val pb = Process(runCommand, runCwd)
    val process = pb.run(logger)

    (process, logger)
  }

  def runCommand = "echo insert run command here"
  def runCwd = new File(".")
  def runLogFile = new File("run_output.log")

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
  val cwd = sys.env.get("CIY_CWD")

  override def pullCommand = sys.env.get("CIY_PULL_CMD").getOrElse("echo pull command")
  override def pullCwd = new File(sys.env
    .get("CIY_PULL_CWD")
    .orElse(cwd)
    .getOrElse("."))

  override def testCommand = sys.env.get("CIY_TEST_CMD").getOrElse("echo test command")
  override def testCwd = new File(sys.env
    .get("CIY_TEST_CWD")
    .orElse(cwd)
    .getOrElse("."))

  override def runCommand = sys.env.get("CIY_RUN_CMD").getOrElse("echo run command")
  override def runCwd = new File(sys.env
    .get("CIY_RUN_CWD")
    .orElse(cwd)
    .getOrElse("."))
}
