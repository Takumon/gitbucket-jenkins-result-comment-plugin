package gitbucket.plugin.controller


import gitbucket.core.util.Implicits._
import scala.language.implicitConversions


import gitbucket.core.controller.ControllerBase
import gitbucket.core.service._
import gitbucket.core.util.{OwnerAuthenticator, ReadableUsersAuthenticator, ReferrerAuthenticator, WritableUsersAuthenticator}
import gitbucket.plugin.html
import gitbucket.plugin.service.JenkinsResultCommentService
import org.scalatra.forms._
import gitbucket.plugin.model.JenkinsResultCommentSetting



class JenkinsResultCommentController
  extends JenkinsResultCommentControllerBase
    with RepositoryService
    with AccountService
    with ActivityService
    with IssuesService
    with WebHookService
    with CommitsService
    with OwnerAuthenticator
    with LabelsService
    with MilestonesService
    with PrioritiesService
    with ReadableUsersAuthenticator
    with ReferrerAuthenticator
    with WritableUsersAuthenticator
    with PullRequestService
    with CommitStatusService
    with WebHookPullRequestService
    with WebHookPullRequestReviewCommentService
    with ProtectedBranchService
    with JenkinsResultCommentService



trait JenkinsResultCommentControllerBase extends ControllerBase {
  self: RepositoryService
    with AccountService
    with ActivityService
    with IssuesService
    with WebHookService
    with CommitsService
    with OwnerAuthenticator
    with ReadableUsersAuthenticator
    with ReferrerAuthenticator
    with WritableUsersAuthenticator
    with PullRequestService
    with CommitStatusService
    with WebHookPullRequestService
    with WebHookPullRequestReviewCommentService
    with ProtectedBranchService
    with JenkinsResultCommentService =>


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
    "jenkinsJobName"                -> trim(label("Jenkins Job Name"                , text(required, maxlength(200)))),
    "jenkinsUserId"                 -> trim(label("Jenkins User Id"                 , text(required, maxlength(200)))),
    "jenkinsUserPass"               -> trim(label("Jenkins User Password"           , text(required, maxlength(200)))),
    "gitbucketCommentUserId"        -> trim(label("GitBucket Comment User Id"       , text(required, maxlength(200)))),
    "gitbucketCommentUserPass"      -> trim(label("GitBucket Comment User Password" , text(required, maxlength(200)))),
    "resultBuildStatus"             -> trim(label("Build Status"                    , boolean())),
    "resultTest"                    -> trim(label("Test"                            , boolean())),
    "resultCheckstyle"              -> trim(label("Checkstyle"                      , boolean())),
    "resultFindbugs"                -> trim(label("Findbugs"                        , boolean())),
    "resultPmd"                     -> trim(label("Pmd"                             , boolean()))
  )(SettingsForm.apply)


  get("/:owner/:repository/settings/jenkins-result-comment")(ownerOnly { repository =>
    println("初期表示")
    println("リポジトリ : " + repository)

    val settingOptions = getJenkinsResultCommentSetting(repository.owner, repositoryName = repository.name)
    val setting = settingOptions.getOrElse(new JenkinsResultCommentSetting(
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

  post("/:owner/:repository/settings/jenkins-result-comment/upsert", settingsForm)(ownerOnly { (form, repository) =>
    println("登録処理")
    println("フォーム : " + form)
    println("リポジトリ : " + repository)

    registerJenkinsResultCommentSetting(
      repositoryName            = repository.name,
      userName                  = repository.owner,
      jenkinsUrl                = form.jenkinsUrl,
      jenkinsJobName            = form.jenkinsJobName,
      jenkinsUserId             = form.jenkinsUserPass,
      jenkinsUserPass           = form.jenkinsUserId,
      gitbucketCommentUserId    = form.gitbucketCommentUserId,
      gitbucketCommentUserPass  = form.gitbucketCommentUserId,
      resultTest                = form.resultTest,
      resultFindbugs            = form.resultFindbugs,
      resultCheckstyle          = form.resultCheckstyle,
      resultPmd                 = form.resultPmd
    )

    redirect(s"/${repository.owner}/${repository.name}/settings/jenkins-result-comment")
  })

  post("/:owner/:repository/settings/jenkins-result-comment/delete")(ownerOnly { repository =>
    println("削除処理")
    println("リポジトリ : " + repository)

    deleteJenkinsResultCommentSetting(
      repositoryName    = repository.name,
      userName          = repository.owner
    )

    redirect(s"/${repository.owner}/${repository.name}/settings/jenkins-result-comment")
  })
}
