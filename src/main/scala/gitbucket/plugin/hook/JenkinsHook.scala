package gitbucket.plugin.hook

import gitbucket.core.controller.Context
import gitbucket.core.model.Issue
import gitbucket.core.plugin.PullRequestHook
import gitbucket.core.service._
import gitbucket.core.model.Profile._
import gitbucket.plugin.service.JenkinsResultCommentService
import profile.api._
import gitbucket.plugin.service.JenkinsResultService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class JenkinsHook
  extends PullRequestHook
    with PullRequestService
    with IssuesService
    with AccountService
    with RepositoryService
    with CommitsService
    with JenkinsResultCommentService
    with JenkinsResultService {

  override def addedComment(commentId: Int, content: String, issue: Issue, repository: RepositoryService.RepositoryInfo)(implicit session: Session, context: Context): Unit = {
    if (content != "Jenkins?") {
      return
    }

    val settingOptions = getJenkinsResultCommentSetting(repository.owner, repositoryName = repository.name)
    settingOptions match {
      case Some(setting) => {
        getPullRequest(repository.owner, repository.name, issue.issueId) map { case(issue, pullreq) =>

          getJenkinsResultComment(pullreq.requestBranch, setting).onComplete {
            case Success(comment) => {
              addPullRequestComment(setting, issue.issueId, comment, context.baseUrl).onComplete {
                case Success(body) => println("コメントに成功しました")
                case Failure(t) => println(t.getMessage())
              }
            }
            case Failure(t) => println(t.getMessage())
          }
        }
      }
      case None => println("Jenkins Result Commentの設定されていないため処理終了")
    }
  }
}
