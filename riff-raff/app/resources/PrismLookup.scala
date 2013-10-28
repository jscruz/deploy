package resources

import magenta.{Stage, Datum, Host, App}
import magenta.resources.{Data, Instances, Lookup}
import org.joda.time.DateTime

object PrismLookup extends Lookup {

  def lastUpdated: DateTime = ???

  def data = new Data {
    def keys: List[String] = ???
    def all: Map[String, List[Datum]] = ???
    def datum(key: String, app: App, stage: Stage): Option[Datum] = ???
  }

  def instances = new Instances {
    def get(app: App, stage: Stage): List[Host] = ???
    def all: List[Host] = ???
  }
}