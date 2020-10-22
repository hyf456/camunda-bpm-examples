package com.han.camunda.bpm.example.reject;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.RepositoryServiceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.PvmTransition;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.TransitionImpl;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description TODO
 * @Date 2020/10/22 14:50
 * @Author hanyf
 */
@Component
public class RejectTransition {

	@Autowired
	private ProcessEngine processEngine;

	private void doReject(Task rejectTask, CommonTaskVO commonTaskVO) {
		TaskService taskService = processEngine
				.getTaskService();
		HistoryService historyService = processEngine.getHistoryService();
		// 工作流实例ID
		String processInstanceId = rejectTask.getProcessInstanceId();
		RepositoryService repositoryService = processEngine.getRepositoryService();
		RuntimeService runtimeService = processEngine.getRuntimeService();
		ExecutionEntity executionEntity = (ExecutionEntity) runtimeService.createExecutionQuery()
				.executionId(processInstanceId)
				.singleResult();
		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
				.getDeployedProcessDefinition(executionEntity.getProcessDefinitionId());
		// 参数
		// VariableMapImpl variables = executionEntity.getVariables();
		//当前活动环节
		ActivityImpl currActivity = processDefinitionEntity
				.findActivity(executionEntity.getActivityId());
		// 9499125668130816
		// 历史的任务实例 通过 act_hi_taskinst 中的 ACT_INST_ID_ 获取需要驳回的节点  Activity_14fxask:9499125668130816
		// 历史的活动实例 通过 act_hi_actinst 中的 ID_ 获取需要驳回的节点  Activity_14fxask:9499125668130816
		//目标活动节点
		// Activity_14fxask
		String activityId = commonTaskVO.getActivityId();
		ActivityImpl nextActivity = processDefinitionEntity.findActivity(activityId);
		if (currActivity != null) {
			// 所有的出口集合 获取所在节点的流出方向
			List<PvmTransition> pvmTransitions = currActivity.getOutgoingTransitions();
			List<PvmTransition> oriPvmTransitions = new ArrayList<PvmTransition>();
			for (PvmTransition transition : pvmTransitions) {
				oriPvmTransitions.add(transition);
			}
			//清除所有出口
			pvmTransitions.clear();
			//建立新的出口
			List<TransitionImpl> transitionImpls = new ArrayList<TransitionImpl>();
			TransitionImpl tImpl = currActivity.createOutgoingTransition();
			tImpl.setDestination(nextActivity);
			transitionImpls.add(tImpl);
			List<Task> taskList = taskService
					.createTaskQuery()
					.processInstanceId(executionEntity.getProcessInstanceId())
					.taskDefinitionKey(executionEntity.getActivityId())
					.list();
			for (Task task : taskList) {
				Map<String, Object> variables = new HashMap<>();
				taskService.complete(task.getId(), variables);
				historyService.deleteHistoricTaskInstance(task.getId());
			}
			for (TransitionImpl transitionImpl : transitionImpls) {
				currActivity.getOutgoingTransitions().remove(transitionImpl);
			}
			for (PvmTransition pvmTransition : oriPvmTransitions) {
				pvmTransitions.add(pvmTransition);
			}
			// TODO: 2020/10/16 act_hi_actinst 没有删除
		}
	}
}
