package com.plexobject.nsauto.jenkins.plugin;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ReportInfo {
    private String application;
    private String group;
    private String account;
    private String platform;
    private String packageId;
    private Long task;
    private String binary;
    private String creator;
    private String created;

    public ReportInfo() {

    }

    public static ReportInfo fromJson(String json) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(json);
        ReportInfo reportInfo = new ReportInfo();
        reportInfo.setApplication((String) jsonObject.get("application"));
        reportInfo.setGroup((String) jsonObject.get("group"));
        reportInfo.setAccount((String) jsonObject.get("account"));
        reportInfo.setPlatform((String) jsonObject.get("platform"));
        reportInfo.setPackageId((String) jsonObject.get("package"));
        reportInfo.setTask((Long) jsonObject.get("task"));
        reportInfo.setCreator((String) jsonObject.get("creator"));
        reportInfo.setCreated((String) jsonObject.get("created"));
        return reportInfo;

    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public Long getTask() {
        return task;
    }

    public void setTask(Long task) {
        this.task = task;
    }

    public String getBinary() {
        return binary;
    }

    public void setBinary(String binary) {
        this.binary = binary;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "ReportInfo [application=" + application + ", group=" + group + ", account=" + account + ", platform="
               + platform + ", packageId=" + packageId + ", task=" + task + ", binary=" + binary + ", creator="
               + creator + ", created=" + created + "]";
    }

}
