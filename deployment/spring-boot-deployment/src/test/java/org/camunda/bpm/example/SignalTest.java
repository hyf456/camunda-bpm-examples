package org.camunda.bpm.example;

import com.han.camunda.bpm.example.CamundaSpringBootExampleHanApplication;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * @Description 信号边界事件
 * 部署流程→启动流程→填写工单→审批→发送信号→触发信号边界事件→更改工单
 * 部署流程→启动流程→填写工单→审批→发送信号→触发信号边界事件→更改工单（完成）→填写工单
 * @Date 2020/9/16 16:26
 * @Author hanyf
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CamundaSpringBootExampleHanApplication.class)
public class SignalTest {

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private IdentityService identityService;
	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private HistoryService historyService;


	/**
	 * 发布流程,测试信号边界事件
	 */
	@Test
	public void deploymentProcesses_zip() {
		Deployment deploy = repositoryService.createDeployment()
				.name("测试-信号边界事件-1")//创建流程名称
				.addClasspathResource("processes/signal.bpmn")//指定zip完成部署
				.deploy();
		System.out.println("部署id:" + deploy.getId());
		System.out.println("部署名称:" + deploy.getName());
	}

	/**
	 * 启动流程，测试信号边界事件
	 */
	@Test
	public void startProcess() throws InterruptedException {
		//可根据id,key,message启动流程
		ProcessInstance parallel = runtimeService.startProcessInstanceByKey("signal");
		System.out.println("流程实例id:" + parallel.getId());
		System.out.println("流程定义id:" + parallel.getProcessDefinitionId());
		//睡一会
		Thread.sleep(1000 * 10);
		System.out.println("========================================================");
		List<Task> list = taskService.createTaskQuery().taskAssignee("user1").list();
		for (Task task : list) {
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
			//完成任务
			taskService.complete(task.getId());
		}


		// 查询任务
		List<Task> list1 = taskService.createTaskQuery() // 创建任务查询
				.taskAssignee("user2") // 指定某个人
				.list();
		for (Task task : list1) {
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
		}
		//睡一会
		Thread.sleep(1000 * 10);
		System.out.println("========================================================");
		//发送信号
		runtimeService.signalEventReceived("changeSignal");
		//睡一会
		Thread.sleep(1000 * 10);
		// 查询任务
		List<Task> list2 = taskService.createTaskQuery() // 创建任务查询
				.taskAssignee("user3") // 指定某个人
				.list();
		for (Task task : list2) {
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
		}
	}

	/**
	 * 查看任务
	 */
	@Test
	public void queryTask() {
		List<Task> taskList = taskService.createTaskQuery() // 创建任务查询
				.taskAssignee("user3") // 指定某个人
				.list();
		for (Task task : taskList) {
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
		}
	}

	/**
	 * 提交任务
	 */
	@Test
	public void completeTask2() {

		String taskId = "2218";
		taskService.complete(taskId);
	}


	/**
	 * 发送信号
	 */
	@Test
	public void eventReceived() {
		// 可以查询所有订阅了特定信号事件的执行流
		List<Execution> executions = runtimeService.createExecutionQuery()
				.signalEventSubscriptionName("changeSignal")
				.list();
		// 把信号发送给全局所有订阅的处理器（广播语义）
		runtimeService.signalEventReceived("changeSignal");

		// 只把信息发送给指定流程的执行
		runtimeService.signalEventReceived("changeSignal", executions.get(0).getId());
	}


	/**
	 * 查询流程状态（判断流程走到哪一个节点）
	 */
	@Test
	public void isProcessActive() {
		// ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
		// 		.processInstanceId("2501")
		// 		.singleResult();
		//
		// if (processInstance == null) {
		// 	System.out.println("该流程已经结束");
		// } else {
		// 	System.out.println("该流程尚未结束");
		// 	//获取任务状态(该方式无法获取节点信息)
		// 	System.out.println("节点ID:" + processInstance.getId());
		// }
		HistoricActivityInstance hiv = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId("2218")
				.unfinished()
				.singleResult();
		if (hiv == null) {
			System.out.println("该流程已结束");
		} else {
			System.out.println("该流程未结束，节点id为：" + hiv.getActivityId()
					+ "  节点名称为:" + hiv.getActivityName()
					+ "  节点类型：" + hiv.getActivityType()
					+ "  节点代理人：" + hiv.getAssignee());
		}
	}

	/**
	 * 历史记录
	 */
	@Test
	public void historyActiviti() {
		List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId("2218")
				.list();

		for (HistoricActivityInstance his : list) {
			System.out.println("活动id:" + his.getActivityId()
					+ " 审批人： " + his.getAssignee()
					+ " 任务id： " + his.getTaskId());
			System.out.println("===========================");
		}
	}
}
