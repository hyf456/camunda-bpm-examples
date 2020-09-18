package org.camunda.bpm.example;

import com.han.camunda.bpm.example.CamundaSpringBootExampleHanApplication;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description 信号边界事件
 * 征询是指，你对当前的流程不是很清楚，你需要将流程转发给另外一个人需要另外一个人给你指导，另外一个人审批完后流程回到你这里，你根据他的审批意见进行审批。简单说，如果正常的审批流程为A->B->C，如果B执行了征询操作，那么流程就变为了A->B-->D-->B->C，征询和加签的区别就在于，征询会回到发起征询的节点，加签不会。
 * 征询就是两次的重新分配：
 * B重新分配给D
 * D重新分配给B
 * 两次的reassign操作就可以实现征询，要解决的问题就是怎么协调好这两次重新分配。
 * B执行征询操作时，通过变量标记征询动作，并将征询人B的信息（主要是账号）保存在变量中
 * D执行回复操作时（流程变为征询后，被征询人只有回复的权限），从变量中取出B的账号，重新将流程分配给B
 * B执行正常的审批操作，流程正常流转。
 * @Date 2020/9/16 16:26
 * @Author hanyf
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CamundaSpringBootExampleHanApplication.class)
public class ConsultationTest {

	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private TaskService taskService;
	@Resource
	private HistoryService historyService;

	/**
	 * 部署流程,测试征询
	 */
	@Test
	public void repositoryDeploy(){
		Deployment deploy = repositoryService.createDeployment()
				.addClasspathResource("processes/activiti_Consultation.bpmn")
				.addClasspathResource("processes/activiti_Consultation.png")
				.name("测试征询-2")
				.deploy();
		System.out.println("部署ID:"+deploy.getId());
		System.out.println("部署名称"+deploy.getName());
	}

	/**
	 * 发布流程
	 */
	@Test
	public void runtimeRelease(){
		ProcessInstance pi = runtimeService.startProcessInstanceByKey("consultation");
		System.out.println("流程实例ID:"+pi.getId());
		System.out.println("流程定义ID:"+pi.getProcessDefinitionId());
	}

	/**
	 * 查询及完成任务
	 */
	@Test
	public void taskQueryComplete(){
		List<Task> list = taskService.createTaskQuery()
				.taskAssignee("A")
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
	 *征询
	 */
	@Test
	public void taskQueryComplete2(){

		Task task = taskService.createTaskQuery()
				.taskAssignee("B")
				.singleResult();
		taskService.setAssignee(task.getId(),"D");

		//对被征询者做出记录
		taskService.setVariableLocal(task.getId(),"status","D");
		//对发出征询者做出记录
		taskService.setVariableLocal(task.getId(),"originUser","B");
	}

	/**
	 * D执行回复操作
	 */
	@Test
	public void reply(){
		String originUser = (String)
				taskService.getVariableLocal("5002", "originUser");
		System.out.println(originUser);
		taskService.setAssignee("5002",originUser);
		taskService.removeVariable("5002", "status");
		taskService.removeVariable("5002", "originUser");
	}
}
