package gitbucket.plugin.hook

import gitbucket.core.controller.Context
import gitbucket.core.model.{Issue, Profile}
import gitbucket.core.plugin.PullRequestHook
import gitbucket.core.service._

trait JenkinsHook extends PullRequestHook {
  self: PullRequestService with PullRequestService with IssuesService with AccountService with RepositoryService with CommitsService =>

  override def addedComment(commentId: Int, content: String, issue: Issue, repository: RepositoryService.RepositoryInfo)(implicit session: Profile.profile.api.Session, context: Context): Unit = {
    println(content)

    if (content != "Jenkins?") {
      println("プラグイン処理対象外")
      return
    }
    println("プラグイン処理対象")


    createComment(repository.owner, repository.name, "jenkins", issue.issueId, "お試しプラグインからのコメント", "")
  }
}
