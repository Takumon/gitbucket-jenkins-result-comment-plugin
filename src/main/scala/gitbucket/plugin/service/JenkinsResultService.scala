package gitbucket.plugin.service

import java.nio.charset.StandardCharsets
import java.util.Base64

import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.{HttpHeaders, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}


trait JenkinsResultService {
  // TODO データベースから取得する
  val GITIBUCKET_COMMENT_USER_ID = "jenkins"
  val GITBUCKET_COMMENT_USER_PASS = "jenkins"
  val JENKINS_USER_ID = "admin"
  val JENKINS_USER_PASS = "admin"


  def addPullRequestComment(repositoryOwner: String, repositoryName: String, pullRequestNumber: Int, comment: String, baseUrl: String): (Future[HttpResponse]) = {
    val url = s"$baseUrl/api/v3/repos/$repositoryOwner/$repositoryName/issues/$pullRequestNumber/comments";

    val f = Future {
      val httpClient = HttpClientBuilder.create.useSystemProperties.build
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
    }
    f.onComplete {
      case Success(_) => print(_)
      case Failure(t) => print(t.getMessage)
    }
    f
  }


  def getJenkinsResult(url: String): Future[HttpResponse] = {
    val f = Future {
      val httpClient = HttpClientBuilder.create.useSystemProperties.build
      val httpGet = new HttpGet(url)
      val encodedAuth = Base64.getEncoder.encodeToString(s"$JENKINS_USER_ID:$JENKINS_USER_PASS".getBytes(StandardCharsets.UTF_8))
      httpGet.addHeader(HttpHeaders.AUTHORIZATION, s"Basic $encodedAuth")

      val res = httpClient.execute(httpGet)
      httpGet.releaseConnection()
      res
    }
    f.onComplete {
      case Success(_) => print(_)
      case Failure(t) => print(t.getMessage)
    }
    f
  }
}
