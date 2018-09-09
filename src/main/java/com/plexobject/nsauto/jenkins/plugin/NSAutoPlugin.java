package com.plexobject.nsauto.jenkins.plugin;

import java.io.File;
import java.io.IOException;

import javax.mail.MessagingException;
import javax.servlet.ServletException;

import org.json.simple.parser.ParseException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONException;

public class NSAutoPlugin extends Builder implements SimpleBuildStep {

    private static final String DEFAULT_URL = "https://lab-api.nowsecure.com/build/?";
    private String apiUrl;
    private String group;
    private Secret apiKey;
    private String binaryName;
    private String description;

    @DataBoundConstructor
    public NSAutoPlugin(String apiUrl, String group, Secret apiKey, String binaryName, String description) {
        this.apiUrl = apiUrl != null && apiUrl.length() > 0 ? apiUrl : DEFAULT_URL;
        this.group = group;
        this.apiKey = apiKey;
        this.binaryName = binaryName;
        this.description = description;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    @DataBoundSetter
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getGroup() {
        return group;
    }

    @DataBoundSetter
    public void setGroup(String group) {
        this.group = group;
    }

    public Secret getApiKey() {
        return apiKey;
    }

    @DataBoundSetter
    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
    }

    public String getBinaryName() {
        return binaryName;
    }

    @DataBoundSetter
    public void setBinaryName(String binaryName) {
        this.binaryName = binaryName;
    }

    public String getDescription() {
        return description;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        try {
            String json = upload(new File(workspace.getRemote()));
            ReportInfo report = ReportInfo.fromJson(json);
            listener.getLogger().println("UPLOADED Results");
            listener.getLogger().println(json);
            listener.getLogger().println("REPORT Summary");
            listener.getLogger().println(report);
        } catch (Exception e) {
            listener.getLogger().println("Failed to upload due to " + e);
        }
    }

    private String upload(File artifactsDir) throws IOException, ParseException {
        if (binaryName == null || binaryName.length() == 0) {
            return null;
        }
        File file = IOHelper.find(artifactsDir, binaryName);
        if (file == null) {
            return null;
        }
        //
        String url = apiUrl;
        if (group != null && group.length() > 0) {
            if (url.contains("?")) {
                url += "&";
            } else {
                url += "?";
            }
            url += "group=" + group;
        }
        return IOHelper.upload(url, apiKey.getPlainText(), file.getCanonicalPath());
    }

    // @Symbol("apiKey")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doTestApiKey(@QueryParameter("apiKey") final String apiKey)
                throws MessagingException, IOException, JSONException, ServletException {
            if (apiKey != null && apiKey.length() > 50) {
                return FormValidation.ok();
            } else {
                return FormValidation.errorWithMarkup(Messages.NSAutoPlugin_DescriptorImpl_errors_missingKey());
            }
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.NSAutoPlugin_DescriptorImpl_DisplayName();
        }

    }

}
