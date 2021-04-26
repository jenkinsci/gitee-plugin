# Table of Contents
- [Introduction](#introduction) <!-- [简介]完成 -->
  - [Current Supported Features](#Current-Supported-Features) <!--[目前支持特性] 完成 -->
  - [Features in plan](#Features-in-plan) <!--[计划中特性] 完成 -->
- [Global plugin installation](#global-plugin-installation) <!--[插件安装] 完成 -->
- [Global plugin configuration](#global-plugin-configuration) <!--[插件配置] 完成 -->
  - [Gitee link configuration](#gitee-link-configuration) <!--[添加码云链接配置] 完成 -->
  - [Build task configuration](#Build-task-configuration) <!--[构建任务配置] 再写 -->
    - [New build task](#New-build-task) <!--[新建构建任务] -->
    - [Task global configuration](#Task-global-configuration) <!--[任务全局配置] -->



 - [Jenkins Job Configuration](#jenkins-job-configuration)
   - [Git configuration](#git-configuration)
     - [Freestyle jobs](#freestyle-jobs) <!-- 源代码管理 -->
   - [Job trigger configuration](#job-trigger-configuration)
     - [Webhook URL](#webhook-url)
     - [Freestyle jobs](#freestyle-and-pipeline-jobs)
   - [Build status configuration](#build-status-configuration)
     - [Freestyle jobs](#freestyle-jobs-1)
- [Advanced features](#advanced-features)
- [Environment variable](#Environment-variable) <!--[环境变量] 完成 -->
- [User support](#user-support) <!--[用户支持] 完成 -->
- [Participate in contribution](#Participate-in-contribution) <!--[参与贡献] 完成 -->
  - [Package or run tests](#Package-or-run-tests) <!--[打包或运行测试] 完成 -->

# Introduction

This plugin allows Gitee to trigger builds in Jenkins when code is committed or pull requests are opened/updated. It can also send build status back to Gitee.

## Current Supported Features
- When pushing code to the Gitee, the configured Webhook triggers the Jenkins task build。
- Comments on a submission record trigger a Jenkins task build for the corresponding version of the submission record
- When submitting a Pull Request to a Gitee project, Jenkins task builds are triggered by the configured Webhook, which supports PR actions: New, Update, Accept, Close, Review Pass, Test Pass.
- Support [ci-skip] instruction filtering or [ci-build] instruction to trigger build.
- Filter Commit versions that have already been built. If the branch is Push, the same branch Push is filtered,and if the branch is PR, the same PR is filtered.
- Filters triggers by branch name。
- Regular expressions filter the branches that can be triggered。
- Set the Webhook authentication password。
- Post-build operations can configure PR triggered build results to comment in the corresponding PR of Gitee.
- Post-build operation configurable PR triggered by the successful build, the corresponding PR can be automatically merged.
- For all PR-related events, if the PR code conflicts cannot be automatically merged, the build will not be triggered; and if the function of commenting to PR is configured, the comment to PR will prompt conflict.
- PR comments can be triggered via WebHook (can be used for PR to trigger a build failure to facilitate re-triggering the build from the code cloud platform comments).
- Support for configuring PR does not require filtering to trigger a build when testing is required. (Can be used to build a deployment test environment without testing).
- Support the same PR to trigger the construction, cancel the incomplete construction in progress, and proceed to the current construction (the same PR construction is not queued, and multiple different PR constructions still need to be queued).

## Features in plan
1. PR review and test pass trigger build (users can trigger deployment, and the feature of automatically merged PR can be used to improve the workflow.) 
2. Check the trigger mode to automatically add WebHook to Gitee.

# Global plugin installation
1. Online installation 
    - Go to Manage Jenkins -> Manage Plugins -> Available
    - Right Filter enter: Gitee
    - Check Gitee in the optional list below (if Gitee does not exist in the list, click Check now to update the plug-in list) 
    - Click Download now and install after restart

![输入图片说明](https://images.gitee.com/uploads/images/2018/0723/112748_b81a1ee3_58426.png "屏幕截图.png")

2. Manual installation
    - From [release](https://gitee.com/oschina/Gitee-Jenkins-Plugin/releases) Enter the latest release in the list and download the corresponding XXX.hpi file 
    - Go to Manage Jenkins -> Manage Plugins -> Advanced
    - In Upload Plugin File, select the XXX.hpi you just downloaded and click Upload
    - Check in the subsequent pages Restart Jenkins when installation is complete and no jobs are running

![输入图片说明](https://images.gitee.com/uploads/images/2018/0723/113303_2a1d0a03_58426.png "屏幕截图.png")








































# Global plugin configuration
## Gitee link configuration 
1. Go on Jenkins -> Manage Jenkins -> Configure System -> Gitee Configuration -> Gitee connections
2. Enter ``Gitee`` or the name you want in ``Connection name`` 
3. Enter the full URL address of Gitee in ``Gitee host URL'': ``https://gitee.com`` (Customers of Gitee privatization enter the deployed domain name) 
4. If you haven't configured the Code Cloud APIV5 private token in ``Credentials'', click ``Add'' -> ``Jenkins `` 
    1. ``Domain`` select ``Global credentials`` 
    2. ``Kind`` select ``Gitee API Token``
    3. ``Scope`` choose the range you need 
    4. ``Gitee API Token`` Enter your code cloud private token to obtain the address: https://gitee.com/profile/personal_access_tokens 
    5. Enter the ID and description you want in ``ID``, ``Descripiton``.
5. ``Credentials`` Select the configured Gitee APIV5 Token 
6. Click ``Advanced``, you can configure whether to ignore SSL errors (depending on whether your Jenkins environment supports it), and set the link test timeout period (depending on your network environment) 

7. Click ``Test Connection`` to test whether the link is successful, if it fails, please check the above 3, 5, 6 steps.

After the configuration is successful, as shown in the figure ：
![码云链接配置](https://images.gitee.com/uploads/images/2018/0716/185651_68707d16_58426.png "屏幕截图.png")

### New build task
Go to Jenkins -> New Item, enter'Gitee Test' as name, select ``Freestyle project`` and save to create a build project.

### Task global configuration 
In the task global configuration, you need to select the code cloud link in the previous step. Go to Configure -> General of a task (such as'Gitee Test'), and select the code cloud link configured earlier in Gitee connection, as shown in the figure: ：
![任务全局配置](https://images.gitee.com/uploads/images/2018/0716/191715_9660237b_58426.png "屏幕截图.png")


## Gitee-to-Jenkins authentication
By default the plugin will require authentication to be set up for the connection from Gitee to Jenkins, in order to prevent unauthorized persons from being able to trigger jobs. 

### Configuring global authentication
1. Create a user in Jenkins which has, at a minimum, Job/Build permissions
2. Log in as that user (this is required even if you are a Jenkins admin user), then click on the user's name in the top right corner of the page
3. Click 'Configure,' then 'Show API Token...', and note/copy the User ID and API Token
4. In Gitee, when you create webhooks to trigger Jenkins jobs, use this format for the URL and do not enter anything for 'Secret Token': `http://USERID:APITOKEN@JENKINS_URL/project/YOUR_JOB`
5. After you add the webhook, click the 'Test' button, and it should succeed

### Configuring per-project authentication

If you want to create separate authentication credentials for each Jenkins job:
1. In the configuration of your Jenkins job, in the Gitee configuration section, click 'Advanced'
2. Click the 'Generate' button under the 'Secret Token' field
3. Copy the resulting token, and save the job configuration
4. In Gitee, create a webhook for your project, enter the trigger URL (e.g. `http://JENKINS_URL/project/YOUR_JOB`) and paste the token in the Secret Token field
5. After you add the webhook, click the 'Test' button, and it should succeed


## Jenkins-to-Gitee authentication
**PLEASE NOTE:** This auth configuration is only used for accessing the Gitee API for sending build status to Gitee. It is **not** used for cloning git repos. The credentials for cloning (usually SSH credentials) should be configured separately, in the git plugin.

This plugin can be configured to send build status messages to Gitee, which show up in the Gitee Pull Request UI. To enable this functionality: 
1. Create a new user in Gitee
2. Give this user 'Developer' permissions on each repo you want Jenkins to send build status to
3. Log in or 'Impersonate' that user in Gitee, click the user's icon/avatar and choose Settings
4. Click on 'Access Tokens'
5. Create a token named e.g. 'jenkins' with 'api' scope; expiration is optional
6. Copy the token immediately, it cannot be accessed after you leave this page
7. On the Global Configuration page in Jenkins, in the Gitee configuration section, supply the Gitee host URL, e.g. `http://your.gitee.server` 
8. Click the 'Add' button to add a credential, choose 'Gitee API token' as the kind of credential, and paste your Gitee user's API key into the 'API token' field
9. Click the 'Test Connection' button; it should succeed

# Jenkins Job Configuration

There are two aspects of your Jenkins job that you may want to modify when using Gitee to trigger jobs. The first is the Git configuration, where Jenkins clones your git repo. The Gitee Jenkins Plugin will set some environment variables when Gitee triggers a build, and you can use those to control what code is cloned from Git. The second is the configuration for sending the build status back to Gitee, where it will be visible in the commit and/or pull request UI.


You will need to update this code anytime you add or remove parameters.

## Git configuration 
### Freestyle jobs
 <!-- 源码管理配置 -->
In the *Source Code Management* section:

1. Click *Git*
2. Enter your *Repository URL*, such as ``git@your.gitee.server:gitee_group/gitee_project.git``
    1. In the *Advanced* settings, set *Name* to ``origin`` and *Refspec* to ``+refs/heads/*:refs/remotes/origin/* +refs/pull/*/MERGE:refs/pull/*/MERGE``
3. In *Branch Specifier* enter:
    1. For single-repository workflows: ``origin/${giteeSourceBranch}``
    2. For forked repository workflows: ``merge-requests/${giteePullRequestIid}``
4. In *Additional Behaviours*:
    1. Click the *Add* drop-down button
    2. Select *Merge before build* from the drop-down
    3. Set *Name of repository* to ``origin``
    4. Set *Branch to merge* as ``${giteeTargetBranch}``

## Job trigger configuration
### Webhook URL
When you configure the plugin to trigger your Jenkins job, by following the instructions below depending on job type, it will listen on a dedicated URL for JSON POSTs from Gitee's webhooks. That URL always takes the form ``http://JENKINS_URL/project/PROJECT_NAME``, or ``http://JENKINS_URL/project/FOLDER/PROJECT_NAME`` if the project is inside a folder in Jenkins. **You should not be using** ``http://JENKINS_URL/job/PROJECT_NAME/build`` or ``http://JENKINS_URL/job/gitee-plugin/buildWithParameters``, as this will bypass the plugin completely.

### Freestyle jobs
1. In the *Build Triggers* section:
    * Select *Build when a change is pushed to Gitee*
    * Copy the *Gitee webhook URL* shown in the UI (see [here](#webhook-url) for guidance)
    * Use the check boxes to trigger builds on *Push Events* and/or *Created Pull Request Events* and/or *Accepted Pull Request Events* and/or *Closed Pull Request Events*
    * Optionally use *Rebuild open Pull Requests* to enable re-building open pull requests after a push to the source branch
    * If you selected *Rebuild open Pull Requests* other than *None*, check *Comments*, and specify the *Comment for triggering a build*.  A new build will be triggered when this phrase appears in a commit comment.  In addition to a literal phrase, you can also specify a Java regular expression
2. Configure any other pre build, build or post build actions as necessary
3. Click *Save* to preserve your changes in Jenkins
4. Create a webhook in the relevant Gitee projects (consult the Gitee documentation for instructions on this), and use the URL you copied from the Jenkins job configuration UI. It should look something like `http://JENKINS_URL/project/yourbuildname`


## Build status configuration
You can optionally have your Jenkins jobs send their build status back to Gitee, where it will be displayed in the commit or pull request UI as appropriate. 

### Freestyle jobs
Freestyle jobs can only send build status after the build steps are complete. To do this, choose 'Publish build status to Gitee' from the available 'Post-build actions' in your Jenkins job config. Also make sure you have chosen the appropriate Gitee instance from the 'Gitee connection' dropdown menu, if you have more than one.


# Environment variable
The currently supported environment variables are shown in the following functions. Different WebHook triggers may cause some variables to be empty. Please install the plug-in for details.  [EnvInject Plugin](https://wiki.jenkins-ci.org/display/JENKINS/EnvInject+Plugin), View in build Environment Variables


```java
    public Map<String, String> getBuildVariables() {
        MapWrapper<String, String> variables = new MapWrapper<>(new HashMap<String, String>());
        variables.put("giteeBranch", branch);
        variables.put("giteeSourceBranch", sourceBranch);
        variables.put("giteeActionType", actionType.name());
        variables.put("giteeUserName", userName);
        variables.put("giteeUserEmail", userEmail);
        variables.put("giteeSourceRepoHomepage", sourceRepoHomepage);
        variables.put("giteeSourceRepoName", sourceRepoName);
        variables.put("giteeSourceNamespace", sourceNamespace);
        variables.put("giteeSourceRepoURL", sourceRepoUrl);
        variables.put("giteeSourceRepoSshUrl", sourceRepoSshUrl);
        variables.put("giteeSourceRepoHttpUrl", sourceRepoHttpUrl);
        variables.put("giteePullRequestTitle", pullRequestTitle);
        variables.put("giteePullRequestDescription", pullRequestDescription);
        variables.put("giteePullRequestId", pullRequestId == null ? "" : pullRequestId.toString());
        variables.put("giteePullRequestIid", pullRequestIid == null ? "" : pullRequestIid.toString());
        variables.put("giteePullRequestTargetProjectId", pullRequestTargetProjectId == null ? "" : pullRequestTargetProjectId.toString());
        variables.put("giteePullRequestLastCommit", lastCommit);
        variables.put("giteePushCreated", created ? "true" : "false");
        variables.put("giteePushDeleted", deleted ? "true" : "false");
        variables.putIfNotNull("giteePullRequestState", pullRequestState);
        variables.putIfNotNull("giteeMergedByUser", mergedByUser);
        variables.putIfNotNull("giteePullRequestAssignee", pullRequestAssignee);
        variables.put("giteeTargetBranch", targetBranch);
        variables.put("giteeTargetRepoName", targetRepoName);
        variables.put("giteeTargetNamespace", targetNamespace);
        variables.put("giteeTargetRepoSshUrl", targetRepoSshUrl);
        variables.put("giteeTargetRepoHttpUrl", targetRepoHttpUrl);
        variables.put("giteeBefore", before);
        variables.put("giteeAfter", after);
        variables.put("giteeBeforeCommitSha", before);
        variables.put("giteeAfterCommitSha", after);
        variables.put("giteeRef", ref);
        variables.put("ref", ref);
        variables.put("beforeSha", beforeSha);
        variables.put("isTag", isTag);
        variables.put("sha", sha);
        variables.put("status", status);
        variables.put("stages", stages);
        variables.put("createdAt", createdAt);
        variables.put("finishedAt", finishedAt);
        variables.put("duration", buildDuration);
        variables.put("jsonBody", jsonBody);
        variables.put("noteBody", noteBody);
        variables.putIfNotNull("giteeTriggerPhrase", triggerPhrase);
        return variables;
    }

```

# User support

If you have a problem or question about using the plugin, please make sure you are using the latest version. Then create an issue in the Gitee project if necessary. New issues should include the following:
* Jenkins version (e.g. 2.111)
* Relevant log output from the plugin (see below for instructions on capturing this)

Gitee Jenkins plugin introduced improved logging for debugging purposes. To enable it:

1. Go to Jenkins -> Manage Jenkins -> System Log
2. Add new log recorder
3. Enter 'Gitee plugin' or whatever you want for the name
4. On the next page, enter 'com.gitee.jenkins' for Logger, set log level to FINEST, and save
5. Then click on your Gitee jenkins plugin log, click 'Clear this log' if necessary, and then use Gitee to trigger some actions
6. Refresh the log page and you should see output


# Participate in contribution

Welcome to submit CI scenario feature suggestions or directly submit PR contribution code 

## Package or run tests

To perform a plugin hpi file, maintainers can run ``mvn package`` To release a snapshot, e.g. with a bug fix for users to test, just run ``mvn hpi:run``