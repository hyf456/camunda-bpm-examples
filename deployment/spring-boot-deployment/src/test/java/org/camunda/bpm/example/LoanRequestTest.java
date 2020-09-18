/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.example;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.han.camunda.bpm.example.CamundaSpringBootExampleHanApplication;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CamundaSpringBootExampleHanApplication.class)
public class LoanRequestTest {

	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private HistoryService historyService;

	@Test
	public void verifyProcessInstanceStarted() {
		// process instance is started by the application and waits on the user task
		Task task = taskService.createTaskQuery().taskName("Approve Loan").singleResult();
		assertThat(task, is(notNullValue()));

		// complete the user task and verify that the process ends
		taskService.complete(task.getId());

		assertThat(runtimeService.createProcessInstanceQuery().count(), is(0L));
	}

	/**
	 * 发布流程
	 */
	@Test
	public void deploymentProcessesZip() {
		Deployment deployment = repositoryService.createDeployment()
				.name("贷款请求")
				.addClasspathResource("processes/loanRequest.bpmn")
				.deploy();
		System.out.println("部署id:" + deployment.getId());
		System.out.println("部署名称:" + deployment.getName());
		System.out.println("部署信息:" + deployment.toString());
	}

	/**
	 * 启动流程
	 */
	@Test
	public void startProcess() {
		// 可根据 id,key,message 启动流程
		ProcessInstance loanRequest = runtimeService.startProcessInstanceByKey("loanRequest");
		System.out.println("流程定义ID:" + loanRequest.getId());
		System.out.println("流程实例ID:" + loanRequest.getProcessDefinitionId());
		System.out.println(loanRequest.getCaseInstanceId());
		System.out.println(loanRequest.getBusinessKey());
		System.out.println(loanRequest.toString());
	}

	/**
	 * 查看任务
	 */
	@Test
	public void queryTask() {
		TaskQuery taskQuery = taskService.createTaskQuery()
				.taskAssignee("hanyf");
		List<Task> list = taskQuery
				.list();
		System.out.println(list.size());

		if (list != null && list.size() > 0) {
			for (Task task : list) {
				System.out.println("任务ID:" + task.getId());
				System.out.println("任务名称:" + task.getName());
				System.out.println("审批人:" + task.getAssignee());
				System.out.println("任务时间:" + task.getCreateTime());
				System.out.println("任务时间:" + task.getCreateTime().toLocaleString());

			}
		}
	}

	/**
	 * 完成任务
	 */
	@Test
	public void handleTask() {
		// 根据上一步生成的 taskId 执行任务
		String taskId = "410";
		taskService.complete(taskId);
	}

	/**
	 * 办理任务,这里是测试请假 4 天,结果会跳到部门经理和项目经理, 这两个节点
	 */
	@Test
	public void completeTask() {
		// 根据上一步生成的 taskId 执行任务
		String taskId = "406";
		HashMap<String, Object> var = new HashMap<>();
		var.put("day", 4);
		taskService.complete(taskId, var);
	}

	/**
	 * 查询流程状态 (判断流程走到哪一个节点)
	 */
	@Test
	public void isProcessActive() {
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId("701")
				.singleResult();

		if (null == processInstance) {
			System.out.println("该流程已经结束");
		} else {
			System.out.println("该流程尚未结束");
			// 获取任务状态(该方式无法获取节点信息)
			System.out.println("节点ID:" + processInstance.getId());
		}

		HistoricActivityInstance hiv = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId("701")
				.unfinished()
				.singleResult();
		if (null == hiv) {
			System.out.println("该流程已结束");
		} else {
			System.out.println("该流程未结束，节点id为:" + hiv.getActivityId()
					+ " 节点名称为:" + hiv.getActivityName()
					+ " 节点类型为:" + hiv.getActivityType());
		}
	}

	/**
	 * 历史记录
	 */
	@Test
	public void historyActivity() {
		// act_hi_taskinst
		List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId("406")
				.list();
		for (HistoricActivityInstance his : list) {
			System.out.println("活动id:" + his.getActivityId()
					+ "审批人: " + his.getAssignee()
					+ "任务id:" + his.getTaskId());
			System.out.println("=================");
		}
	}



}
