package magenta
package tasks

// represents a command line to execute
//  including the ability to actually execute and become remote
case class CommandLine(commandLine: List[String], successCodes: List[Int] = List(0)) {
  lazy val quoted = (commandLine) map quoteIfNeeded mkString " "
  private def quoteIfNeeded(s: String) = if (s.contains(" ")) "\"" + s + "\"" else s

  def suppressor(filteredOut: String => Unit) = { line:String =>
    if ( !line.startsWith("tcgetattr") &&
      !line.startsWith("Warning: Permanently added") )
        filteredOut(line)
  }

  def run() {
    import sys.process._
    MessageBroker.infoContext("$ " + quoted) {
      val returnValue = commandLine ! ProcessLogger(MessageBroker.commandOutput(_), suppressor(MessageBroker.commandError(_)))
      MessageBroker.verbose("return value " + returnValue)
      if (!successCodes.contains(returnValue)) {
        MessageBroker.fail("Exit code %d from command: %s" format (returnValue, quoted))
      }
    }
  }
}

object CommandLine {
  implicit def fromStringList(c: List[String]) = CommandLine(c)
  implicit def fromString(c: String) = CommandLine(List(c))
}