package ci

import controllers.Logging
import scala.concurrent.{ExecutionContext, Future}
import rx.lang.scala.Observable
import ci.teamcity.{TeamCity, Job, BuildSummary, TeamCityWS}
import ci.teamcity.TeamCity.BuildTypeLocator
import concurrent.duration._
import conf.{Configuration, TeamCityMetrics}
import org.joda.time.DateTime

object Every extends Logging {

  def apply[T](frequency: Duration)
              (buildRetriever: => Observable[T])
              (implicit ec: ExecutionContext): Observable[T] = {
    (for {
      _ <- Observable.timer(1.second, frequency)
      builds <- buildRetriever.onErrorResumeNext(Observable.empty)
    } yield builds).publish.refCount
    // publish.refCount turns this from a 'cold' to a 'hot' observable (http://www.introtorx.com/content/v1.0.10621.0/14_HotAndColdObservables.html)
    // i.e. however many subscriptions, we only make one set of API calls
  }
}

trait ContinuousIntegrationAPI {
  def jobs(implicit ec: ExecutionContext): Observable[Job]
  def builds(job: Job)(implicit ec: ExecutionContext): Observable[CIBuild]
  def buildBatch(job: Job)(implicit ec: ExecutionContext): Observable[Iterable[CIBuild]]
}

object TeamCityAPI extends ContinuousIntegrationAPI {
  def jobs(implicit ec: ExecutionContext): Observable[Job] =
    Observable.from(BuildTypeLocator.list).flatMap(Observable.from(_))

  def builds(job: Job)(implicit ec: ExecutionContext): Observable[CIBuild] = for {
    builds <- TeamCityAPI.buildBatch(job)
    build <- Observable.from(builds)
  } yield build

  def buildBatch(job: Job)(implicit ec: ExecutionContext): Observable[Iterable[CIBuild]] = {
    Observable.from {
      val startTime = DateTime.now()
      TeamCityWS.url(s"/app/rest/builds?locator=buildType:${job.id},branch:default:any&count=20").get().flatMap { r =>
        TeamCityMetrics.ApiCallTimer.recordTimeSpent(DateTime.now.getMillis - startTime.getMillis)
        BuildSummary(r.xml, (id: String) => Future.successful(Some(job)), false)
      }
    }
  }

  def recentBuildJobIds(implicit ec: ExecutionContext): Observable[String] = {
    Observable.from(
      TeamCity.api.build.since(DateTime.now.minusMinutes(Configuration.teamcity.pollingWindowMinutes)).get().map { r =>
        (r.xml \\ "@buildTypeId").map(_.text)
      }
    ) flatMap (Observable.from(_))
  }
}

