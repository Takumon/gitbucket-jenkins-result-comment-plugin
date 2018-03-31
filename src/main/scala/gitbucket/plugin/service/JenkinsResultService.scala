package gitbucket.plugin.service

import java.nio.charset.StandardCharsets
import java.util.Base64

import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import spray.json._
import spray.json.DefaultJsonProtocol._
import gitbucket.plugin.model.JenkinsResultCommentSetting

trait JenkinsResultService {

  def addPullRequestComment(setting: JenkinsResultCommentSetting, pullRequestNumber: Int, comment: String, baseUrl: String): (Future[HttpResponse]) =
    Future {
      val url = s"$baseUrl/api/v3/repos/${setting.userName}/${setting.repositoryName}/issues/$pullRequestNumber/comments"
      val httpClient = HttpClientBuilder.create.useSystemProperties.build
      val httpPost = new HttpPost(url)
      val com = s"""{"body":"$comment"}"""
      val entity = EntityBuilder.create
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
    val jenkinsLatestResultBaseUrl = s"${setting.jenkinsUrl}/job/${setting.jenkinsJobName}/job/$branchName/lastBuild"


    getJenkinsBuildStatus(jenkinsLatestResultBaseUrl, setting).flatMap {
      case (lastBuildNumber, buildStatusComment) => {

        val jenkinsResultBaseUrl = s"${setting.jenkinsUrl}/job/${setting.jenkinsJobName}/job/$branchName/$lastBuildNumber"

        var futures = List[Future[String]]()


        if (setting.resultTest) {
          futures :+= getJenkinsTestResult(jenkinsResultBaseUrl, setting)
        }

        if (setting.resultCheckstyle || setting.resultFindbugs || setting.resultPmd) {
          futures :+= Future {
            Array(
              s"""""",
              s"""**静的コード解析**""",
              s"""""",
              s"""||全件|高|中|低|""",
              s"""|:-|-:|-:|-:|-:|"""
            ).mkString("\\n")
          }

          if (setting.resultCheckstyle) {
            futures :+= getJenkinsCheckstyleResult(jenkinsResultBaseUrl, setting)
          }

          if (setting.resultFindbugs) {
            futures :+= getJenkinsFindBugsResult(jenkinsResultBaseUrl, setting)
          }

          if (setting.resultPmd) {
            futures :+= getJenkinsPMDResult(jenkinsResultBaseUrl, setting)
          }
        }


        val jenkinsResults = Future.sequence(futures)
        jenkinsResults.map {
          buildStatusComment + "\\n" + _.mkString("\\n")
        }
      }
    }
  }

  private def getJenkinsBuildStatus(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[(Int, String)] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val status = jsObject.fields("result").convertTo[String]
      // このURLは XXX/lastBuildではなく　XXX/12 のようにビルド番号が指定されている
      val url = jsObject.fields("url").convertTo[String]
      val lastBuildNumber = jsObject.fields("number").convertTo[Int]


      val comment = Array(
        s"""#### Jenkins最新ビルド結果""",
        s"""**[ステータス]($url)**""",
        s"""`$status`"""
      ).mkString("\\n")

      (lastBuildNumber,comment)
    }

  private def getJenkinsTestResult(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/testReport/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val fail = jsObject.fields("failCount").convertTo[Int]
      val pass = jsObject.fields("passCount").convertTo[Int]
      val skip = jsObject.fields("skipCount").convertTo[Int]

      Array(
        s"""""",
        s"""**[テスト]($jenkinsResultBaseUrl/testReport)**""",
        s"""""",
        s"""|全件|失敗|成功|スキップ|""",
        s"""|-:|-:|-:|-:|""",
        s"""|${fail + pass + skip}|$fail|$pass|$skip|"""
      ).mkString("\\n")
    }

  private def getJenkinsCheckstyleResult(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/checkstyleResult/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val highCount = jsObject.fields("numberOfHighPriorityWarnings").convertTo[Int]
      val normalCount = jsObject.fields("numberOfNormalPriorityWarnings").convertTo[Int]
      val lowCount = jsObject.fields("numberOfLowPriorityWarnings").convertTo[Int]

      val url = s"$jenkinsResultBaseUrl/checkstyleResult"
      val high    = if (highCount > 0)    s"""[$highCount]($url/HIGH)"""      else s"$highCount"
      val normal  = if (normalCount > 0)  s"""[$normalCount]($url/NORMAL)"""  else s"$normalCount"
      val low     = if (lowCount > 0)     s"""[$lowCount]($url/LOW)"""        else s"$lowCount"

      s"""|[Checkstyle]($url)|${highCount + normalCount + lowCount}|$high|$normal|$low|"""
    }

  private def getJenkinsFindBugsResult(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/findbugsResult/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val highCount = jsObject.fields("numberOfHighPriorityWarnings").convertTo[Int]
      val normalCount = jsObject.fields("numberOfNormalPriorityWarnings").convertTo[Int]
      val lowCount = jsObject.fields("numberOfLowPriorityWarnings").convertTo[Int]

      val url = s"$jenkinsResultBaseUrl/findbugsResult"
      val high    = if (highCount > 0)    s"""[$highCount]($url/HIGH)"""      else s"$highCount"
      val normal  = if (normalCount > 0)  s"""[$normalCount]($url/NORMAL)"""  else s"$normalCount"
      val low     = if (lowCount > 0)     s"""[$lowCount]($url/LOW)"""        else s"$lowCount"

      s"""|[FindBugs]($url)|${highCount + normalCount + lowCount}|$high|$normal|$low|"""
    }

  private def getJenkinsPMDResult(jenkinsResultBaseUrl: String, setting: JenkinsResultCommentSetting): Future[String] =
    Future {
      val buildStatusUrl = s"$jenkinsResultBaseUrl/pmdResult/api/json"
      val content = getJenkinsResult(buildStatusUrl, setting)
      val jsObject = content.parseJson.asJsObject

      val highCount = jsObject.fields("numberOfHighPriorityWarnings").convertTo[Int]
      val normalCount = jsObject.fields("numberOfNormalPriorityWarnings").convertTo[Int]
      val lowCount= jsObject.fields("numberOfLowPriorityWarnings").convertTo[Int]

      val url = s"$jenkinsResultBaseUrl/pmdResult"
      val high    = if (highCount > 0)    s"""[$highCount]($url/HIGH)"""      else s"$highCount"
      val normal  = if (normalCount > 0)  s"""[$normalCount]($url/NORMAL)"""  else s"$normalCount"
      val low     = if (lowCount > 0)     s"""[$lowCount]($url/LOW)"""        else s"$lowCount"

      s"""|[PMD]($url)|${highCount + normalCount + lowCount}|$high|$normal|$low|"""
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
    is.close()
    content
  }


  def testJenkinsResult(jenkinsUrl: String, jenkinsJobName: String, jenkinsUserId: String, jenkinsUserPass: String): (String, Future[HttpRequest], Future[HttpResponse]) = {

    val url = s"""$jenkinsUrl/job/$jenkinsJobName/api/json"""

    val requestPromise = Promise[HttpRequest]
    val responseFuture = Future {
      val intercepter = new HttpRequestInterceptor {
        override def process(request: HttpRequest, context: protocol.HttpContext): Unit = {
          requestPromise.success(request)
        }
      }

      try {
        val httpClient = HttpClientBuilder.create.useSystemProperties.addInterceptorLast(intercepter).build
        val httpGet = new HttpGet(url)
        val encodedAuth = Base64.getEncoder.encodeToString(s"$jenkinsUserId:$jenkinsUserPass".getBytes(StandardCharsets.UTF_8))
        httpGet.addHeader(HttpHeaders.AUTHORIZATION, s"Basic $encodedAuth")

        val res = httpClient.execute(httpGet)
        httpGet.releaseConnection()
        res
      } catch {
        case e: Throwable => {
          if (!requestPromise.isCompleted) {
            requestPromise.failure(e)
          }
          throw e
        }
      }
    }

    (url, requestPromise.future, responseFuture)
  }







}
