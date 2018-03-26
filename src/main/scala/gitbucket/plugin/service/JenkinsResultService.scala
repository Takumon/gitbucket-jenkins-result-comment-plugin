package gitbucket.plugin.service

import java.nio.charset.StandardCharsets
import java.util.Base64

import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpHeaders, HttpRequest, HttpRequestInterceptor, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}


trait JenkinsResultService {
  // TODO ユーザ名、パスワードの入力受付
  // TODO トークン付与
  val GITIBUCKET_COMMENT_USER_ID = "jenkins"
  val GITBUCKET_COMMENT_USER_PASS = "jenkins"

  def addPullRequestComment(repositoryOwner: String, repositoryName: String, pullRequestNumber: Int, comment: String, baseUrl: String): (Future[HttpResponse]) = {
    val url = s"$baseUrl/api/v3/repos/$repositoryOwner/$repositoryName/issues/$pullRequestNumber/comments";

    println(url)
    val reqPromise = Promise[HttpRequest]

    val f = Future {
      try {
        val interceptor = new HttpRequestInterceptor {
          override def process(req: HttpRequest, ctx: HttpContext): Unit = {
            req.getAllHeaders.foreach(h => println(h.getName + " = " + h.getValue))
            reqPromise.success(req)
          }
        }

        val httpClient = HttpClientBuilder.create.useSystemProperties.addInterceptorLast(interceptor).build
        val httpPost = new HttpPost(url)
        val com = s"""{"body":"$comment"}"""
        val entity = EntityBuilder.create()
          .setContentType(ContentType.APPLICATION_JSON)
          .setText(com)
          .build()
        httpPost.setEntity(entity)
        val encodedAuth = Base64.getEncoder.encodeToString(s"$GITIBUCKET_COMMENT_USER_ID:$GITIBUCKET_COMMENT_USER_ID".getBytes(StandardCharsets.UTF_8))
        httpPost.addHeader(HttpHeaders.AUTHORIZATION, s"Basic $encodedAuth")

        val res = httpClient.execute(httpPost)
        httpPost.releaseConnection()
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
    f
  }


  def getJenkinsResult(url: String): Future[HttpResponse] = {


    val reqPromise = Promise[HttpRequest]
    val f = Future {
      val interceptor = new HttpRequestInterceptor {
        override def process(req: HttpRequest, ctx: HttpContext): Unit = {
          reqPromise.success(req)
        }
      }
      try {
        // TODO Basic認証
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
    f
  }
}
