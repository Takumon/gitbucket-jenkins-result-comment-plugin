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
import spray.json._
import DefaultJsonProtocol._
import gitbucket.plugin.model.JenkinsResultCommentSetting

trait JenkinsResultService {

  def addPullRequestComment(setting: JenkinsResultCommentSetting, pullRequestNumber: Int, comment: String, baseUrl: String): (Future[HttpResponse]) =
    Future {
      val url = s"$baseUrl/api/v3/repos/${setting.userName}/${setting.repositoryName}/issues/$pullRequestNumber/comments";
      val httpClient = HttpClientBuilder.create.useSystemProperties.build
      val httpPost = new HttpPost(url)
      val com = s"""{"body":"$comment"}"""
      val entity = EntityBuilder.create()
        .setContentType(ContentType.APPLICATION_JSON)
        .setText(com)
        .build()
      httpPost.setEntity(entity)
      val encodedAuth = Base64.getEncoder.encodeToString(s"${setting.gitbucketCommentUserId}:${setting.gitbucketCommentUserPass}".getBytes(StandardCharsets.UTF_8))
      httpPost.addHeader(HttpHeaders.AUTHORIZATION, s"Basic $encodedAuth")

      val res = httpClient.execute(httpPost)
      httpPost.releaseConnection()
      res
    }


  def getJenkinsBuildStatus(branchName: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val jenkinsUrl = s"${setting.jenkinsUrl}/job/${setting.jenkinsJobName}/job/${branchName}/lastBuild/api/json"

      val httpClient = HttpClientBuilder.create.useSystemProperties.build
      val httpGet = new HttpGet(jenkinsUrl)
      val encodedAuth = Base64.getEncoder.encodeToString(s"${setting.jenkinsUserId}:${setting.jenkinsUserPass}".getBytes(StandardCharsets.UTF_8))
      httpGet.addHeader(HttpHeaders.AUTHORIZATION, s"Basic $encodedAuth")

      val res = httpClient.execute(httpGet)
      httpGet.releaseConnection()


      val is = res.getEntity.getContent
      val content = scala.io.Source.fromInputStream(is).getLines.mkString
      is.close

      val jsObject = content.parseJson.asJsObject
      val status = jsObject.fields("result").convertTo[String]
      val url = jsObject.fields("url").convertTo[String]

      Array(
        s"""#### [Jenkins最新ビルド結果]($url)""",
        s"""**ステータス**""",
        s"""`$status`"""
      ).mkString("\\n")
    }

}
