package gitbucket.plugin.hook

import gitbucket.core.controller.Context
import gitbucket.core.model.{Issue, Profile}
import gitbucket.core.plugin.PullRequestHook
import gitbucket.core.service._
import gitbucket.core.model.Profile._
import gitbucket.plugin.model.JenkinsResultCommentSetting
import gitbucket.plugin.service.JenkinsResultCommentService
import profile.api._
import gitbucket.plugin.service.JenkinsResultService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util. {Success, Failure}


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


    createCommentContent(setting).onComplete {
      case Success(body) => {
        println(body)
        // TODO コメントのユーザ情報を定数か設定に外出し
        createComment(repository.owner, repository.name, "jenkins", issue.issueId, body, "comment")
      }
      case Failure(t) => println(t.getMessage())
    }
  }

  def createCommentContent(setting: JenkinsResultCommentSetting): Future[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    // TODO Jenkinsから情報を取得する
    // TODO マークダウン形式にフォーマットする
    // TODO URLはDBから取得する
    val jenkinsUrl = "http://192.168.1.3:10080/jenkins/job/SampleProject/job/issue-1/lastBuild/api/json"
    getJenkinsResult(jenkinsUrl)._2.map(res => {
      val is = res.getEntity.getContent
      val content = scala.io.Source.fromInputStream(is).getLines.mkString
      is.close
      content
    })
  }
}
