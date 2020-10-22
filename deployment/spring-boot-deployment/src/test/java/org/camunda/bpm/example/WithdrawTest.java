package org.camunda.bpm.example;

import com.han.camunda.bpm.example.CamundaSpringBootExampleHanApplication;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.RepositoryServiceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.PvmTransition;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.TransitionImpl;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.impl.VariableMapImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 流程撤回
 * 1、获取当前任务所在的节点
 * 2、获取所在节点的流出方向
 * 3、记录所在节点的流出方向，并将所在节点的流出方向清空
 * 4、获取目标节点
 * 5、创建新的方向
 * 6、将新的方向set到所在节点的流出方向
 * 7、完成当前任务
 * 8、还原所在节点的流出方向
 * @Date 2020/9/16 16:26
 * @Author hanyf
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CamundaSpringBootExampleHanApplication.class)
public class WithdrawTest {

	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private TaskService taskService;
	@Resource
	private HistoryService historyService;

	/**
	 * 部署流程,测试撤回
	 */
	@Test
	public void repositoryDeploy() {
		Deployment deploy = repositoryService.createDeployment()
				.addClasspathResource("processes/withdraw.bpmn")
				.name("测试撤回-1")
				.deploy();
		System.out.println("部署ID:" + deploy.getId());
		System.out.println("部署名称" + deploy.getName());
	}

	/**
	 * 发布流程
	 */
	@Test
	public void runtimeRelease() {

		ProcessInstance pi = runtimeService.startProcessInstanceByKey("withdraw");
		System.out.println("流程实例ID:" + pi.getId());
		System.out.println("流程定义ID:" + pi.getProcessDefinitionId());
	}

	/**
	 * 查询及完成任务
	 */
	@Test
	public void taskQueryComplete() {
		List<Task> list = taskService.createTaskQuery()
				// .processInstanceBusinessKey("withdraw")
				.taskAssignee("zhangsan")
				// .taskAssignee("lisi")
				// .taskAssignee("zhaowu")
				.list();
		for (Task task : list) {
			System.out.println("--------------------------------------------");
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
			System.out.println("任务定义Key:" + task.getTaskDefinitionKey());
			System.out.println("--------------------------------------------");
			// taskService.complete(task.getId());
		}
	}

	/**
	 * 查询已完成的任务
	 */
	@Test
	public void taskQuerySuccess() {
		List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
				.executionId("10811")
				// .taskDeleteReason("completed")
				.list();
		for (HistoricTaskInstance historicTaskInstance : list) {
			System.out.println("taskKey:" + historicTaskInstance.getTaskDefinitionKey());
			System.out.println(historicTaskInstance);
		}

	}

	@Test
	public void test() {
		rollBackToAssignWorkFlow("15311", "_3");
	}

	/**
	 * 撤回
	 *  * 1、获取当前任务所在的节点
	 *  * 2、获取所在节点的流出方向
	 *  * 3、记录所在节点的流出方向，并将所在节点的流出方向清空
	 *  * 4、获取目标节点
	 *  * 5、创建新的方向
	 *  * 6、将新的方向set到所在节点的流出方向
	 *  * 7、完成当前任务
	 *  * 8、还原所在节点的流出方向
	 * @param processInstanceId
	 * @param destTaskKey
	 */
	public void rollBackToAssignWorkFlow(String processInstanceId, String destTaskKey) {
		// 取得当前任务.当前任务节点
		// HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
		VariableMapImpl variables;
		ExecutionEntity entity = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(processInstanceId).singleResult();
		ProcessDefinitionEntity definition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
				.getDeployedProcessDefinition(entity.getProcessDefinitionId());
		variables = entity.getVariables();
		//当前活动环节
		ActivityImpl currActivityImpl = definition.findActivity(entity.getActivityId());
		//目标活动节点
		ActivityImpl nextActivityImpl = definition.findActivity(destTaskKey);
		if (currActivityImpl != null) {
			//所有的出口集合 获取所在节点的流出方向
			List<PvmTransition> pvmTransitions = currActivityImpl.getOutgoingTransitions();
			List<PvmTransition> oriPvmTransitions = new ArrayList<PvmTransition>();
			for (PvmTransition transition : pvmTransitions) {
				oriPvmTransitions.add(transition);
			}
			//清除所有出口
			pvmTransitions.clear();
			//建立新的出口
			List<TransitionImpl> transitionImpls = new ArrayList<TransitionImpl>();
			TransitionImpl tImpl = currActivityImpl.createOutgoingTransition();
			tImpl.setDestination(nextActivityImpl);
			transitionImpls.add(tImpl);
			List<Task> list = taskService
					.createTaskQuery()
					.processInstanceId(entity.getProcessInstanceId())
					.taskDefinitionKey(entity.getActivityId()).list();
			for (Task task : list) {
				taskService.complete(task.getId(), variables);
				historyService.deleteHistoricTaskInstance(task.getId());
			}
			for (TransitionImpl transitionImpl : transitionImpls) {
				currActivityImpl.getOutgoingTransitions().remove(transitionImpl);
			}
			for (PvmTransition pvmTransition : oriPvmTransitions) {
				pvmTransitions.add(pvmTransition);
			}
		}
	}


	/**
	 *撤回上一个节点
	 */
	// @Test
	// public void taskQueryComplete2() throws Exception{
	// 	//当前任务
	// 	Task task = taskService.createTaskQuery()
	// 			.taskAssignee("lisi")
	// 			.singleResult();
	// 	if(task==null) {
	// 		throw new RuntimeException("流程未启动或已执行完成，无法撤回");
	// 	}
	// 	List<HistoricTaskInstance> htlist = historyService.createHistoricTaskInstanceQuery()
	// 			.processDefinitionId(task.getProcessDefinitionId())
	// 			.list();
	//
	// 	String myTaskId = null;
	// 	HistoricTaskInstance myTask = null;
	//
	// 	for (HistoricTaskInstance hti : htlist) {
	// 		//回退到zhangsan也就是任务A,业务中这里就是当前登录的用户名
	// 		if(hti.getAssignee().equals("zhangsan")){
	// 			myTaskId=hti.getId();
	// 			myTask=hti;
	// 			break;
	// 		}
	// 	}
	// 	if(myTask==null){
	// 		throw new RuntimeException("该任务非当前用户提交，无法撤回");
	// 	}
	// 	String processDefinitionId = myTask.getProcessDefinitionId();
	// 	BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
	// 	String myActivityId = null;
	// 	List<HistoricActivityInstance> haiList =
	// 			historyService
	// 					.createHistoricActivityInstanceQuery()
	// 					.executionId(myTask.getExecutionId())
	// 					.finished()
	// 					.list();
	//
	// 	for (HistoricActivityInstance hai : haiList) {
	// 		if(myTaskId.equals(hai.getTaskId())) {
	// 			myActivityId = hai.getActivityId();
	// 			break;
	// 		}
	// 	}
	// 	FlowNode myFlowNode =
	// 			(FlowNode) bpmnModel.getMainProcess().getFlowElement(myActivityId);
	// 	Execution execution = runtimeService.createExecutionQuery()
	// 			.executionId(task.getExecutionId()).singleResult();
	// 	String activityId = execution.getActivityId();
	// 	System.out.println(activityId);
	// 	FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess()
	// 			.getFlowElement(activityId);
	// 	//记录原活动方向
	// 	List<SequenceFlow> oriSequenceFlows = new ArrayList<SequenceFlow>();
	// 	oriSequenceFlows.addAll(flowNode.getOutgoingFlows());
	// 	//清理活动方向
	// 	flowNode.getOutgoingFlows().clear();
	// 	//建立新方向
	// 	List<SequenceFlow> newSequenceFlowList = new ArrayList<SequenceFlow>();
	// 	SequenceFlow newSequenceFlow = new SequenceFlow();
	// 	newSequenceFlow.setId("newSequenceFlowId");
	// 	newSequenceFlow.setSourceFlowElement(flowNode);
	// 	newSequenceFlow.setTargetFlowElement(myFlowNode);
	// 	newSequenceFlowList.add(newSequenceFlow);
	// 	flowNode.setOutgoingFlows(newSequenceFlowList);
	// 	Authentication.setAuthenticatedUserId("zhangsan");
	// 	taskService.addComment(task.getId(), task.getProcessInstanceId(), "撤回");
	// 	//完成任务
	// 	taskService.complete(task.getId());
	// 	//恢复原方向
	// 	flowNode.setOutgoingFlows(oriSequenceFlows);
	// }
}
