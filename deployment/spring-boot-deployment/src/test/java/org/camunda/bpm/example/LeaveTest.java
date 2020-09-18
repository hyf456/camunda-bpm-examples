package org.camunda.bpm.example;

import com.han.camunda.bpm.example.CamundaSpringBootExampleHanApplication;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.HashMap;
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
public class LeaveTest {

	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private TaskService taskService;
	@Resource
	private HistoryService historyService;

	/**
	 * 部署流程,
	 */
	@Test
	public void repositoryDeploy() {
		Deployment deploy = repositoryService.createDeployment()
				.addClasspathResource("processes/leave.bpmn")
				.name("测试一个角色多人审批01")
				.deploy();
		System.out.println("部署ID:" + deploy.getId());
		System.out.println("部署名称" + deploy.getName());
	}

	/**
	 * 发布流程
	 */
	@Test
	public void runtimeRelease() {
//        模拟登录用户，进行指定任务人
		HashMap<String, Object> map = new HashMap<>();
		map.put("inputUser", "jack");
		ProcessInstance pi = runtimeService.startProcessInstanceByKey("leave", map);
		System.out.println("流程实例ID:" + pi.getId());
		System.out.println("流程定义ID:" + pi.getProcessDefinitionId());
	}

	/**
	 * 查询及完成任务
	 */
	@Test
	public void taskQueryComplete() {
		//        User user = (User) request.getServletContext().getAttribute("user");
		//        模拟登录用户，获取到任务人,进行任务的查询和提交
		List<Task> list = taskService.createTaskQuery()
				.taskAssignee("jack")
				.list();
		for (Task task : list) {
			System.out.println("--------------------------------------------");
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
			//完成任务,这里测试请假3天触发一个角色多个审批人
			HashMap<String, Object> map = new HashMap<>();
			map.put("day", 3);
			taskService.complete(task.getId(), map);
		}
	}

	/**
	 * 测试通过待办任务获取到属于该任务角色id的审批人都有哪些
	 */
	@Test
	public void testMultiple() {
		//Approval approval
		//模拟获取到前台传来的待办任务的id,这里是5005
		//获取到运行时act_ru_identitylink中的数据
		List<IdentityLink> taskList = taskService.getIdentityLinksForTask("5005");

		//遍历取出数据
		for (IdentityLink identityLink : taskList) {
			System.out.println(identityLink.getGroupId() + "==审批人名称:" + identityLink.getUserId()
					+ "==对应审批角色的ID" + identityLink.getTaskId());
		}
	}

	/**
	 *为审批人拾取任务，这样才能提交任务
	 */
	@Test
	public void claimTask() {
		//上面获取到的
		String taskId = "5005";
		String userId = "王一";
		//拾取任务
		taskService.claim(taskId, userId);
		//任务拾取以后, 可以回退给组
		//taskService.setAssignee(taskId, null);
		//任务拾取以后,可以转给别人去处理(别人可以是组成员也可以不是)
		//taskService.claim(taskId, "wangliu");
	}

	/**
	 * 查询及完成任务
	 */
	@Test
	public void taskQueryComplete2() {
		List<Task> list = taskService.createTaskQuery()
				.taskAssignee("王一")
				.list();
		for (Task task : list) {
			System.out.println("--------------------------------------------");
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
			//完成任务，王一进行审批
			taskService.complete(task.getId());
		}
	}
}
