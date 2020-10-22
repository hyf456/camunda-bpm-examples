package com.han.camunda.bpm.example.reject;

import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionManager;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.TransitionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * 任务驳回方法支持
 *
 */
public class RejectTaskCMD implements Command<Object>, Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(RejectTaskCMD.class);
	/**
	 * 历史信息中的当前任务实例
	 */
	private HistoricTaskInstance currentTaskInstance;
	/**
	 * 历史信息中的目标任务实例
	 */
	private HistoricTaskInstance destinationTaskInstance;
	/**
	 * 目标任务节点
	 */
	private ActivityImpl destinationActivity;

	/**
	 * 构造方法
	 *
	 * @param currentTaskInstance  当前任务实例
	 * @param destinationTaskInstance 目标任务实例
	 * @param destinationActivity  目标节点
	 */
	public RejectTaskCMD(HistoricTaskInstance currentTaskInstance
			, HistoricTaskInstance destinationTaskInstance
			, ActivityImpl destinationActivity) {
		this.currentTaskInstance = currentTaskInstance;
		this.destinationTaskInstance = destinationTaskInstance;
		this.destinationActivity = destinationActivity;
	}

	@Override
	public Object execute(CommandContext commandContext) {
		// 流程实例ID
		String processInstanceId = destinationTaskInstance.getProcessInstanceId();
		// 执行管理器
		ExecutionManager executionEntityManager =
				commandContext.getExecutionManager();
		// select * from ACT_RU_EXECUTION where ID_ = ? 查询当前流程实例中正在执行的唯一任务 --追源码时发现这个方法的作用，就记录了下来，省的自己遗忘掉
		ExecutionEntity executionEntity = executionEntityManager.findExecutionById(processInstanceId);
		// 当前活跃的节点信息
		ActivityImpl currentActivity = executionEntity.getActivity();
		// 创建一个出口转向
		TransitionImpl outgoingTransition = currentActivity.createOutgoingTransition();
		// 封装目标节点到转向实体
		outgoingTransition.setDestination(destinationActivity);
		// 流程转向
		executionEntity.setTransition(outgoingTransition);
		return null;
	}
}