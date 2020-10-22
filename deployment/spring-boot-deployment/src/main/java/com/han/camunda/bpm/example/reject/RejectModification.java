package com.han.camunda.bpm.example.reject;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description 驳回采用修正方式
 * @Date 2020/10/22 14:36
 * @Author hanyf
 */
@Component
public class RejectModification {

	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private HistoryService historyService;

	public void reject(CommonTaskVO commonTaskVO) {
		String taskId = commonTaskVO.getTaskId();
		String destinationTaskId = commonTaskVO.getDestinationTaskId();
		HistoricTaskInstance historicTaskInstance = historyService
				.createHistoricTaskInstanceQuery()
				.taskId(taskId)
				.singleResult();

		HistoricTaskInstance historicDestinationTaskInstance = historyService
				.createHistoricTaskInstanceQuery()
				.taskId(destinationTaskId)
				.singleResult();

		String processInstanceId= historicTaskInstance.getProcessInstanceId();
		String message= commonTaskVO.getRemark();
		Task task = taskService.createTaskQuery()
				.taskAssignee("韩云飞5") //当前登录用户的id
				.processInstanceId(processInstanceId)
				.singleResult();
		ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);
		// 得到需要驳回的活动节点
		HistoricActivityInstance historicActivityInstance = historyService
				.createHistoricActivityInstanceQuery()
				.activityInstanceId(historicDestinationTaskInstance.getActivityInstanceId())
				.singleResult();
		String toActId = historicActivityInstance.getActivityId();
		String assignee = historicActivityInstance.getAssignee();
		//设置流程中的可变参数
		Map<String, Object> taskVariable = new HashMap<>(2);
		taskVariable.put("user", assignee);
		taskVariable.put("name", "3213");
		taskService.createComment(task.getId(), processInstanceId, "驳回原因:" + message);
		runtimeService.createProcessInstanceModification(processInstanceId)
				.cancelActivityInstance(getInstanceIdForActivity(tree, task.getTaskDefinitionKey()))//关闭相关任务
				.setAnnotation("进行任务驳回")
				.startBeforeActivity(toActId)//启动目标活动节点
				.setVariables(taskVariable)//流程的可变参数赋值
				.execute();
		historyService.deleteHistoricTaskInstance(task.getId());
	}

	private String getInstanceIdForActivity(ActivityInstance activityInstance, String activityId) {
		ActivityInstance instance = getChildInstanceForActivity(activityInstance, activityId);
		if (instance != null) {
			return instance.getId();
		}
		return null;
	}

	private ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
		if (activityId.equals(activityInstance.getActivityId())) {
			return activityInstance;
		}
		for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
			ActivityInstance instance = getChildInstanceForActivity(childInstance, activityId);
			if (instance != null) {
				return instance;
			}
		}
		return null;
	}
}
