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
 * @Description
 *  @see com.han.camunda.bpm.example.listener.MyCompeteistener
 * @see com.han.camunda.bpm.example.listener.MutiGroupsListener
 * 驳回请假单后，继续提交任务，所有人都同意，流程结束
 * 所有审批人都不同意，流程驳回填写请假单
 * @Date 2020/9/16 16:26
 * @Author hanyf
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CamundaSpringBootExampleHanApplication.class)
public class LeaveSignTest {

	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private TaskService taskService;
	@Resource
	private HistoryService historyService;

	/**
	 * 部署流程,测试会签功能2
	 */
	@Test
	public void repositoryDeploy() {
		Deployment deploy = repositoryService.createDeployment()
				.addClasspathResource("processes/leaveSign.bpmn")
				.name("测试会签-2")
				.deploy();
		System.out.println("部署ID:" + deploy.getId());
		System.out.println("部署名称" + deploy.getName());
	}

	/**
	 * 发布流程
	 */
	@Test
	public void runtimeRelease() {
//        User user = (User) request.getServletContext().getAttribute("user");
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
	 * 查询及完成任务+驳回功能
	 */
	@Test
	public void taskQueryComplete2() {

		Task task = taskService.createTaskQuery()
				.taskAssignee("王一")
				.singleResult();
		List<Task> list = taskService.createTaskQuery()
				.taskName(task.getName())
				.processInstanceId(task.getProcessInstanceId())
				.list();
		int passCount = 0;//审批同意人数

		int noPassCount = 0;//审批不同意人数

		int totalCount = 0;//任务总人数

		String tmpPassCount = runtimeService.getVariable(task.getProcessInstanceId(),
				task.getTaskDefinitionKey() + "#passCount") + "";

		String tmpNoPassCount = runtimeService.getVariable(task.getProcessInstanceId(),
				task.getTaskDefinitionKey() + "#noPassCount") + "";

		String tmpTotal = runtimeService.getVariable(task.getProcessInstanceId(),
				task.getTaskDefinitionKey() + "#totalCount") + "";


		if (!tmpPassCount.equals("null") && !tmpPassCount.trim().equals("")) {

			passCount = Integer.parseInt(tmpPassCount);

		}


		if (!tmpNoPassCount.equals("null") && !tmpNoPassCount.trim().equals("")) {

			noPassCount = Integer.parseInt(tmpNoPassCount);

		}


		if (tmpTotal.equals("null") || tmpTotal.trim().equals("")) {

			totalCount = list.size();

		} else if (!tmpTotal.equals("null") && !tmpTotal.trim().equals("")) {

			totalCount = Integer.parseInt(tmpTotal);
		}
		String passflag = "同意";
		for (Task tmp : list) {
			if (passflag.equals("同意") && tmp.getId().equals(task.getId())) {
				passCount++;
			}
			if (passflag.equals("不同意") && tmp.getId().equals(task.getId())) {
				noPassCount++;
			}
		}

		HashMap<String, Object> map = new HashMap<>();
		map.put("passCount", passCount);
		map.put("noPassCount", noPassCount);
		map.put("totalCount", totalCount);
		map.put(task.getTaskDefinitionKey() + "#passCount", passCount);

		map.put(task.getTaskDefinitionKey() + "#noPassCount", noPassCount);

		map.put(task.getTaskDefinitionKey() + "#totalCount", totalCount);

		taskService.complete(task.getId(), map);
	}
}
