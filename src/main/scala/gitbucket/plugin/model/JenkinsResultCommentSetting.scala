package gitbucket.plugin.model


trait JenkinsResultCommentSettingComponent { self: gitbucket.core.model.Profile =>
  import profile.api._

  lazy val JenkinsResultCommentSettings = TableQuery[JenkinsResultCommentSettings]

  class JenkinsResultCommentSettings(tag: Tag) extends Table[JenkinsResultCommentSetting](tag, "JENKINS_RESULT_COMMENT_SETTING") {
    val userName                = column[String]  ("USER_NAME")
    val repositoryName          = column[String]  ("REPOSITORY_NAME")
    val jenkinsUrl              = column[String]  ("JENKINS_URL")
    val jenkinsJobName          = column[String]  ("JENKINS_JOB_NAME")
    val jenkinsUserId           = column[String]  ("JENKINS_USER_ID")
    val jenkinsUserPass         = column[String]  ("JENKINS_USER_PASS")
    val gitbucketCommentUserId  = column[String]  ("GITBUCKET_COMMENT_USER_ID")
    val gitbucketCommentUserPass= column[String]  ("GITBUCKET_COMMENT_USER_PASS")
    val resultTest              = column[Boolean] ("RESULT_TEST")
    val resultCheckstyle        = column[Boolean] ("RESULT_CHECKSTYLE")
    val resultFindbugs          = column[Boolean] ("RESULT_FINDBUGS")
    val resultPmd               = column[Boolean] ("RESULT_PMD")
    def * = (
      userName,
      repositoryName,
      jenkinsUrl,
      jenkinsJobName,
      jenkinsUserId,
      jenkinsUserPass,
      gitbucketCommentUserId,
      gitbucketCommentUserPass,
      resultTest,
      resultCheckstyle,
      resultFindbugs,
      resultPmd
    ) <> ((JenkinsResultCommentSetting.apply _).tupled, JenkinsResultCommentSetting.unapply)
  }
}

case class JenkinsResultCommentSetting (
  userName                  : String,
  repositoryName            : String,
  jenkinsUrl                : String,
  jenkinsJobName            : String,
  jenkinsUserId             : String,
  jenkinsUserPass           : String,
  gitbucketCommentUserId    : String,
  gitbucketCommentUserPass  : String,
  resultTest                : Boolean,
  resultCheckstyle          : Boolean,
  resultFindbugs            : Boolean,
  resultPmd                 : Boolean
)
