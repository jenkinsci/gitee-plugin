# Table of Contents
- [Introduction](#introduction)
- [User support](#user-support)
- [Known bugs/issues](#known-bugsissues)
- [Global plugin configuration](#global-plugin-configuration)
  - [GitLab-to-Jenkins auth](#gitee-to-jenkins-authentication)
  - [Jenkins-to-GitLab auth](#jenkins-to-gitee-authentication)
 - [Jenkins Job Configuration](#jenkins-job-configuration)
   - [Parameter configuration](#parameter-configuration)
   - [Git configuration](#git-configuration)
     - [Freestyle jobs](#freestyle-jobs)
     - [Pipeline jobs](#pipeline-jobs)
     - [Pipeline Multibranch jobs](#pipeline-multibranch-jobs)
   - [Job trigger configuration](#job-trigger-configuration)
     - [Webhook URL](#webhook-url)
     - [Freestyle and Pipeline jobs](#freestyle-and-pipeline-jobs)
     - [Pipeline Multibranch jobs](#pipeline-multibranch-jobs-1)
     - [Multibranch Pipeline jobs with Job DSL](#multibranch-pipeline-jobs-with-job-dsl)
   - [Build status configuration](#build-status-configuration)
     - [Freestyle jobs](#freestyle-jobs-1)
     - [Scripted Pipeline jobs](#scripted-pipeline-jobs)
     - [Declarative Pipeline jobs](#declarative-pipeline-jobs)
     - [Matrix jobs](#matrixmulti-configuration-jobs)
- [Advanced features](#advanced-features)
  - [Branch filtering](#branch-filtering)
  - [Build when tags are pushed](#build-when-tags-are-pushed)
  - [Add a note to merge requests](#add-a-note-to-merge-requests)
- [Contributing to the Plugin](#contributing-to-the-plugin)
- [Release Workflow](#release-workflow)

# Introduction

This plugin allows Gitee to trigger builds in Jenkins when code is committed or merge requests are opened/updated. It can also send build status back to Gitee.

### Seeking maintainers

This plugin was developed base on [GitLab Plugin](https://github.com/jenkinsci/gitee-plugin) by [Gitee.com](https://gitee.com). [Gitee.com](https://gitee.com) will continue to maintain this plugin.  

If you are a regular user and would like to help out, please consider volunteering as a maintainer. There are verified bugs that need fixes, open PRs that need review, and feature requests that range from simple to complex. If you are interested in contributing, contact Owen (email address in git log) for additional access.

# User support

If you have a problem or question about using the plugin, please make sure you are using the latest version. Then create an issue in the GitHub project if necessary. New issues should include the following:
* GitLab plugin version (e.g. 1.5.2)
* GitLab version (e.g. 10.5.1)
* Jenkins version (e.g. 2.111)
* Relevant log output from the plugin (see below for instructions on capturing this)

Version 1.2.0 of the plugin introduced improved logging for debugging purposes. To enable it:

1. Go to Jenkins -> Manage Jenkins -> System Log
2. Add new log recorder
3. Enter 'Gitlab plugin' or whatever you want for the name
4. On the next page, enter 'com.gitee.jenkins' for Logger, set log level to FINEST, and save
5. Then click on your Gitlab plugin log, click 'Clear this log' if necessary, and then use GitLab to trigger some actions
6. Refresh the log page and you should see output

You can also try chatting with us in the #gitee-plugin channel on the Freenode IRC network.

# Known bugs/issues

This is not an exhaustive list of issues, but rather a place for us to note significant bugs that may impact your use of the plugin in certain circumstances. For most things, please search the [Issues](https://github.com/jenkinsci/gitee-plugin/issues) section and open a new one if you don't find anything.
* [#272](https://github.com/jenkinsci/gitee-plugin/issues/272) - Plugin version 1.2.0+ does not work with GitLab Enterprise Edition < 8.8.3. Subsequent versions work fine.
* Jenkins versions 1.651.2 and 2.3 removed the ability of plugins to set arbitrary job parameters that are not specifically defined in each job's configuration. This was an important security update, but it has broken compatibility with some plugins, including ours. See [here](https://jenkins.io/blog/2016/05/11/security-update/) for more information and workarounds if you are finding parameters unset or empty that you expect to have values.
* [#473](https://github.com/jenkinsci/gitee-plugin/issues/473) - When upgrading from plugin versions older than 1.2.0, you must upgrade to that version first, and then to the latest version. Otherwise, you will get a NullPointerException in com.cloudbees.plugins.credentials.matchers.IdMatcher after you upgrade. See the linked issue for specific instructions.
* [#608](https://github.com/jenkinsci/gitee-plugin/issues/608) - GitLab 9.5.0 - 9.5.4 has a bug that causes the "Test Webhook" function to fail when it sends a test to Jenkins. This was fixed in 9.5.5.
* [#730](https://github.com/jenkinsci/gitee-plugin/issues/730) - GitLab 10.5.6 introduced an issue which can cause HTTP 500 errors when webhooks are triggered. See the linked issue for a workaround.



# Global plugin configuration
## GitLab-to-Jenkins authentication
By default the plugin will require authentication to be set up for the connection from GitLab to Jenkins, in order to prevent unauthorized persons from being able to trigger jobs. 

### Configuring global authentication
1. Create a user in Jenkins which has, at a minimum, Job/Build permissions
2. Log in as that user (this is required even if you are a Jenkins admin user), then click on the user's name in the top right corner of the page
3. Click 'Configure,' then 'Show API Token...', and note/copy the User ID and API Token
4. In GitLab, when you create webhooks to trigger Jenkins jobs, use this format for the URL and do not enter anything for 'Secret Token': `http://USERID:APITOKEN@JENKINS_URL/project/YOUR_JOB`
5. After you add the webhook, click the 'Test' button, and it should succeed

### Configuring per-project authentication

If you want to create separate authentication credentials for each Jenkins job:
1. In the configuration of your Jenkins job, in the GitLab configuration section, click 'Advanced'
2. Click the 'Generate' button under the 'Secret Token' field
3. Copy the resulting token, and save the job configuration
4. In GitLab, create a webhook for your project, enter the trigger URL (e.g. `http://JENKINS_URL/project/YOUR_JOB`) and paste the token in the Secret Token field
5. After you add the webhook, click the 'Test' button, and it should succeed

### Disabling authentication

If you want to disable this authentication (not recommended):
1. In Jenkins, go to Manage Jenkins -> Configure System
2. Scroll down to the section labeled 'GitLab'
3. Uncheck "Enable authentication for '/project' end-point" - you will now be able to trigger Jenkins jobs from GitLab without needing authentication

## Jenkins-to-GitLab authentication
**PLEASE NOTE:** This auth configuration is only used for accessing the GitLab API for sending build status to GitLab. It is **not** used for cloning git repos. The credentials for cloning (usually SSH credentials) should be configured separately, in the git plugin.

This plugin can be configured to send build status messages to GitLab, which show up in the GitLab Merge Request UI. To enable this functionality: 
1. Create a new user in GitLab
2. Give this user 'Developer' permissions on each repo you want Jenkins to send build status to
3. Log in or 'Impersonate' that user in GitLab, click the user's icon/avatar and choose Settings
4. Click on 'Access Tokens'
5. Create a token named e.g. 'jenkins' with 'api' scope; expiration is optional
6. Copy the token immediately, it cannot be accessed after you leave this page
7. On the Global Configuration page in Jenkins, in the GitLab configuration section, supply the GitLab host URL, e.g. `http://your.gitee.server` 
8. Click the 'Add' button to add a credential, choose 'GitLab API token' as the kind of credential, and paste your GitLab user's API key into the 'API token' field
9. Click the 'Test Connection' button; it should succeed

# Jenkins Job Configuration

There are two aspects of your Jenkins job that you may want to modify when using GitLab to trigger jobs. The first is the Git configuration, where Jenkins clones your git repo. The GitLab Plugin will set some environment variables when GitLab triggers a build, and you can use those to control what code is cloned from Git. The second is the configuration for sending the build status back to GitLab, where it will be visible in the commit and/or merge request UI.


You will need to update this code anytime you add or remove parameters.

## Git configuration 
### Freestyle jobs
In the *Source Code Management* section:

1. Click *Git*
2. Enter your *Repository URL*, such as ``git@your.gitee.server:gitee_group/gitee_project.git``
    1. In the *Advanced* settings, set *Name* to ``origin`` and *Refspec* to ``+refs/heads/*:refs/remotes/origin/* +refs/pull/*/MERGE:refs/pull/*/MERGE``
3. In *Branch Specifier* enter:
    1. For single-repository workflows: ``origin/${giteeSourceBranch}``
    2. For forked repository workflows: ``merge-requests/${giteeMergeRequestIid}``
4. In *Additional Behaviours*:
    1. Click the *Add* drop-down button
    2. Select *Merge before build* from the drop-down
    3. Set *Name of repository* to ``origin``
    4. Set *Branch to merge* as ``${giteeTargetBranch}``




## Job trigger configuration
### Webhook URL
When you configure the plugin to trigger your Jenkins job, by following the instructions below depending on job type, it will listen on a dedicated URL for JSON POSTs from GitLab's webhooks. That URL always takes the form ``http://JENKINS_URL/project/PROJECT_NAME``, or ``http://JENKINS_URL/project/FOLDER/PROJECT_NAME`` if the project is inside a folder in Jenkins. **You should not be using** ``http://JENKINS_URL/job/PROJECT_NAME/build`` or ``http://JENKINS_URL/job/gitee-plugin/buildWithParameters``, as this will bypass the plugin completely.

### Freestyle and Pipeline jobs
1. In the *Build Triggers* section:
    * Select *Build when a change is pushed to GitLab*
    * Copy the *GitLab webhook URL* shown in the UI (see [here](#webhook-url) for guidance)
    * Use the check boxes to trigger builds on *Push Events* and/or *Created Merge Request Events* and/or *Accepted Merge Request Events* and/or *Closed Merge Request Events*
    * Optionally use *Rebuild open Merge Requests* to enable re-building open merge requests after a push to the source branch
    * If you selected *Rebuild open Merge Requests* other than *None*, check *Comments*, and specify the *Comment for triggering a build*.  A new build will be triggered when this phrase appears in a commit comment.  In addition to a literal phrase, you can also specify a Java regular expression
    * You can use *Build on successful pipeline events* to trigger on a successful pipeline run in Gitlab. Note that this build trigger will only trigger a build if the commit is not already built and does not set the Gitlab status. Otherwise you might end up in a loop
2. Configure any other pre build, build or post build actions as necessary
3. Click *Save* to preserve your changes in Jenkins
4. Create a webhook in the relevant GitLab projects (consult the GitLab documentation for instructions on this), and use the URL you copied from the Jenkins job configuration UI. It should look something like `http://JENKINS_URL/project/yourbuildname`



## Build status configuration
You can optionally have your Jenkins jobs send their build status back to GitLab, where it will be displayed in the commit or merge request UI as appropriate. 

### Freestyle jobs
Freestyle jobs can only send build status after the build steps are complete. To do this, choose 'Publish build status to GitLab' from the available 'Post-build actions' in your Jenkins job config. Also make sure you have chosen the appropriate GitLab instance from the 'GitLab connection' dropdown menu, if you have more than one.


### Matrix/Multi-configuration jobs

This plugin can be used with Matrix/Multi-configuration jobs together with the [Flexible Publish](https://plugins.jenkins.io/flexible-publish) plugin which allows you to run publishers after all axis jobs are done. Configure the *Post-build Actions* as follows:

1. Add a *Flexible publish* action
2. In the *Flexible publish* section:
      1. *Add conditional action*
      2. In the *Conditional action* section:
          1. Set *Run?* to *Never*
          2. Select *Condition for Matrix Aggregation*
          3. Set *Run on Parent?* to *Always*
          4. Add GitLab actions as required

# Advanced features
## Branch filtering
Triggers may be filtered based on the branch name, i.e. the build will only be allowed for selected branches. On the project configuration page, when you configure the GitLab trigger, you can choose 'Filter branches by name' or 'Filter branches by regex.' Filter by name takes comma-separated lists of branch names to include and/or exclude from triggering a build. Filter by regex takes a Java regular expression to include and/or exclude.

**Note:** This functionality requires access to GitLab and a git repository url already saved in the project configuration. In other words, when creating a new project, the configuration needs to be saved *once* before being able to add branch filters. For Pipeline jobs, the configuration must be saved *and* the job must be run once before the list is populated.

## Build when tags are pushed
In order to build when a new tag is pushed:
1. In the GitLab webhook configuration, add 'Tag push events'
2. In the job configuration under 'Source code management':
    1. Select 'Advanced...' and add '`+refs/tags/*:refs/remotes/origin/tags/*`' as the Refspec
    2. You can also use 'Branch Specifier' to specify which tag need to be built (example 'refs/tags/${TAGNAME}')

## Add a note to merge requests
To add a note to GitLab merge requests after the build completes, select 'Add note with build status on GitLab merge requests' from the optional Post-build actions. Optionally, click the 'Advanced' button to customize the content of the note depending on the build result.

# Contributing to the Plugin

Plugin source code is hosted on [Github](https://github.com/jenkinsci/gitee-plugin).
New feature proposals and bug fix proposals should be submitted as
[Github pull requests](https://help.github.com/articles/creating-a-pull-request).
Fork the repository on Github, prepare your change on your forked
copy, and submit a pull request (see [here](https://github.com/jenkinsci/gitee-plugin/pulls) for open pull requests). Your pull request will be evaluated by the [Cloudbees Jenkins job](https://jenkins.ci.cloudbees.com/job/plugins/job/gitee-plugin/).

If you are adding new features please make sure that they support the Jenkins Workflow Plugin.
See [here](https://github.com/jenkinsci/workflow-plugin/blob/master/COMPATIBILITY.md) for some information.

Before submitting your change make sure that:
* your changes work with the oldest and latest supported GitLab version
* new features are provided with tests
* refactored code is provided with regression tests
* the code formatting follows the plugin standard
* imports are organised
* you updated the help docs
* you updated the README
* you have used findbugs to see if you haven't introduced any new warnings


# Release Workflow

To perform a plugin hpi file, maintainers can run ``mvn package`` To release a snapshot, e.g. with a bug fix for users to test, just run ``mvn hpi:run``
