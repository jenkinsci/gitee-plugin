package com.gitee.jenkins.connection;

import org.eclipse.jgit.util.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

public class GiteeApiRepoProperty extends JobProperty<Job<?, ?>> {

    private ListBoxModel options;
    private String repoOwner;

    public GiteeApiRepoProperty() { }

    @Override
    public JobPropertyDescriptor getDescriptor() {
        DescriptorImpl descriptor = (DescriptorImpl) super.getDescriptor();
        descriptor.setDescriptorOptions(options);
        return descriptor;
    }

    @DataBoundConstructor
    public GiteeApiRepoProperty(ListBoxModel options) {
        if (options == null) {
            this.options = new ListBoxModel();
        } else {
            this.options = options;
        }
    }

    public ListBoxModel getOptions() {
        return options;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    @DataBoundSetter
    public void setRepoOwner(String repoOwner) {
        this.repoOwner = repoOwner;
    }

    @Extension
    @Symbol("giteeApiRepo")
    public static class DescriptorImpl extends JobPropertyDescriptor implements ApiDesciptor {

        private ListBoxModel descriptorOptions;

        public DescriptorImpl() {
            
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Gitee Repo for API";
        }

        public ListBoxModel getDescriptorOptions() {
            return descriptorOptions;
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            GiteeApiRepoProperty prop = req.bindJSON(GiteeApiRepoProperty.class, formData);
            if (descriptorOptions != null) {
                prop.options = descriptorOptions;
            }
            descriptorOptions = null;
            return prop;
        }

        private void setDescriptorOptions(ListBoxModel options) {
            if (options == null) {
                this.descriptorOptions = new ListBoxModel();
            } else {
                this.descriptorOptions = options;
            }   
        }

        private void addOption(String option) {
            if (descriptorOptions == null) {
                descriptorOptions = new ListBoxModel(); 
            }
            descriptorOptions.add(option, option);
        }

        public ListBoxModel doFillGiteeApiRepoItems() {   
            if (descriptorOptions == null) {
                load();
            }
            return descriptorOptions;    
        }

        public FormValidation doCheckRepoOwner(@QueryParameter String value, @QueryParameter String repo, @QueryParameter String owner) {
            if (StringUtils.isEmptyOrNull(repo) && StringUtils.isEmptyOrNull(owner)) {
                return FormValidation.ok("Fill in both repo and owner string for API use");
            }
            if (StringUtils.isEmptyOrNull(repo)) {
                return FormValidation.error("Fill in repo string");
            }
            if (StringUtils.isEmptyOrNull(owner)) {
                return FormValidation.error("Fill in owner string");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckOwner(@QueryParameter String value) {
            if (StringUtils.isEmptyOrNull(value)) {
                return FormValidation.error("Fill in owner string");
            } else {
                return FormValidation.ok();
            }
        }

        @JavaScriptMethod
        public boolean removeAllRepoOwners() {
            if (descriptorOptions.size() > 0) {
                descriptorOptions.clear();
                return true;
            }
            return false;
        }

        @JavaScriptMethod
        public boolean removeRepoOwner(String repoOwner) {
            boolean isRemoved = descriptorOptions.removeIf(elem -> elem.value.equals(repoOwner));
            return isRemoved;
        }


        @JavaScriptMethod
        public void addRepoOwner(String repo, String owner) {
            String repoAndOwnerString = repo + " " + owner;
            addOption(repoAndOwnerString);
            save();
        }
    }
}