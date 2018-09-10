package com.plexobject.nsauto.jenkins.plugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.mail.MessagingException;
import javax.servlet.ServletException;

import org.json.simple.parser.ParseException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.plexobject.nsauto.jenkins.domain.ReportInfo;
import com.plexobject.nsauto.jenkins.domain.UploadInfo;
import com.plexobject.nsauto.jenkins.utils.IOHelper;

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

    private static final int ONE_MINUTE = 1000 * 60;
    private static final int CVSS_SCORE_THRESHOLD = 9;
    private static final int DEFAULT_WAIT_MINUTES = 15;
    private static final String DEFAULT_URL = "https://lab-api.nowsecure.com";
    private String apiUrl;
    private String group;
    private Secret apiKey;
    private String binaryName;
    private String description;
    private boolean waitForResults;
    private int waitMinutes = DEFAULT_WAIT_MINUTES;
    private boolean breakBuildOnCVSS;
    private int cvssScoreThreshold = CVSS_SCORE_THRESHOLD;

    @DataBoundConstructor
    public NSAutoPlugin(Secret apiKey, String binaryName, String description, String apiUrl, String group,
            boolean waitForResults, int waitMinutes, boolean breakBuildOnCVSS, int cvssScoreThreshold) {
        this.apiUrl = apiUrl;
        this.group = group;
        this.apiKey = apiKey;
        this.binaryName = binaryName;
        this.description = description;
        this.waitForResults = waitForResults;
        this.waitMinutes = waitMinutes;
        this.breakBuildOnCVSS = breakBuildOnCVSS;
        this.cvssScoreThreshold = cvssScoreThreshold;
    }

    public String getApiUrl() {
        return apiUrl != null && apiUrl.length() > 0 ? apiUrl : DEFAULT_URL;
    }

    public String getBaseUrl() throws MalformedURLException {
        URL u = new URL(getApiUrl());
        return u.getProtocol() + "://" + u.getHost() + ":" + u.getPort();
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

    public boolean isWaitForResults() {
        return waitForResults;
    }

    @DataBoundSetter
    public void setWaitForResults(boolean waitForResults) {
        this.waitForResults = waitForResults;
    }

    public int getWaitMinutes() {
        return waitMinutes == 0 || waitMinutes > 100 ? DEFAULT_WAIT_MINUTES : waitMinutes;
    }

    @DataBoundSetter
    public void setWaitMinutes(int waitMinutes) {
        this.waitMinutes = waitMinutes;
    }

    public boolean isBreakBuildOnCVSS() {
        return breakBuildOnCVSS;
    }

    @DataBoundSetter
    public void setBreakBuildOnCVSS(boolean breakBuildOnCVSS) {
        this.breakBuildOnCVSS = breakBuildOnCVSS;
    }

    public int getCvssScoreThreshold() {
        return cvssScoreThreshold == 0 || cvssScoreThreshold > 10 ? CVSS_SCORE_THRESHOLD : cvssScoreThreshold;
    }

    @DataBoundSetter
    public void setCvssScoreThreshold(int cvssScoreThreshold) {
        this.cvssScoreThreshold = cvssScoreThreshold;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        listener.getLogger().println(new Date() + " Executing step for " + this);
        File artifactsDir = run.getArtifactsDir();
        if (!artifactsDir.mkdirs()) {
            listener.getLogger().println(new Date() + " Could not create directory " + artifactsDir);
        }
        try {
            String uploadJson = upload(listener, artifactsDir);
            //
            if (waitForResults) {
                waitForResults(listener, artifactsDir, uploadJson);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            listener.getLogger().println(new Date() + " Failed to upload due to " + e);
        }
    }

    private void waitForResults(TaskListener listener, File artifactsDir, String uploadJson)
            throws IOException, ParseException {
        UploadInfo upload = UploadInfo.fromJson(uploadJson);
        if (upload.getPackageId() == null || upload.getPackageId().length() == 0) {
            throw new IOException("Package-id not found in JSON");
        }
        if (upload.getTask() == 0) {
            throw new IOException("Task not found in JSON");
        }
        String url = getBaseUrl() + "/app/android/" + upload.getPackageId() + "/assessment/" + upload.getTask()
                     + "/results";
        String path = artifactsDir.getCanonicalPath() + "/ns-report.json";
        //
        long started = System.currentTimeMillis();
        for (int min = 0; min < getWaitMinutes(); min++) {
            listener.getLogger().println(new Date() + " Waiting for results for " + url);
            try {
                Thread.sleep(ONE_MINUTE);
            } catch (InterruptedException e) {
                Thread.interrupted();
            } // wait a minute
            String reportJson = IOHelper.get(url, apiKey.getPlainText());
            ReportInfo[] reportInfos = ReportInfo.fromJson(reportJson);
            if (reportInfos.length > 0) {
                IOHelper.save(path, reportJson);
                listener.getLogger().println(new Date() + " Saved analysis report from " + url + " to " + path);
                for (int i = 0; i < reportInfos.length; i++) {
                    if (reportInfos[i].getCvss() >= getCvssScoreThreshold()) {
                        listener.error(new Date() + " Failed on following test:\n" + reportInfos[i].toString());
                        throw new IOException(
                                "Analysis failed because score (" + reportInfos[i].getCvss() + ") exceeded threshold "
                                              + getCvssScoreThreshold() + " for " + reportInfos[i].getTitle());
                    }
                }
                long elapsed = (System.currentTimeMillis() - started) / ONE_MINUTE;
                listener.getLogger().println(new Date() + " NS Security analysis passed in " + elapsed + " minutes");
                return;
            }
        }
        long elapsed = (System.currentTimeMillis() - started) / ONE_MINUTE;
        listener.error(new Date() + " Timedout (" + elapsed
                       + " minutes) while waiting for results from security analysis " + url);
        throw new IOException(
                "Timedout (" + elapsed + " minutes) while waiting for results from security analysis " + url);
    }

    private String upload(TaskListener listener, File artifactsDir) throws IOException, ParseException {
        if (binaryName == null || binaryName.length() == 0) {
            return null;
        }
        File file = IOHelper.find(artifactsDir, binaryName);
        if (file == null) {
            return null;
        }
        //
        String url = getBaseUrl() + "/build/";
        if (group != null && group.length() > 0) {
            url += "?group=" + group;
        }
        listener.getLogger().println(new Date() + " Uploading binary to " + url);

        String uploadJson = IOHelper.upload(url, apiKey.getPlainText(), file.getCanonicalPath());
        String path = artifactsDir.getCanonicalPath() + "/ns-uploaded.json";
        IOHelper.save(path, uploadJson);
        listener.getLogger().println(new Date() + " Uploaded binary to " + url + " and saved output to " + path);
        return uploadJson;
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

    @Override
    public String toString() {
        return "NSAutoPlugin [apiUrl=" + apiUrl + ", group=" + group + ", binaryName=" + binaryName + ", description="
               + description + ", waitForResults=" + waitForResults + ", waitMinutes=" + waitMinutes
               + ", breakBuildOnCVSS=" + breakBuildOnCVSS + ", cvssScoreThreshold=" + cvssScoreThreshold + "]";
    }

}
