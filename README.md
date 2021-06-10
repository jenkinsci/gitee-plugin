# Table of Contents
- [Introduction](#introduction) 
  - [Current Supported Features](#Current-Supported-Features)
  - [Features in plan](#Features-in-plan) 
- [Global plugin installation](#global-plugin-installation)
- [Global plugin configuration](#global-plugin-configuration) 
  - [Gitee link configuration](#gitee-link-configuration) 
  <!-- - [添加码云APIV5私人令牌](#添加码云APIV5私人令牌) -->
  - [Build task configuration](#Build-task-configuration)
    - [New build task](#New-build-task)
    - [Task global configuration](#Task-global-configuration)
    - [Source code management configuration](#Source-code-management-configuration)
    - [Trigger configuration](#Trigger-configuration) <!--[触发器配置] 完成-->
    <!-- - [WebHook密码配置](#WebHook密码配置) -->
    - [Post-build step configuration](#Post-build-step-configuration)
      - [Build results back to Gitee](#Build-results-back-to-Gitee)
      - [Build successfully and automatically merge PR ](#Build-successfully-and-automatically-merge-PR )
    - [New Gitee project WebHook ](#New-Gitee-project-WebHook )
      - [Test push to trigger build](#Test-push-to-trigger-build)
      - [Test PR trigger build](#Test-PR-trigger-build)
  - [Use scripts to configure triggers](#Use-scripts-to-configure-triggers)
- [Environment variable](#Environment-variable) 
- [User support](#user-support) 
- [Participate in contribution](#Participate-in-contribution)
  - [Package or run tests](#Package-or-run-tests)

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

### Source code management configuration
Go to the Configure -> Source Code Management tab of a task (such as'Gitee Test') 

1. Click *Git*
2. Enter your warehouse address, for example ``git@your.gitee.server:gitee_group/gitee_project.git``
    1. Click the *Advanced* button, enter in the *Name* field  ``origin``， *Refspec* Field input  ``+refs/heads/*:refs/remotes/origin/* +refs/pull/*/MERGE:refs/pull/*/MERGE``
，Note that the new version of jenkins no longer accepts multiple refs descriptions that contain * wildcards at the same time. If only the push trigger can write the first half, if only PR triggers, only the second half can be written. See the figure below for details ：![输入图片说明](https://images.gitee.com/uploads/images/2020/0601/220940_0ce95dd0_58426.png "屏幕截图.png")
3. Credentials, please enter the username and password credentials corresponding to the https address of the git warehouse, or the ssh key credentials corresponding to ssh. Note that the Gitee API Token credentials cannot be used for source code management credentials, but only used for the API call credentials of the gitee plugin.
4. *Branch Specifier* options:
    1. For single warehouse workflow input : ``origin/${giteeSourceBranch}``
    2. For PR workflow input : ``pull/${giteePullRequestIid}/MERGE``
5. *Additional Behaviours* options：
    1. For a single warehouse workflow, if you want to merge the default branch (released branch) before the pushed branch is built, you can do the following：
        1. Click the *Add* drop-down box 
        2. Select *Merge before build* 
        3. Set *Name of repository* to ``origin``
        4. Set *Branch to merge to* to ``${ReleaseBranch}`` The default branch (release branch) you want to merge 
    2. For the PR workflow, the code cloud server has pre-merged the original branch and target branch of the PR. You can build it directly. If the target branch is not the default branch (release branch), you can also merge before the appeal build.。

The configuration is shown in the figure: 

![源码管理配置](https://images.gitee.com/uploads/images/2018/0716/191913_ef0995f4_58426.png "屏幕截图.png")

### Trigger configuration

Go to the trigger build of task configuration: Configure -> Build Triggers tab

1. ``Enabled Gitee triggers`` Check the build trigger rules you need, such as `Push Event`, `Opened Merge Request Events`, the checked events will receive WebHook and trigger the build. Currently supported trigger events are ：
    - Push Events
    - Commit Comment Events 
    - Opened Merge Request Events 
    - Updated Merge Request Events 
    - Accepted Merge Request Events			
    - Closed Merge Request Events
    - Approved Pull Requests 
    - Tested Pull Requests 
2. `Build Instruction Filter` :
    - `None` 
    - `[ci-skip] skip build` ：When commit message or PR description contains `[ci-skip]`, skip the build trigger.
    - `[ci-build] trigger build` ：When commit message or PR description contains `[ci-build]`, the build will be triggered.
3. `Ignore last commit has build` This option can skip the Commit version that has been built.
4. `Cancel incomplete build on same Pull Requests` This option will determine whether there is an unfinished build with the same PR when the PR triggers the build. If there is, the unfinished build will be cancelled and the current build will be carried out. 
5. `Ignore Pull Request conflicts` This option will select whether to build according to the PR conflict when the PR triggers the build.
6. `Allowed branches` You can configure the branches that are allowed to be built, and currently support branch names and regular expressions for filtering.
7. `Secret Token for Gitee WebHook` This option can configure the password of WebHook, which needs to be consistent with the password configured by Code Cloud WebHook to trigger the construction. 
8. Note: If the PR status is not automatically merged, the build will not be triggered 。
![触发器配置](https://images.gitee.com/uploads/images/2021/0107/171932_e25c8359_2102225.png "屏幕截图.png")

### Post-build step configuration 

Go to the post-build configuration of task configuration: Configure -> Post-build Actions tab

#### Build results back to Gitee

1. Click the drop-down box of `Add post-build action` to select: `Add note with build status on Gitee pull requests` 
2. It can be configured in `Advanced`: 
    - Add message only for failed builds ：Re-evaluate to Code Cloud only for failed builds
    - Customize the content of the feedback for each state (the content can refer to the environment variables of Jenkins, or custom environment variables) 
3. If you turn on this function, you can also review the non-automatically merged state to Gitee.

#### Build successfully and automatically merge PR 
Click the drop-down box of `Add post-build action` to select: `Accept Gitee pull request on success` 

![构建后步骤配置](https://images.gitee.com/uploads/images/2018/0716/192304_0e323bc0_58426.png "屏幕截图.png")

### New Gitee project WebHook 
Enter the Gitee project set in the source management configuration, enter Management -> WebHooks

1. Add WebHook and fill in the URL in `Trigger configuration: Build when a change is pushed to Gitee. Gitee webhook URL`, as shown in ： http://127.0.0.1:8080/jenkins/project/fu
2. Password: the WebHook password configured in point 5 of the trigger configuration, you can leave it without a password 
3. Check PUSH， Pull Request

#### Test push to trigger build 
1. In the WebHook management of Gitee, select the WebHook with PUSH ticked and click test to observe the construction status of the Jenkins task 
2. Edit a file on the Gitee project page and submit it, and observe the build status of the Jenkins task

#### Test PR trigger build 
1. In the WebHook management of Gitee, select the WebHook with Pull Request checked and click test to observe the construction status of the Jenkins task 
2. Create a new Pull Request in the Gitee project and observe the build status of the Jenkins task 

## Use scripts to configure triggers 
```groovy
pipeline {

    agent any

    triggers {
        gitee (
                // Push code 
                triggerOnPush: true,
                // Comment submission record 
                triggerOnCommitComment: true,
                // New Pull Requests
                triggerOnOpenPullRequest: true,
                // Update Pull Requests "0":None "1":Source Branch updated "2":Target Branch updated "3":Both Source and Target Branch updated
                triggerOnUpdatePullRequest: "1",
                // accept  Pull Requests
                triggerOnAcceptedPullRequest: true,
                // Close Pull Requests
                triggerOnClosedPullRequest: true,
                // Approved Pull Requests	
                triggerOnApprovedPullRequest: true,
                // Test passed  Pull Requests
                triggerOnTestedPullRequest: true,
                // comment  Pull Requests
                triggerOnNoteRequest: true,
                // Regular expression for comment content 
                noteRegex: "build",
                // Build command filtering "NONE": None "CI_SKIP":[ci-skip] command to skip building "CI_BUILD":[ci-build] command to trigger build 
                buildInstructionFilterType: "NONE",
                // PR does not require that you filter the build when you have to test 
                ciSkipFroTestNotRequired: false,
                // Filter the Commit version that has been built 
                skipLastCommitHasBeenBuild: false,
                // Cancel the same Pull Requests incomplete construction 
                cancelIncompleteBuildOnSamePullRequest: false,
                // Branches allowed to trigger construction "All": Allow all branches to trigger construction "NameBasedFilter": Filter by branch name  "RegexBasedFilter":Filter branches based on regular expressions 
                branchFilterType: "All",
                // "NameBasedFilter" - include 
                includeBranchesSpec: "include",
                // "NameBasedFilter" - exclude 
                excludeBranchesSpec: "exclude",
                // "RegexBasedFilter" - Regular expression of the target branch 
                targetBranchRegex: "regex",
                // Gitee WebHook password
                secretToken: "123456"
        )
    }

    stages {
        stage('Build') {
            steps{
                echo 'Hello world!'
            }
        }
    }
}
```


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