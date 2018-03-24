package gitbucket.plugin.controller


import gitbucket.core.util.Implicits._
import scala.language.implicitConversions


import gitbucket.core.controller.ControllerBase
import gitbucket.core.service._
import gitbucket.core.util.{OwnerAuthenticator, ReadableUsersAuthenticator, ReferrerAuthenticator, WritableUsersAuthenticator}
import gitbucket.plugin.html
import gitbucket.plugin.service.JenkinsResultCommentService
import org.scalatra.forms._



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
    jenkinsUrl:         String,
    jenkinsJobName:     String,
    resultBuildStatus:  Boolean,
    resultTest:         Boolean,
    resultCheckstyle:   Boolean,
    resultFindbugs:     Boolean,
    resultPmd:          Boolean
  )

  val settingsForm = mapping(
    "jenkinsUrl"          -> trim(label("Jenkins Url"       , text(required, maxlength(200)))),
    "jenkinsJobName"      -> trim(label("Jenkins Job Name"  , text(required, maxlength(200)))),
    "resultBuildStatus"   -> trim(label("Build Status"      , boolean())),
    "resultTest"          -> trim(label("Test"              , boolean())),
    "resultCheckstyle"    -> trim(label("Checkstyle"        , boolean())),
    "resultFindbugs"      -> trim(label("Findbugs"          , boolean())),
    "resultPmd"           -> trim(label("Pmd"               , boolean()))
  )(SettingsForm.apply)

  post("/:owner/:repository/settings/jenkins-result-comment", settingsForm)(ownerOnly { (form, repository) =>
    println("get処理")
    println("フォーム : " + form)
    println("リポジトリ : " + repository)



    registerJenkinsResultCommentSettings(
      repositoryName = repository.name,
      userName = repository.owner,
      jenkinsUrl = form.jenkinsUrl,
      jenkinsJobName = form.jenkinsJobName,
      resultBuildStatus = form.resultBuildStatus,
      resultTest = form.resultTest,
      resultFindbugs = form.resultFindbugs,
      resultCheckstyle = form.resultCheckstyle,
      resultPmd = form.resultPmd
    )

    html.setting(repository, None)
  })

  get("/:owner/:repository/settings/jenkins-result-comment")(ownerOnly { repository =>
    println("get処理")
    println("リポジトリ : " + repository)
    html.setting(repository, None)
  })

}
