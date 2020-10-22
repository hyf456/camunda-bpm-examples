package com.han.camunda.bpm.example.reject;

import org.camunda.bpm.engine.task.Task;

import java.io.Serializable;

/**
 * 通用任务信息
 *
 * @author: xiexindong
 * @date: 2020-08-19 20:06
 */
public class CommonTaskVO<T extends CommonTaskVO> implements Serializable {
    /**
     * 应用标识
     */
    private String appKey;
    /**
     * 所属组织ID
     */
    private Long orgId;

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public Long getTeaProcessId() {
        return teaProcessId;
    }

    public void setTeaProcessId(Long teaProcessId) {
        this.teaProcessId = teaProcessId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Integer getWithFlag() {
        return withFlag;
    }

    public void setWithFlag(Integer withFlag) {
        this.withFlag = withFlag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }

    public Boolean getAutoClaim() {
        return autoClaim;
    }

    public void setAutoClaim(Boolean autoClaim) {
        this.autoClaim = autoClaim;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getDestinationTaskId() {
        return destinationTaskId;
    }

    public void setDestinationTaskId(String destinationTaskId) {
        this.destinationTaskId = destinationTaskId;
    }

    /**
     * 所属组织名称
     */
    private String orgName;
    /**
     * 大益工作流实例ID
     */
    private Long teaProcessId;
    /**
     * 工作流实例ID
     */
    private String processInstanceId;
    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 包含标志(参见TeaTaskWithEnum)
     */
    private Integer withFlag;
    /**
     * 备注
     */
    private String remark;
    /**
     * 表单参数Json
     */
    private String formData;
    /**
     * 是否自动认领
     */
    private Boolean autoClaim;

    /**
     * 任务
     */
    private transient Task task;
    /**
     * 活动ID
     */
    private String activityId;
    /**
     * 驳回任务ID
     */
    private String destinationTaskId;

}
