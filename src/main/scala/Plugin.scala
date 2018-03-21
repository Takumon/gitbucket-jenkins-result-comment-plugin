import javax.servlet.ServletContext

import gitbucket.core.controller.Context
import gitbucket.core.plugin._
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.service.SystemSettingsService.SystemSettings
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
