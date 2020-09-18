package org.camunda.bpm.example;

import com.han.camunda.bpm.example.CamundaSpringBootExampleHanApplication;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description 信号边界事件
 * 部署流程→启动流程→填写工单→审批→发送信号→触发信号边界事件→更改工单
 * 部署流程→启动流程→填写工单→审批→发送信号→触发信号边界事件→更改工单（完成）→填写工单
 * @Date 2020/9/16 16:26
 * @Author hanyf
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CamundaSpringBootExampleHanApplication.class)
public class SubprocessTest {

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private IdentityService identityService;

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	private ProcessEngine processEngine;

	@Autowired
	private HistoryService historyService;


	/**
	 * 发布流程,测试调用式子流程
	 */
	@Test
	public void deploymentProcesses_zip(){
		Deployment deploy = repositoryService.createDeployment()
				.name("测试-调用式子流程-1")//创建流程名称
				.addClasspathResource("processes/s.bpmn")//主流程
				.addClasspathResource("processes/subprocess2.bpmn")//子流程
				.deploy();
		System.out.println("部署id:"+deploy.getId());
		System.out.println("部署名称:"+deploy.getName());

	}

	/**
	 * 启动流程，测试调用式子流程
	 */
	@Test
	public void startProcess() {
		//可根据id,key,message启动流程
		ProcessInstance pi = runtimeService.startProcessInstanceByKey("s");
		System.out.println("流程实例id:" + pi.getId());
		System.out.println("流程定义id:" + pi.getProcessDefinitionId());
		System.out.println("=========================================================");

		//查询任务
		List<Task> taskList = taskService.createTaskQuery() // 创建任务查询
				.taskAssignee("user1") // 指定某个人
				.list();
		for (Task task : taskList) {
			System.out.println("--------------------------------------------");
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());

			// 参数
			Map<String, Object> vars = new HashMap<String, Object>();
			vars.put("days", 10);
			// 完成任务
			taskService.complete(task.getId(), vars);
		}

		// 主流程中查询参数
		Integer days = (Integer) runtimeService.getVariable(pi.getId(), "days");
		System.out.println("主流程中参数days:" + days);

		// 调用式子流程中查询参数
		ProcessInstance subPi = runtimeService.createProcessInstanceQuery()
				.processDefinitionKey("subprocess2")
				.singleResult();
		Integer subDays = (Integer) runtimeService.getVariable(subPi.getId(), "subDays");
		System.out.println("子流程中参数subDays:" + subDays);

		// 设置子流程参数
		runtimeService.setVariable(subPi.getId(), "subDays", subDays -1);

		try {
			Thread.sleep(1000 * 3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// 查询任务
		taskList = taskService.createTaskQuery() // 创建任务查询
				.taskAssignee("user3") // 指定某个人
				.list();
		for (Task task : taskList) {
			System.out.println("--------------------------------------------");
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());

			// 完成任务
			taskService.complete(task.getId());
		}


		//查询任务
		taskList = taskService.createTaskQuery() // 创建任务查询
				.taskAssignee("user2") // 指定某个人
				.list();
		for (Task task : taskList) {
			System.out.println("--------------------------------------------");
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
		}

		System.out.println("--------------------------------------------");
		// 主流程中查询参数
		Integer mainDays = (Integer) runtimeService.getVariable(pi.getId(), "mainDays");
		System.out.println("主流程中参数mainDays:" + mainDays);

	}
}
