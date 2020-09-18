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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Description 信号边界事件
 * 会签，是指多个人员针对同一个事务进行协商处理，共同签署决定一件事情。
 * 在工作流中会签，是指多个人员在同一个环节进行处理，
 * 同一环节的有多个处理人并行处理，按照配置规则，固定比例的人员办理完成后即可继续扭转至下一环节。
 *
 * ${nrOfCompletedInstances/nrOfInstances >= 0.6}，表示需要两个会签任务完成就会往下执行。
 * @Date 2020/9/16 16:26
 * @Author hanyf
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CamundaSpringBootExampleHanApplication.class)
public class JointlySignTest2 {

	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private TaskService taskService;
	@Resource
	private HistoryService historyService;

	/**
	 * 部署流程,测试会签功能
	 */
	@Test
	public void repositoryDeploy(){
		Deployment deploy = repositoryService.createDeployment()
				.addClasspathResource("processes/jointlySign2.bpmn")
				.addClasspathResource("processes/jointlySign2.png")
				.name("测试会签功能-2")
				.deploy();
		System.out.println("部署ID:"+deploy.getId());
		System.out.println("部署名称"+deploy.getName());
	}

	/**
	 * 发布流程
	 */
	@Test
	public void runtimeRelease(){
		ArrayList<String> list = new ArrayList<>();
		list.add("tom");
		list.add("jack");
		list.add("mary");
		HashMap<String, Object> map = new HashMap<>();
		map.put("assignees",list);
		ProcessInstance pi = runtimeService.startProcessInstanceByKey("jointlySign2",map);
		System.out.println("流程实例ID:"+pi.getId());
		System.out.println("流程定义ID:"+pi.getProcessDefinitionId());
	}

	/**
	 * 查询及完成任务
	 */
	@Test
	public void taskQueryComplete(){
		List<Task> list = taskService.createTaskQuery()
				.taskAssignee("tom")
				.list();
		for (Task task : list) {
			System.out.println("--------------------------------------------");
			System.out.println("任务ID:" + task.getId());
			System.out.println("任务名称:" + task.getName());
			System.out.println("任务创建时间:" + task.getCreateTime());
			System.out.println("任务委派人:" + task.getAssignee());
			System.out.println("流程实例ID:" + task.getProcessInstanceId());
			taskService.complete(task.getId());
		}
	}
}
