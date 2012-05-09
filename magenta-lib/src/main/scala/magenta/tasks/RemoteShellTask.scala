package magenta
package tasks

import java.io.File
import com.decodified.scalassh.{SimplePasswordProducer, PublicKeyLogin, SSH}
import com.decodified.scalassh.PublicKeyLogin._
import scala.Some


trait RemoteShellTask extends ShellTask {
  def host: Host
  lazy val remoteCommandLine = CommandLine("ssh" :: "-qtt" :: host.connectStr :: commandLine.quoted :: Nil)

  override def execute(credentials: Credentials) { credentials.forScalaSsh match {
    case Some(credentials) => {
      val credentialsForHost = host.connectAs match {
        case Some(username) => credentials.copy(user = username)
        case None => credentials
      }
      SSH(host.name, credentialsForHost) { client =>
        client.exec(commandLine.quoted)
      }
    }
    case None => remoteCommandLine.run()
  }}

  lazy val description = "on " + host.name
  override lazy val verbose = "$ " + remoteCommandLine.quoted
}

case class Credentials(
  user: Option[String] = None,
  passphrase: Option[String] = None,
  keyFileLocation: Option[File] = None
) {
  lazy val forScalaSsh: Option[PublicKeyLogin] = for {
    u <- user
    p <- passphrase
  } yield PublicKeyLogin(u, SimplePasswordProducer(p),
      (keyFileLocation map (x => List(x.getPath))) getOrElse (DefaultKeyLocations))
}

object Credentials {
  def apply(user: String, passphrase: String, keyFileLocation: Option[File]): Credentials =
    Credentials(Some(user), Some(passphrase), keyFileLocation)
}