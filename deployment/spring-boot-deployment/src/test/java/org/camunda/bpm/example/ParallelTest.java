package org.camunda.bpm.example;

import com.han.camunda.bpm.example.CamundaSpringBootExampleHanApplication;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 信号边界事件
 * 项目经理审批→（主管审批→撤回操作）→上级领导审批（不做审批）→
 * （项目经理审批→提交，上级领导审批）→（上级领导审批，上级领导审批，主管审批））
 * @Date 2020/9/16 16:26
 * @Author hanyf
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CamundaSpringBootExampleHanApplication.class)
public class ParallelTest {

	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private TaskService taskService;
	@Resource
	private HistoryService historyService;

	@Resource
	private ManagementService managementService;
	/**
	 * 部署流程,测试征询
	 */
	@Test
	public void repositoryDeploy(){
		Deployment deploy = repositoryService.createDeployment()
				.addClasspathResource("processes/parallel.bpmn")
				.name("测试并行流程回退-1")
				.deploy();
		System.out.println("部署ID:"+deploy.getId());
		System.out.println("部署名称"+deploy.getName());
	}

	/**
	 * 发布流程
	 */
	@Test
	public void runtimeRelease(){

		ProcessInstance pi = runtimeService.startProcessInstanceByKey("parallel");
		System.out.println("流程实例ID:"+pi.getId());
		System.out.println("流程定义ID:"+pi.getProcessDefinitionId());
	}

	/**
	 * 查询及完成任务
	 */
	@Test
	public void taskQueryComplete(){
		List<Task> list = taskService.createTaskQuery()
				.taskAssignee("user1")
				.list();
		for (Task task : list) {
			System.out.println("--------------------------------------------");
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
			System.out.println("--------------------------------------------");
			taskService.complete(task.getId());
		}
	}

	/**
	 * 流程回退
	 */
	// @Test
	// public void taskBack(){
	// 	Task task = taskService.createTaskQuery()
	// 			.taskAssignee("user4")
	// 			.singleResult();
	//
	// 	Process process = repositoryService.getBpmnModel(task.getProcessDefinitionId()).getMainProcess();
	//
	// 	FlowNode flowNode = (FlowNode) process.getFlowElement("_3");
	//
	// 	String s = managementService.executeCommand(new DeleteTaskCommand(task.getId()));
	//
	// 	managementService.executeCommand(new JumpCommand(flowNode,s));
	//
	// }


	/**
	 * 根据流程实例id获取上一个节点的信息
	 */
	@Test
	public void queryUpOneNode(){
		Task task = taskService.createTaskQuery()
				.taskAssignee("user4")
				.singleResult();
		List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
				.processInstanceId(task.getProcessInstanceId())
				.orderByHistoricTaskInstanceEndTime()
				.desc()
				.list();

		HistoricTaskInstance taskInstance = null;

		if(!list.isEmpty()){
			if(list.get(0).getEndTime()!=null){
				taskInstance=list.get(0);
			}
		}
		System.out.println(list.get(0).getEndTime());
		System.out.println(taskInstance.getTaskDefinitionKey()+"==="+taskInstance.getName());
	}

	/**
	 * 流程回退
	 */
// 	@Test
// 	public void taskBack(){
// 		Task task = taskService.createTaskQuery()
// 				.taskAssignee("user5")
// 				.singleResult();
//
// 		List<Task> list = taskService.createTaskQuery()
// 				.processInstanceId(task.getProcessInstanceId())
// 				.list();
// 		Process process = repositoryService.getBpmnModel(task.getProcessDefinitionId()).getMainProcess();
//
// 		//通过项目经理审批节点id获取到流程
// 		FlowNode flowNode = (FlowNode) process.getFlowElement("_3");
// 		//删除当前所有任务
// 		String s = null;
// 		for (Task task1 : list) {
// 			DeleteTaskCommand deleteTaskCommand = new DeleteTaskCommand(task1.getId());
// 			s = managementService.executeCommand(deleteTaskCommand);
// 		}
// //        String s = managementService.executeCommand(new DeleteTaskCommand(task.getProcessInstanceId()));
// //        managementService.executeCommand()
// 		//跳转到项目经理审批节点
// 		managementService.executeCommand(new JumpCommand(flowNode,s));
// 	}

}
