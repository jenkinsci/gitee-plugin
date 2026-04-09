package com.gitee.jenkins.connection;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

public class GiteeApiRepoProperty extends JobProperty<Job<?, ?>> {

    private ListBoxModel options;
    
    public GiteeApiRepoProperty() { }

    @Override
    public JobPropertyDescriptor getDescriptor() {
        DescriptorImpl descriptor = (DescriptorImpl) super.getDescriptor();
        descriptor.setDescriptorOptions(options);
        return descriptor;
    }

    @DataBoundConstructor
    public GiteeApiRepoProperty(ListBoxModel options) {
        this.options = options;
    }

    public ListBoxModel getOptions() {
        return options;
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
            this.descriptorOptions = options;
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

        @JavaScriptMethod
        public boolean removeAllRepoOwners() {
            if (descriptorOptions.size() > 0) {
                descriptorOptions.clear();
                save();
                return true;
            }
            return false;
        }

        @JavaScriptMethod
        public boolean removeRepoOwner(String repoOwner) {
            boolean isRemoved = descriptorOptions.removeIf(elem -> elem.value.equals(repoOwner));
            save();
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