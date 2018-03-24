package gitbucket.plugin.hook

import gitbucket.core.controller.Context
import gitbucket.core.model.{Issue, Profile}
import gitbucket.core.plugin.PullRequestHook
import gitbucket.core.service._
import gitbucket.core.model.Profile._
import gitbucket.plugin.model.JenkinsResultCommentSetting
import gitbucket.plugin.service.JenkinsResultCommentService
import profile.api._

class JenkinsHook
  extends JenkinsHookBase
    with PullRequestService
    with IssuesService
    with AccountService
    with RepositoryService
    with CommitsService
    with JenkinsResultCommentService


trait JenkinsHookBase extends PullRequestHook {
  self: PullRequestService
    with IssuesService
    with AccountService
    with RepositoryService
    with CommitsService
    with JenkinsResultCommentService =>

  override def addedComment(commentId: Int, content: String, issue: Issue, repository: RepositoryService.RepositoryInfo)(implicit session: Session, context: Context): Unit = {
    println(content)

    if (content != "Jenkins?") {
      println("プラグイン処理対象外")
      return
    }
    println("プラグイン処理対象")


    val settingOptions = getJenkinsResultCommentSetting(repository.owner, repositoryName = repository.name)
    val setting = settingOptions.getOrElse(new JenkinsResultCommentSetting(
      userName          = repository.owner,
      repositoryName    = repository.name,
      jenkinsUrl        = "",
      jenkinsJobName    = "",
      resultBuildStatus = false,
      resultTest        = false,
      resultCheckstyle  = false,
      resultFindbugs    = false,
      resultPmd         = false
    ))


    createComment(repository.owner, repository.name, "jenkins", issue.issueId, createCommentContent(setting), "comment")
  }

  def createCommentContent(setting: JenkinsResultCommentSetting): String = {
    // TODO Jenkinsから情報を取得する
    // TODO マークダウン形式にフォーマットする
    setting.jenkinsUrl
  }
}
