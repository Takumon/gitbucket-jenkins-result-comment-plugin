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

  def getJenkinsResultComment(branchName: String, setting: JenkinsResultCommentSetting): Future[String] = {
    val jenkinsResultBaseUrl = s"${setting.jenkinsUrl}/job/${setting.jenkinsJobName}/job/${branchName}/lastBuild"

    val statusF = getJenkinsBuildStatus(jenkinsResultBaseUrl, setting)
    val testF = getJenkinsTestResult(jenkinsResultBaseUrl, setting)
    val checkstyleF = getJenkinsCheckstyleResult(jenkinsResultBaseUrl, setting)
    val findBugsF = getJenkinsFindBugsResult(jenkinsResultBaseUrl, setting)
    val pmdF = getJenkinsPMDResult(jenkinsResultBaseUrl, setting)

    for {
      status <- statusF
      test <- testF
      checkstyle <- checkstyleF
      findBugs <- findBugsF
      pmd <- pmdF
    } yield ( Array(
      status,
      test,
      s"""""",
      s"""**静的コード解析**""",
      s"""""",
      s"""||全件|高|中|低|""",
      s"""|:-|-:|-:|-:|-:|""",
      checkstyle,
      findBugs,
      pmd
    ).mkString("\\n"))
  }

  private def getJenkinsBuildStatus(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val status = jsObject.fields("result").convertTo[String]
      val url = jsObject.fields("url").convertTo[String]

      Array(
        s"""#### [Jenkins最新ビルド結果]($url)""",
        s"""**ステータス**""",
        s"""`$status`"""
      ).mkString("\\n")
    }

  private def getJenkinsTestResult(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/testReport/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val fail = jsObject.fields("failCount").convertTo[Int];
      val pass = jsObject.fields("passCount").convertTo[Int];
      val skip = jsObject.fields("skipCount").convertTo[Int];

      Array(
        s"""""",
        s"""**テスト**""",
        s"""""",
        s"""|全件|失敗|成功|スキップ|""",
        s"""|-:|-:|-:|-:|""",
        s"""|${fail + pass + skip}|${fail}|${pass}|${skip}|""",
      ).mkString("\\n")
    }

  private def getJenkinsCheckstyleResult(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/checkstyleResult/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val high = jsObject.fields("numberOfHighPriorityWarnings").convertTo[Int];
      val normal = jsObject.fields("numberOfNormalPriorityWarnings").convertTo[Int];
      val low = jsObject.fields("numberOfLowPriorityWarnings").convertTo[Int];

      s"""|Checkstyle|${high + normal + low}|${high}|${normal}|${low}|"""
    }

  private def getJenkinsFindBugsResult(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/findbugsResult/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val high = jsObject.fields("numberOfHighPriorityWarnings").convertTo[Int];
      val normal = jsObject.fields("numberOfNormalPriorityWarnings").convertTo[Int];
      val low = jsObject.fields("numberOfLowPriorityWarnings").convertTo[Int];

      s"""|FindBugs|${high + normal + low}|${high}|${normal}|${low}|"""
    }

  private def getJenkinsPMDResult(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/pmdResult/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val high = jsObject.fields("numberOfHighPriorityWarnings").convertTo[Int];
      val normal = jsObject.fields("numberOfNormalPriorityWarnings").convertTo[Int];
      val low = jsObject.fields("numberOfLowPriorityWarnings").convertTo[Int];

      s"""|PMD|${high + normal + low}|${high}|${normal}|${low}|"""
    }


  private def getJenkinsResult(url: String, setting: JenkinsResultCommentSetting): String = {
    val httpClient = HttpClientBuilder.create.useSystemProperties.build
    val httpGet = new HttpGet(url)
    val encodedAuth = Base64.getEncoder.encodeToString(s"${setting.jenkinsUserId}:${setting.jenkinsUserPass}".getBytes(StandardCharsets.UTF_8))
    httpGet.addHeader(HttpHeaders.AUTHORIZATION, s"Basic $encodedAuth")

    val res = httpClient.execute(httpGet)
    httpGet.releaseConnection()

    val is = res.getEntity.getContent
    val content = scala.io.Source.fromInputStream(is).getLines.mkString
    is.close
    content
  }

}
