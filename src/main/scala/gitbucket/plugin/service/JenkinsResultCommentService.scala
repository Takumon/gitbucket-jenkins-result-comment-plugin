package gitbucket.plugin.service

import gitbucket.plugin.model.JenkinsResultCommentSetting
import gitbucket.plugin.model.Profile._
import gitbucket.plugin.model.Profile.profile.blockingApi._


trait JenkinsResultCommentService {
  def registerJenkinsResultCommentSettings(
        userName: String,
        repositoryName: String,
        jenkinsUrl:         String,
        jenkinsJobName:     String,
        resultBuildStatus:  Boolean,
        resultTest:         Boolean,
        resultCheckstyle:   Boolean,
        resultFindbugs:     Boolean,
        resultPmd:          Boolean
  )(implicit session: Session): Unit =
    JenkinsResultCommentSettings.insert(JenkinsResultCommentSetting(
      userName = userName,
      repositoryName = repositoryName,
      jenkinsUrl = jenkinsUrl,
      jenkinsJobName = jenkinsJobName,
      resultBuildStatus = resultBuildStatus,
      resultTest = resultTest,
      resultCheckstyle = resultCheckstyle,
      resultFindbugs = resultFindbugs,
      resultPmd = resultPmd
    ))
}
