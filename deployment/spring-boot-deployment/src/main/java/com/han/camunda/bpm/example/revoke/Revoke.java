package com.han.camunda.bpm.example.revoke;

import com.google.common.collect.Lists;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.TaskServiceImpl;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.PvmTransition;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Description
 * 从主路撤回到主路的上一节点
 * 从支路撤回到主路的上一节点
 * 从支路撤回到支路的上一节点
 * 从主路撤回到支路的上一节点
 * 必须得有至少一个当前运行节点（对应数据库中表：act_hi_actinst）
 * 至少一个当前运行任务（对应数据库中表：act_ru_task）
 * 至少一个目标运行节点（对应数据库中表：act_hi_actinst）
 * 至少一个目标运行任务（对应数据库中表：act_hi_taskinst）
 * @Date 2020/10/19 11:43
 * @Author hanyf
 */
@Component
public class Revoke {

	private static final Logger log = LoggerFactory.getLogger(Revoke.class);

	@Autowired
	private TaskService taskService;
	@Autowired
	private HistoryService historyService;

	/**
	 * 查询当前正在运行的任务
	 *
	 * @param processInstanceId
	 * @return
	 */
	public List<Task> findCurrentTask(String processInstanceId) {
		List<Task> list = taskService
				.createTaskQuery()
				.processInstanceId(processInstanceId)
				.active()
				.list();
		for (Task task : list) {
			log.info("当前运行的任务id：{},name：{},definaKey:{}", task.getId(), task.getName(),
					task.getTaskDefinitionKey());
		}
		return list;
	}

	/**
	 * 查询要退回的目标任务
	 *
	 * @param targetActivity
	 * @param processInstanceId
	 * @return
	 */
	public HistoricTaskInstanceEntity findTargetTask(String targetActivity, String processInstanceId) {
		List<HistoricTaskInstance> list = historyService
				.createHistoricTaskInstanceQuery()
				.taskDefinitionKey(targetActivity)
				.processInstanceId(processInstanceId)
				.orderByHistoricActivityInstanceStartTime()
				// .orderByTaskCreateTime()
				.desc()
				.list();
		for (HistoricTaskInstance hti : list) {
			log.info("要退回的目标任务id:{},name:{},definaKey:{}",
					hti.getId(), hti.getName(), hti.getTaskDefinitionKey());
		}
		return (HistoricTaskInstanceEntity) list.get(0);
	}

	// 查找当前运行节点：
	// 查找目标运行节点集合：
	// 验证目标节点是否是当前节点的上一个节点：

	/**
	 * 查找目标activity
	 *
	 * @param currentActivities 当前节点的集合
	 * @param targetActivities
	 * @return
	 */
	public ActivityImpl findTargetActivity(List<ActivityImpl> currentActivities, List<ActivityImpl> targetActivities) {
		List<PvmTransition> ptList = currentActivities.get(0).getIncomingTransitions();
		List<String> activityIdList = Lists.newArrayList();
		findLastActivityid(ptList, currentActivities.size(), currentActivities.get(0), activityIdList);
		for (ActivityImpl targetAciIn : targetActivities) {
			for (String s : activityIdList) {
				if (StringUtils.equals(targetAciIn.getId(), s)) {
					return targetAciIn;
				}
			}

		}
		return null;
	}

	/**
	 * 查找上一个userTask 的ActivityId
	 *
	 * @param ptList
	 * @param currentTaskNum
	 * @return
	 */
	private void findLastActivityid(List<PvmTransition> ptList, Integer currentTaskNum, ActivityImpl currentActivity, List<String> activityIdList) {
		for (PvmTransition pt : ptList) {
			PvmActivity source = pt.getSource();
			String type = source.getProperty("type").toString();
			if (StringUtils.equals("userTask", type)) {
				activityIdList.add(source.getId());
			} else if (StringUtils.equals("inclusiveGateway", type)) {
				int concurrentNum = source.getOutgoingTransitions().size();
				if (source.getOutgoingTransitions().size() == currentTaskNum) {
					findLastActivityid(source.getIncomingTransitions(), currentTaskNum, currentActivity, activityIdList);
				} else {
					List<PvmTransition> outgoingTransitions = source.getOutgoingTransitions();
					for (PvmTransition outTr : outgoingTransitions) {
						if (!StringUtils.equals(currentActivity.getId(), outTr.getDestination().getId())) {
							activityIdList.add(outTr.getDestination().getId());
						}
					}
				}
			} else if (StringUtils.equals("exclusiveGateway", type)) {
				findLastActivityid(source.getIncomingTransitions(), currentTaskNum, currentActivity, activityIdList);
			}
		}
	}

	/**
	 * 执行撤回的主要方法
	 *
	 * @param targetTask
	 * @param currentTask
	 * @param userCommonName
	 * @param targetActivity
	 * @param gatewayNum 有支路则是所有支路数量的综合否则为1
	 */
	public void revokeExecution(HistoricTaskInstanceEntity targetTask,
	                            List<Task> currentTask,
	                            String userCommonName,
	                            ActivityImpl targetActivity,
	                            int gatewayNum) {
		((TaskServiceImpl) taskService).getCommandExecutor().execute(
				(Command<Object>) commandContext -> {
					//销毁当前task
					if (currentTask.size() == gatewayNum) {
						//销毁多个execution
						ExecutionEntity parentExecution = null;
						for (Task taskEntity : currentTask) {
							ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(taskEntity.getExecutionId());
							log.info(execution.toString());
							// execution.destroyScope("任务已被撤回,操作人：" + userCommonName);  //任务已被撤回   这几个字被流程历史使用
							if (StringUtils.isNotBlank(execution.getParentId())) {
								parentExecution = commandContext
										.getExecutionManager()
										.findExecutionById(execution.getParentId());
								execution.remove();
							}
						}
						if (parentExecution == null) {
							//程序只有一个节点跳转至重新申请的时候
							if (targetTask == null) {
								parentExecution = commandContext
										.getExecutionManager()
										.findExecutionById(currentTask.get(0).getExecutionId());
							} else {
								parentExecution = commandContext
										.getExecutionManager()
										.findExecutionById(targetTask.getExecutionId());
							}
						}
						//当撤回是从主路跳转至支路的时候，重新创建一个execution
						if (parentExecution == null) {
							ExecutionEntity currentExecution = commandContext
									.getExecutionManager()
									.findExecutionById(currentTask.get(0).getExecutionId());

							parentExecution = backToMainBranch(currentExecution, targetActivity);
						}
						log.info("parent execution is :{}", parentExecution.getId());
						parentExecution.executeActivity(targetActivity);
						parentExecution.setActive(true);
						return parentExecution;
					} else if (currentTask.size() < gatewayNum) {
						//不销毁当前task
						//通过原来的execution生成一个新的execution
						ExecutionEntity executionEntity = commandContext
								.getExecutionManager()
								.findExecutionById(targetTask.getExecutionId());
						executionEntity.executeActivity(targetActivity);
						executionEntity.setActive(true);
						return executionEntity;
					}
					return null;
				});
	}

	/**
	 * 由主路撤回至支路,通过主路的execution创建一个新的子execution
	 * @param currentExecution
	 * @param targetActivity
	 * @return
	 */
	private ExecutionEntity backToMainBranch(ExecutionEntity currentExecution, ActivityImpl targetActivity) {

		ExecutionEntity branchExecution = currentExecution.createExecution();
		branchExecution.setConcurrent(true);
		branchExecution.setScope(false);
		List<PvmTransition> outgoingTransitions = targetActivity.getOutgoingTransitions();
		List<PvmTransition> incomingTransitions = targetActivity.getIncomingTransitions();

		//设置inclusiveGateway1
		// currentExecution.setActivity(findIncomeInclusiveGateWay(incomingTransitions));
		currentExecution.setActive(false);
		currentExecution.setScope(true);
		currentExecution.setConcurrent(false);

		//设置inclusiveGateway2
		ExecutionEntity execution = currentExecution.createExecution();
		// execution.setActivity(findOutgoingInclusiveGateWay(outgoingTransitions));
		execution.setActive(false);
		execution.setConcurrent(true);
		execution.setScope(false);
		return branchExecution;

	}
}
