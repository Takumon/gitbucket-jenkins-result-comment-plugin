package gitbucket.plugin.service

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.HttpContext

import scala.concurrent._
import scala.util.{Failure, Success}
import org.apache.http.{HttpRequest, HttpRequestInterceptor, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global


trait JenkinsResultService {


  def getJenkinsResult(url: String): (Future[HttpRequest], Future[HttpResponse]) = {


    val reqPromise = Promise[HttpRequest]
    val f = Future {
      val interceptor = new HttpRequestInterceptor {
        override def process(req: HttpRequest, ctx: HttpContext): Unit = {
          reqPromise.success(req)
        }
      }
      try {
        val httpClient = HttpClientBuilder.create.useSystemProperties.addInterceptorLast(interceptor).build
        val httpGet = new HttpGet(url)

        val res = httpClient.execute(httpGet)
        httpGet.releaseConnection()
        res
      } catch {
        case e: Throwable => {
          if (!reqPromise.isCompleted) {
            reqPromise.failure(e)
          }
          throw e
        }
      }
    }
    f.onComplete {
      case Success(_) => print(_)
      case Failure(t) => print(t.getMessage)
    }
    (reqPromise.future, f)
  }
}
