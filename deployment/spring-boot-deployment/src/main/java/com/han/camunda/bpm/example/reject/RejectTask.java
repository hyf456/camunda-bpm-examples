package com.han.camunda.bpm.example.reject;

import com.google.common.collect.Lists;
import com.han.camunda.bpm.example.util.NumberUtil;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.ManagementServiceImpl;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricActivityInstanceManager;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.PvmTransition;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.camunda.bpm.engine.ActivityTypes.GATEWAY_EXCLUSIVE;
import static org.camunda.bpm.engine.ActivityTypes.START_EVENT;

/**
 * @Description TODO
 * @Date 2020/10/16 16:35
 * @Author hanyf
 */
@Component
public class RejectTask {

	@Autowired
	private HistoryService historyService;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private RuntimeService runtimeService;

	/**
	 * 驳回任务方封装
	 *
	 * @param destinationTaskID 驳回的任务ID 目标任务ID
	 * @param messageContent  驳回的理由
	 * @param currentTaskID  当前正要执行的任务ID
	 * @return 驳回结果 携带下个任务编号
	 */
	public String rejectTask(String destinationTaskID, String currentTaskID, String messageContent) {
		// region 目标任务实例 historicDestinationTaskInstance 带流程变量，任务变量
		HistoricTaskInstance historicDestinationTaskInstance = historyService
				.createHistoricTaskInstanceQuery()
				.taskId(destinationTaskID)
				// .includeProcessVariables()
				// .includeTaskLocalVariables()
				.singleResult();
		// endregion
		// region 正在执行的任务实例 historicCurrentTaskInstance 带流程变量，任务变量
		HistoricTaskInstance historicCurrentTaskInstance = historyService
				.createHistoricTaskInstanceQuery()
				.taskId(currentTaskID)
				// .includeProcessVariables()
				// .includeTaskLocalVariables()
				.singleResult();
		// endregion
		// 流程定义ID
		String processDefinitionId = historicCurrentTaskInstance.getProcessDefinitionId();
		// 流程实例ID
		String processInstanceId = historicCurrentTaskInstance.getProcessInstanceId();
		// 流程定义实体
		ProcessDefinitionEntity processDefinition =
				(ProcessDefinitionEntity) repositoryService.getProcessDefinition(processDefinitionId);
		// region 根据任务创建时间正序排序获取历史任务实例集合 historicTaskInstanceList 含流程变量，任务变量
		List<HistoricTaskInstance> historicTaskInstanceList = historyService
				.createHistoricTaskInstanceQuery()
				.processInstanceId(processInstanceId)
				.orderByHistoricActivityInstanceStartTime()
				// .includeProcessVariables()
				// .includeTaskLocalVariables()
				// .orderByTaskCreateTime()
				.asc()
				.list();
		// endregion
		// region 历史活动节点实例集合 historicActivityInstanceList
   		List<HistoricActivityInstance> historicActivityInstanceList =
				historyService
						.createHistoricActivityInstanceQuery()
						.processInstanceId(processInstanceId)
						.orderByHistoricActivityInstanceStartTime()
						.asc()
						.list();
		// endregion
		// 获取目标任务的节点信息
		ActivityImpl destinationActivity = processDefinition
				.findActivity(historicDestinationTaskInstance.getTaskDefinitionKey());
		// 定义一个历史任务集合，完成任务后任务删除此集合中的任务
		List<HistoricTaskInstance> deleteHistoricTaskInstanceList = new ArrayList<>();
		// 定义一个历史活动节点集合，完成任务后要添加的历史活动节点集合
		List<HistoricActivityInstanceEntity> insertHistoricTaskActivityInstanceList = new ArrayList<>();
		// 目标任务编号
		Long destinationTaskInstanceId = NumberUtil.getLong(destinationTaskID);
		// 有序
		for (HistoricTaskInstance historicTaskInstance : historicTaskInstanceList) {
			Long historicTaskInstanceId = NumberUtil.getLong(historicTaskInstance.getId());
			if (destinationTaskInstanceId <= historicTaskInstanceId) {
				deleteHistoricTaskInstanceList.add(historicTaskInstance);
			}
		}
		// 有序
		for (int i = 0; i < historicActivityInstanceList.size() - 1; i++) {
			HistoricActivityInstance historicActivityInstance = historicActivityInstanceList.get(i);
			// 历史活动节点的任务编号
			Long historicActivityInstanceTaskId;
			String taskId = historicActivityInstance.getTaskId();
			if (taskId != null) {
				historicActivityInstanceTaskId = NumberUtil.getLong(taskId);
				if (historicActivityInstanceTaskId <= destinationTaskInstanceId) {
					insertHistoricTaskActivityInstanceList.add((HistoricActivityInstanceEntity) historicActivityInstance);
				}
			} else {
				if (historicActivityInstance.getActivityType().equals(START_EVENT)) {
					insertHistoricTaskActivityInstanceList.add((HistoricActivityInstanceEntity) historicActivityInstance);
				} else if (historicActivityInstance.getActivityType().equals(GATEWAY_EXCLUSIVE)) {
					// insertHistoricTaskActivityInstanceList.add((HistoricActivityInstanceEntity) historicActivityInstance);
				}
			}
		}
		// 获取流程定义的节点信息
		List<ActivityImpl> processDefinitionActivities = processDefinition.getActivities();
		// 用于保存正在执行的任务节点信息
		ActivityImpl currentActivity = null;
		// 用于保存原来的任务节点的出口信息
		PvmTransition pvmTransition = null;
		// 保存原来的流程节点出口信息
		for (ActivityImpl activity : processDefinitionActivities) {
			if (historicCurrentTaskInstance.getTaskDefinitionKey().equals(activity.getId())) {
				currentActivity = activity;
				// 备份
				pvmTransition = activity.getOutgoingTransitions().get(0);
				// 清空当前任务节点的出口信息
				activity.getOutgoingTransitions().clear();
			}
		}

		// 执行流程转向
		// processEngine.getManagementService().executeCommand(
		// 		new RejectTaskCMD(historicDestinationTaskInstance, historicCurrentTaskInstance, destinationActivity));
		((ManagementServiceImpl) processEngine.getManagementService())
				.getCommandExecutor()
				.execute(new RejectTaskCMD(historicDestinationTaskInstance,
						historicCurrentTaskInstance, destinationActivity));
		// // 获取正在执行的任务的流程变量
		// Map<String, Object> taskLocalVariables = historicCurrentTaskInstance.getTaskLocalVariables();
		// // 获取目标任务的流程变量，修改任务不自动跳过，要求审批
		// Map<String, Object> processVariables = historicDestinationTaskInstance.getProcessVariables();
		// // 获取流程发起人编号
		// Integer employeeId = (Integer) processVariables.get(ProcessConstant.PROCESS_START_PERSON);
		// processVariables.put(ProcessConstant.SKIP_EXPRESSION, false);
		// taskLocalVariables.put(ProcessConstant.SKIP_EXPRESSION, false);
		// // 设置驳回原因
		// taskLocalVariables.put(ProcessConstant.REJECT_REASON, messageContent);
		// // region 流程变量转换
		// // 修改下个任务的任务办理人
		// processVariables.put(ProcessConstant.DEAL_PERSON_ID, processVariables.get(ProcessConstant.CURRENT_PERSON_ID));
		// // 修改下个任务的任务办理人姓名
		// processVariables.put(ProcessConstant.DEAL_PERSON_NAME, processVariables.get(ProcessConstant.CURRENT_PERSON_NAME));
		// // 修改下个任务的任务办理人
		// taskLocalVariables.put(ProcessConstant.DEAL_PERSON_ID, processVariables.get(ProcessConstant.CURRENT_PERSON_ID));
		// // 修改下个任务的任务办理人姓名
		// taskLocalVariables.put(ProcessConstant.DEAL_PERSON_NAME, processVariables.get(ProcessConstant.CURRENT_PERSON_NAME));
		// // endregion
		// // 完成当前任务，任务走向目标任务
		processEngine.getTaskService().complete(currentTaskID);
		// taskService.complete(commonTaskVO.getTaskId(), formData);
		if (currentActivity != null) {
			// 清空临时转向信息
			currentActivity.getOutgoingTransitions().clear();
		}
		if (currentActivity != null) {
			// 恢复原来的走向
			currentActivity.getOutgoingTransitions().add(pvmTransition);
		}
		// 删除历史任务
		for (HistoricTaskInstance historicTaskInstance : deleteHistoricTaskInstanceList) {
			historyService.deleteHistoricTaskInstance(historicTaskInstance.getId());
		}
		// 删除活动节点
		((ManagementServiceImpl) processEngine.getManagementService())
				.getCommandExecutor()
				.execute(
						(Command<List<HistoricActivityInstanceEntity>>) commandContext -> {
							HistoricActivityInstanceManager historicActivityInstanceEntityManager =
									commandContext.getHistoricActivityInstanceManager();
							// 删除所有的历史活动节点
							historicActivityInstanceEntityManager
									.deleteHistoricActivityInstancesByProcessInstanceIds(Lists.newArrayList(processInstanceId));
							// 提交到数据库
							commandContext.getDbSqlSession().flush();
							// 添加历史活动节点的
							// for (HistoricActivityInstanceEntity historicActivityInstance : insertHistoricTaskActivityInstanceList) {
							// 	// historicActivityInstanceEntityManager.insertHistoricActivityInstance(historicActivityInstance);
							// }
							// 提交到数据库
							// commandContext.getDbSqlSession().flush();
							return null;
						}
				);

		// processEngine.getManagementService().executeCommand(
		// 		(Command<List<HistoricActivityInstanceEntity>>) commandContext -> {
		// 			HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager =
		// 					commandContext.getHistoricActivityInstanceEntityManager();
		// 			// 删除所有的历史活动节点
		// 			historicActivityInstanceEntityManager
		// 					.deleteHistoricActivityInstancesByProcessInstanceId(processInstanceId);
		// 			// 提交到数据库
		// 			commandContext.getDbSqlSession().flush();
		// 			// 添加历史活动节点的
		// 			for (HistoricActivityInstanceEntity historicActivityInstance : insertHistoricTaskActivityInstanceList) {
		// 				historicActivityInstanceEntityManager.insertHistoricActivityInstance(historicActivityInstance);
		// 			}
		// 			// 提交到数据库
		// 			commandContext.getDbSqlSession().flush();
		// 			return null;
		// 		}
		// );
		// 返回下个任务的任务ID
		// return nextTaskId;
		return null;
	}
}
