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
public class StartTest {

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
	 * 发布流程,测试定时器开始事件
	 */
	@Test
	public void deploymentProcesses_zip(){
		Deployment deploy = repositoryService.createDeployment()
				.name("测试-定时器开始事件-1")//创建流程名称
				.addClasspathResource("processes/start.bpmn")//指定zip完成部署
				.deploy();
		System.out.println("部署id:"+deploy.getId());
		System.out.println("部署名称:"+deploy.getName());
		//睡一会
		try {
			Thread.sleep(1000*30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
