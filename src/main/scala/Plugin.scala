import gitbucket.core.controller.Context
import gitbucket.core.plugin.Link
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.service._
import gitbucket.plugin.hook.JenkinsHook
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId = "jenkins-result-comment"
  override val pluginName = "Jenkins result comment Plugin"
  override val description = "You can check Jnekins result at Pull Request comment line"

  override val versions = List(
    new Version("1.0.0", new LiquibaseMigration("update/gitbucket-jenkins-result-comment_1.0.0.xml"))
  )

  override val repositorySettingTabs: Seq[(RepositoryService.RepositoryInfo, Context) => Option[Link]] = Seq(
    (repository: RepositoryInfo, contenxt: Context) => Some(Link(
      id = "jenkins-result-comment",
      label = "Jenkins result comment",
      path = "settings/jenkins-result-comment"))
  )


  override val pullRequestHooks = Seq(
    new JenkinsHook with PullRequestService with IssuesService with AccountService with RepositoryService with CommitsService
  )

}
