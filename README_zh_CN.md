# 目录
- [简介](#简介)
  - [目前支持特性](#目前支持特性)
  - [计划中特性](#计划中特性)
- [插件安装](#插件安装)
- [插件配置](#插件配置)
  - [添加码云链接配置](#添加码云链接配置)
  - [添加码云APIV5私人令牌](#添加码云APIV5私人令牌)
  - [构建任务配置](#构建任务配置)
    - [新建构建任务](#新建构建任务)
    - [源码管理配置](#源码管理配置)
    - [触发器配置](#触发器配置)
    - [WebHook密码配置](#WebHook密码配置)
    - [构建后步骤配置](#构建后步骤配置)
      - [构建结果回评至码云](#构建结果回评至码云)
      - [构建成功自动合并PR](#构建成功自动合并PR)
    - [新建码云项目WebHook](#新建码云项目WebHook)
      - [测试推送触发构建](#测试推送触发构建)
      - [测试PR触发构建](#测试PR触发构建)
- [用户支持](#用户支持)
- [参与贡献](#参与贡献)
  - [打包或运行测试](#打包或运行测试)
   

# 简介
Gitee Jenkins Plugin 是码云基于 [GitLab Plugin](https://github.com/jenkinsci/gitlab-plugin) 开发的 Jenkins 插件。用于配置 Jenkins 触发器，接受码云平台发送的 WebHook 触发 Jenkins 进行自动化持续集成或持续部署，并可将构建状态反馈回码云平台。

## 目前支持特性：
- 推送代码到码云时，由配置的 WebHook 触发 Jenkins 任务构建。
- 提交 Pull Request 到码云项目时，由配置的 WebHook 触发 Jenkins 任务构建，支持PR动作：新建，更新，接受，关闭，审查通过，测试通过。
- 支持 [ci-skip] 指令过滤。
- 过滤已经构建的 Commit 版本，若是分支 Push，则相同分支Push才过滤，若是 PR，则是同一个PR才过滤。
- 按分支名过滤触发器。
- 正则表达式过滤可触发的分支。
- 设置 WebHook 验证密码。
- 构建后操作可配置 PR 触发的构建结果评论到码云对应的PR中。
- 构建后操作可配置 PR 触发的构建成功后可自动合并对应PR。
- 对于 PR 相关的所有事件，若 PR 代码冲突不可自动合并，则不触发构建；且若配置了评论到PR的功能，则评论到 PR 提示冲突。
- PR 评论可通过 WebHook 触发构建（可用于 PR 触发构建失败是便于从码云平台评论重新触发构建）
- 支持配置 PR 不要求必须测试时过滤触发构建。（可用于不需测试则不构建部署测试环境）

## 计划中特性
1. PR 审查并测试通过触发构建（可用户触发部署，且可配合自动合并 PR 的特性完善工作流。）
2. 勾选触发方式自动添加WebHook至码云。

# 插件安装
1. 在线安装
    - 前往 Manage Jenkins -> Manage Plugins -> Available
    - 右侧 Filter 输入： Gitee
    - 下方可选列表中勾选 Gitee（如列表中不存在 Gitee，则点击 Check now 更新插件列表）
    - 点击 Download now and install after restart

![输入图片说明](https://images.gitee.com/uploads/images/2018/0723/112748_b81a1ee3_58426.png "屏幕截图.png")

2. 手动安装
    - 从 [release](https://gitee.com/oschina/Gitee-Jenkins-Plugin/releases) 列表中进入最新发行版，下载对应的 XXX.hpi 文件
    - 前往 Manage Jenkins -> Manage Plugins -> Advanced
    - Upload Plugin File 中选择刚才下载的 XXX.hpi 点击 Upload
    - 后续页面中勾选 Restart Jenkins when installation is complete and no jobs are running

![输入图片说明](https://images.gitee.com/uploads/images/2018/0723/113303_2a1d0a03_58426.png "屏幕截图.png")

# 插件配置

## 添加码云链接配置
1. 前往 Jenkins -> Manage Jenkins -> Configure System -> Gitee Configuration -> Gitee connections
2. 在 ``Connection name`` 中输入 ``Gitee`` 或者你想要的名字
3. ``Gitee host URL`` 中输入码云完整 URL地址： ``https://gitee.com`` （码云私有化客户输入部署的域名）
4. ``Credentials`` 中如还未配置码云 APIV5 私人令牌，点击 ``Add`` - > ``Jenkins ``
    1. ``Domain`` 选择 ``Global credentials``
    2. ``Kind`` 选择 ``Gitee API Token``
    3. ``Scope`` 选择你需要的范围
    4. ``Gitee API Token`` 输入你的码云私人令牌，获取地址：https://gitee.com/profile/personal_access_tokens
    5. ``ID``, ``Descripiton`` 中输入你想要的 ID 和描述即可。
5. ``Credentials`` 选择配置好的 Gitee APIV5 Token
6. 点击 ``Advanced`` ，可配置是否忽略 SSL 错误（适您的Jenkins环境是否支持），并可设置链接测超时时间（适您的网络环境而定）
7. 点击 ``Test Connection`` 测试链接是否成功，如失败请检查以上 3，5，6 步骤。

配置成功后如图所示：
![码云链接配置](https://images.gitee.com/uploads/images/2018/0716/185651_68707d16_58426.png "屏幕截图.png")

### 新建构建任务
前往 Jenkins -> New Item , name 输入 'Gitee Test'，选择 ``Freestyle project`` 保存即可创建构建项目。

### 任务全局配置

任务全局配置中需要选择前一步中的码云链接。前往某个任务（如'Gitee Test'）的 Configure -> General，Gitee connection 中选择前面所配置的码云联机，如图：

![任务全局配置](https://images.gitee.com/uploads/images/2018/0716/191715_9660237b_58426.png "屏幕截图.png")


### 源码管理配置

前往某个任务（如'Gitee Test'）的 Configure -> Source Code Management 选项卡

1. 点击 *Git*
2. 输入你的仓库地址，例如 ``git@your.gitee.server:gitee_group/gitee_project.git``
    1. 点击 *Advanced* 按钮, *Name* 字段中输入 ``origin``， *Refspec* 字段输入 ``+refs/heads/*:refs/remotes/origin/* +refs/pull/*/MERGE:refs/pull/*/MERGE``
3. *Branch Specifier* 选项:
    1. 对于单仓库工作流输入: ``origin/${giteeSourceBranch}``
    2. 对于 PR 工作流输入: ``pull/${giteePullRequestIid}/MERGE``
4. *Additional Behaviours* 选项：
    1. 对于单仓库工作流，如果你希望推送的分支构建前合并默认分支（发布的分支），可以做以下操作：
        1. 点击 *Add* 下拉框
        2. 选择 *Merge before build* 
        3. 设置 *Name of repository* 为 ``origin``
        4. 设置 *Branch to merge to* 为 ``${ReleaseBranch}`` 即您要合并的默认分支（发布分支）
    2. 对于 PR 工作流，码云服务端已经将 PR 的原分支和目标分支作了预合并，您可以直接构建，如果目标分支不是默认分支（发布分支），您也可以进行上诉构建前合并。

配置如图所示：

![源码管理配置](https://images.gitee.com/uploads/images/2018/0716/191913_ef0995f4_58426.png "屏幕截图.png")

### 触发器配置

前往任务配置的触发器构建： Configure -> Build Triggers 选项卡

1. ``Enabled Gitee triggers`` 勾选您所需要的构建触发规则，如 `Push Event`, `Opened Merge Request Events`，勾选的事件会接受WebHook，触发构建。目前支持触发事件有：
    - Push Events ：推送代码事件
    - Opened Merge Request Events ：提交 PR 事件
    - Updated Merge Request Events ：更新 PR 事件	
    - Accepted Merge Request Events	 ：接受/合并 PR 事件		
    - Closed Merge Request Events ：关闭 PR 事件
    - Approved Pull Requests ： 审查通过 PR 事件
    - Tested Pull Requests ：测试通过 PR 事件
2. `Enable [ci-skip]` 该选项可以开启支持 `[ci-skip]` 指令，只要commit message 中包含 `[ci-skip]`，当前commit 即可跳过构建触发。
3. `Ignore last commit has build` 该选项可以跳过已经构建过的 Commit 版本。
4. `Allowed branches` 可以配置允许构建的分支，目前支持分支名和正则表达式的方式进行过滤。
5. `Secret Token for Gitee WebHook` 该选项可以配置 WebHook 的密码，该密码需要与码云 WebHook配置的密码一致方可触发构建。
6. 注意：若 PR 状态为不可自动合并，则不触发构建。

![触发器配置](https://images.gitee.com/uploads/images/2018/0724/120539_106f7480_58426.png "屏幕截图.png")

### 构建后步骤配置

前往任务配置的构建后配置： Configure -> Post-build Actions 选项卡

#### 构建结果回评至码云

1. 点击 `Add post-build action` 下拉框选择：`Add note with build status on Gitee pull requests`
2. `Advanced` 中可以配置：
    - Add message only for failed builds ：仅为构建失败回评到码云
    - 自定义各状态的回评内容（内容可以引用 Jenkins 的环境变量，或者自定义的环境变量）
3. 若开启该功能，还可将不可自动合并的状态回评至码云

#### 构建成功自动合并PR
点击 `Add post-build action` 下拉框选择：`Accept Gitee pull request on success`

![构建后步骤配置](https://images.gitee.com/uploads/images/2018/0716/192304_0e323bc0_58426.png "屏幕截图.png")

### 新建码云项目WebHook
进入源码管理配置中设置的码云项目中，进入 管理 -> WebHooks 

1. 添加 WebHook， URL 填写 `触发器配置：Build when a change is pushed to Gitee. Gitee webhook URL` 中所示 URL，如：: http://127.0.0.1:8080/jenkins/project/fu
2. 密码填写：触发器配置第 5 点中配置的 WebHook密码，不设密码可以不填
3. 勾选 PUSH， Pull Request

#### 测试推送触发构建
1. 码云的 WebHook 管理中选择勾选了PUSH的 WebHook 点击测试，观察 Jenkins 任务的构建状态
2. 码云项目页面编辑一个文件提交，观察 Jenkins 任务的构建状态

#### 测试PR触发构建
1. 码云的 WebHook 管理中选择勾选了 Pull Request 的 WebHook 点击测试，观察 Jenkins 任务的构建状态
2. 在码云项目中新建一个Pull Request，观察 Jenkins 任务的构建状态


# 用户支持
如在使用过程中有任何疑问，欢迎在 [Gitee Jenkins Issue](https://gitee.com/oschina/Gitee-Jenkins-Plugin/issues) 中反馈。

反馈前可按以下步骤获取更多日志以便排查问题：

1. 前往 Jenkins -> Manage Jenkins -> System Log
2. 点击 Add new log recorder。
3. 输入 'Gitee Jenkins Plugin'。
4. 在下一页面中 Logger 点击 Add ，输入框中填写 'com.gitee.jenkins'，并在 Log level 总选择全部，保存。
5. 完成以上步骤后便可在 'Gitee Jenkins Plugin' 这个日志中查看。


# 参与贡献

欢迎提交 CI 场景特性建议或者直接提交 PR 贡献代码。


## 打包或运行测试

打包 hpi 文件在仓库目录中执行： ``mvn package``

直接运行测试执行：``mvn hpi:run``
