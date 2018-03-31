package gitbucket.plugin.controller


import gitbucket.core.controller.ControllerBase
import gitbucket.core.service._
import gitbucket.core.util.Implicits._
import gitbucket.core.util.OwnerAuthenticator
import gitbucket.plugin.html
import gitbucket.plugin.model.JenkinsResultCommentSetting
import gitbucket.plugin.service.{JenkinsResultCommentService, JenkinsResultService}
import org.apache.http.util.EntityUtils
import org.scalatra.forms._

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal



class JenkinsResultCommentController
  extends JenkinsResultCommentControllerBase
    with JenkinsResultCommentService
    with JenkinsResultService
    with RepositoryService
    with AccountService
    with IssuesService
    with OwnerAuthenticator
    with PullRequestService
    with CommitsService
    with WebHookPullRequestService



trait JenkinsResultCommentControllerBase extends ControllerBase {
  self: JenkinsResultCommentService
    with JenkinsResultService
    with RepositoryService
    with CommitsService
    with IssuesService
    with OwnerAuthenticator
    with AccountService
    with WebHookPullRequestService
    with PullRequestService =>


  case class SettingsForm(
    jenkinsUrl              : String,
    jenkinsJobName          : String,
    jenkinsUserId           : String,
    jenkinsUserPass         : String,
    gitbucketCommentUserId  : String,
    gitbucketCommentUserPass: String,
    resultBuildStatus       : Boolean,
    resultTest              : Boolean,
    resultCheckstyle        : Boolean,
    resultFindbugs          : Boolean,
    resultPmd               : Boolean
  )

  val settingsForm = mapping(
    "jenkinsUrl"                    -> trim(label("Jenkins Url"                     , text(required, maxlength(200)))),
    "jenkinsJobName"                -> trim(label("Jenkins Job Name"                , text(required, maxlength(100)))),
    "jenkinsUserId"                 -> trim(label("Jenkins User Id"                 , text(required, maxlength(100)))),
    "jenkinsUserPass"               -> trim(label("Jenkins User Password"           , text(required, maxlength(100)))),
    "gitbucketCommentUserId"        -> trim(label("GitBucket Comment User Id"       , text(required, maxlength(100)))),
    "gitbucketCommentUserPass"      -> trim(label("GitBucket Comment User Password" , text(required, maxlength(100)))),
    "resultBuildStatus"             -> trim(label("Build Status"                    , boolean())),
    "resultTest"                    -> trim(label("Test"                            , boolean())),
    "resultCheckstyle"              -> trim(label("Checkstyle"                      , boolean())),
    "resultFindbugs"                -> trim(label("Findbugs"                        , boolean())),
    "resultPmd"                     -> trim(label("Pmd"                             , boolean()))
  )(SettingsForm.apply)


  get("/:owner/:repository/settings/jenkins-result-comment")(ownerOnly { repository =>

    val settingOptions = getJenkinsResultCommentSetting(repository.owner, repositoryName = repository.name)
    val setting = settingOptions.getOrElse(JenkinsResultCommentSetting(
        userName                  = repository.owner,
        repositoryName            = repository.name,
        jenkinsUrl                = "",
        jenkinsJobName            = "",
        jenkinsUserId             = "",
        jenkinsUserPass           = "",
        gitbucketCommentUserId    = "",
        gitbucketCommentUserPass  = "",
        resultTest                = false,
        resultCheckstyle          = false,
        resultFindbugs            = false,
        resultPmd                 = false
    ))


    html.setting(repository, setting, None)
  })

  ajaxPost("/:owner/:repository/settings/jenkins-result-comment/test")(ownerOnly { repository =>

    val jenkinsUrl = params("jenkinsUrl")
    val jenkinsJobName = params("jenkinsJobName")
    val jenkinsUserId = params("jenkinsUserId")
    val jenkinsUserPass = params("jenkinsUserPass")

    val (url, reqFuture, resFuture) = testJenkinsResult(jenkinsUrl, jenkinsJobName, jenkinsUserId, jenkinsUserPass)


    def _headers(h: Array[org.apache.http.Header]): Array[Array[String]] = h.map { h => Array(h.getName, h.getValue) }

    val toErrorMap: PartialFunction[Throwable, Map[String,String]] = {
      case e: java.net.UnknownHostException => Map("error"-> ("Unknown host " + e.getMessage))
      case e: java.lang.IllegalArgumentException => Map("error"-> ("invalid url"))
      case e: org.apache.http.client.ClientProtocolException => Map("error"-> ("invalid url"))
      case NonFatal(e) => Map("error"-> (e.getClass + " "+ e.getMessage))
    }


    org.json4s.jackson.Serialization.write(Map(
      "url" -> url,
      "request" -> Await.result(reqFuture.map(req => Map(
        "headers" -> _headers(req.getAllHeaders)
      )).recover(toErrorMap), 20.seconds),
      "response" -> Await.result(resFuture.map(res => Map(
        "status"  -> res.getStatusLine,
        "body"    -> EntityUtils.toString(res.getEntity),
        "headers" -> _headers(res.getAllHeaders)
      )).recover(toErrorMap), 20.seconds)
    ))
  })


  post("/:owner/:repository/settings/jenkins-result-comment", settingsForm)(ownerOnly { (form, repository) =>

    registerJenkinsResultCommentSetting(
      repositoryName            = repository.name,
      userName                  = repository.owner,
      jenkinsUrl                = form.jenkinsUrl,
      jenkinsJobName            = form.jenkinsJobName,
      jenkinsUserId             = form.jenkinsUserPass,
      jenkinsUserPass           = form.jenkinsUserId,
      gitbucketCommentUserId    = form.gitbucketCommentUserId,
      gitbucketCommentUserPass  = form.gitbucketCommentUserPass,
      resultTest                = form.resultTest,
      resultFindbugs            = form.resultFindbugs,
      resultCheckstyle          = form.resultCheckstyle,
      resultPmd                 = form.resultPmd
    )

    redirect(s"/${repository.owner}/${repository.name}/settings/jenkins-result-comment")
  })


  post("/:owner/:repository/settings/jenkins-result-comment/delete")(ownerOnly { repository =>

    deleteJenkinsResultCommentSetting(
      repositoryName    = repository.name,
      userName          = repository.owner
    )

    redirect(s"/${repository.owner}/${repository.name}/settings/jenkins-result-comment")
  })
}
