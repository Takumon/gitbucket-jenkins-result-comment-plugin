# gitbucket-jenkins-result-comment-plugin [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Build Status](https://travis-ci.org/Takumon/gitbucket-jenkins-result-comment-plugin.svg?branch=master)](https://travis-ci.org/Takumon/gitbucket-jenkins-result-comment-plugin)

With this plugin, <br>
You can check the build result details of Jenkins in the comment field of pull request <br>
by commenting "Jenkins?".

![Screenshot](images/comment-detail.png)

<br>
You can do setting of the plugin by repository

![Screenshot](images/setting-detail.png)


## Installation

* Download jar file from [the release page](https://github.com/Takumon/gitbucket-jenkins-result-comment-plugin/releases)
* copy the jar file to `<GITBUCKET_HOME>/plugins/`  (`GITBUCKET_HOME` defaults to `~/.gitbucket`)

## Version

Plugin version|GitBucket version
:---|:---
0.1|4.20.x

## Build from source

1. Install sbt
1. `$ sbt assembly`
