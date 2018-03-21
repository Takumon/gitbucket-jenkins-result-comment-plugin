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


  override val pullRequestHooks = Seq(
    new JenkinsHook with PullRequestService with IssuesService with AccountService with RepositoryService with CommitsService
  )

}
