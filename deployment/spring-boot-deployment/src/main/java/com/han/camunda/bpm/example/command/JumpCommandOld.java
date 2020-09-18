// package com.han.camunda.bpm.example.command;
//
// import org.camunda.bpm.engine.impl.interceptor.CommandContext;
// import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
// import org.camunda.bpm.model.bpmn.instance.FlowNode;
// import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
//
// import java.util.List;
//
// /**
//  * @Description 根据提供节点和执行对象id，进行跳转命令
//  * @Date 2020/9/18 13:52
//  * @Author hanyf
//  */
// public class JumpCommandOld {
// 	private FlowNode flowElement;
// 	private String executionId;
//
// 	public JumpCommandOld(FlowNode flowElement, String executionId){
// 		this.flowElement = flowElement;
// 		this.executionId = executionId;
// 	}
//
//
// 	@Override
// 	public Void execute(CommandContext commandContext){
// 		//获取目标节点的来源连线
// 		List<SequenceFlow> flows = flowElement.getIncomingFlows();
// 		if(flows==null || flows.size()<1){
// 			throw new RuntimeException("回退错误，目标节点没有来源连线");
// 		}
// 		//随便选一条连线来执行，时当前执行计划为，从连线流转到目标节点，实现跳转
// 		ExecutionEntity executionEntity = commandContext.getExecutionEntityManager().findById(executionId);
// 		executionEntity.setCurrentFlowElement(flows.get(0));
// 		commandContext.getAgenda().planTakeOutgoingSequenceFlowsOperation(executionEntity, true);
// 		return null;
// 	}
// }
