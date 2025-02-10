package com.gitee.jenkins.trigger.filter;

/**
 * @author zhanggx
 */
public enum BuildInstructionFilterType implements BuildInstructionFilter {

    /**
     * 无操作
     */
    NONE("") {
        @Override
        public boolean isBuildAllow(String body) {
            return true;
        }
    },
    /**
     * 包含 [ci-skip] 时跳过构建
     */
    CI_SKIP("[ci-skip]") {
        @Override
        public boolean isBuildAllow(String body) {
            return body == null || !body.contains(getBody());
        }
    },
    /**
     * 包含 [ci-build] 时触发构建
     */
    CI_BUILD("[ci-build]") {
        @Override
        public boolean isBuildAllow(String body) {
            return body != null && body.contains(getBody());
        }
    };

    private final String body;

    BuildInstructionFilterType(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

}
