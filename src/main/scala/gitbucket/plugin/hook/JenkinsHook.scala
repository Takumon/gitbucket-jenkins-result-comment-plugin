package gitbucket.plugin.hook

import gitbucket.core.controller.Context
import gitbucket.core.model.Issue
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
import spray.json._
import DefaultJsonProtocol._

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
      case Success(comment) => {
        println("▼コメント▼")
        println(comment)
        println("▲コメント▲")

        addPullRequestComment(repository.owner, repository.name, issue.issueId, comment, context.baseUrl).onComplete {
          case Success(body) => println(body)
          case Failure(t) => println(t.getMessage())
        }
      }
      case Failure(t) => println(t.getMessage())
    }
  }

  def createCommentContent(setting: JenkinsResultCommentSetting): Future[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    // TODO JenkinsURLはDBから取得する
    // TODO Jenkinsから取得した情報をマークダウン形式にフォーマットする
    val jenkinsUrl = "http://192.168.1.3:10080/jenkins/job/SampleProject/job/issue-1/lastBuild/api/json"
    getJenkinsResult(jenkinsUrl).map(res => {
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
    })
  }
}
