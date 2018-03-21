import gitbucket.core.service._
import gitbucket.plugin.hook.JenkinsHook
import io.github.gitbucket.solidbase.model.Version

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "jenkins-result-comment"
  override val pluginName: String = "Jenkins result comment Plugin"
  override val description: String = "You can check Jnekins result at Pull Request comment line"
  override val versions: List[Version] = List(new Version("0.0.1"))


  override val pullRequestHooks = Seq(
    new JenkinsHook with PullRequestService with IssuesService with AccountService with RepositoryService with CommitsService
  )

}
