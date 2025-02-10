package com.gitee.jenkins.trigger.filter;

/**
 * @author Robin Müller
 */
public final class BranchFilterFactory {

    private BranchFilterFactory() { }

    public static BranchFilter newBranchFilter(BranchFilterConfig config) {

		if(config == null)
			return new AllBranchesFilter();

        return switch (config.getType()) {
            case NameBasedFilter ->
                new NameBasedFilter(config.getIncludeBranchesSpec(), config.getExcludeBranchesSpec());
            case RegexBasedFilter -> new RegexBasedFilter(config.getTargetBranchRegex());
            default -> new AllBranchesFilter();
        };
    }
}
