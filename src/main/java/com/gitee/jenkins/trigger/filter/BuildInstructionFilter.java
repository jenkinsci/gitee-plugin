package com.gitee.jenkins.trigger.filter;

/**
 * @author zhanggx
 */
public interface BuildInstructionFilter {

    /**
     * 是否触发构建
     *
     * @param body
     * @return
     */
    boolean isBuildAllow(String body);

}
