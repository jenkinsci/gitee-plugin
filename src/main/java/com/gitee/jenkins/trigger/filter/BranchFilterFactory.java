package com.gitee.jenkins.trigger.filter;

/**
 * @author Robin Müller
 */
public final class BranchFilterFactory {

    private BranchFilterFactory() { }

    public static BranchFilter newBranchFilter(BranchFilterConfig config) {
		
		if(config == null)
			return new AllBranchesFilter();
		
        switch (config.getType()) {
            case NameBasedFilter:
                return new NameBasedFilter(config.getIncludeBranchesSpec(), config.getExcludeBranchesSpec());
            case RegexBasedFilter:
                return new RegexBasedFilter(config.getTargetBranchRegex());
            default:
                return new AllBranchesFilter();
        }
    }
}
