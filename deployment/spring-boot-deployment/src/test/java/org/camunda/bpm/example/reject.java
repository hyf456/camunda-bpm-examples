package org.camunda.bpm.example;

import com.han.camunda.bpm.example.CamundaSpringBootExampleHanApplication;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.util.JsonUtil;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Comment;
import org.camunda.bpm.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class reject {

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
				// .taskAssignee("zhangsan")
				.taskAssignee("lisi")
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
			taskService.complete(task.getId());
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
	 */
	@Test
	public void reject() {
		String processInstanceId = "16416";
		String message = "就是要驳回";
		Task task = taskService.createTaskQuery()
				.taskAssignee("zhaowu") //当前登录用户的id
				.processInstanceId(processInstanceId)
				.singleResult();
		ActivityInstance tree = runtimeService
				.getActivityInstance(processInstanceId);
		List<HistoricActivityInstance> resultList = historyService
				.createHistoricActivityInstanceQuery()
				.processInstanceId(processInstanceId)
				.activityType("userTask")
				.finished()
				.orderByHistoricActivityInstanceEndTime()
				.asc()
				.list();
		//得到第一个任务节点的id
		HistoricActivityInstance historicActivityInstance = resultList.get(0);
		String taskId = historicActivityInstance.getTaskId();
		List<Task> taskList = taskService.createTaskQuery()
				.taskId(taskId) //当前登录用户的id
				.processInstanceId(processInstanceId)
				.list();
		String toActId = historicActivityInstance.getActivityId();
		String assignee = historicActivityInstance.getAssignee();
		//设置流程中的可变参数
		Map<String, Object> taskVariable = new HashMap<>(2);
		taskVariable.put("user", assignee);
		taskVariable.put("formName", "项目建设");
		taskService.createComment(task.getId(), processInstanceId, "驳回原因:" + message);
		runtimeService.createProcessInstanceModification(processInstanceId)
				.cancelActivityInstance(getInstanceIdForActivity(tree, task.getTaskDefinitionKey()))//关闭相关任务
				// .cancelActivityInstance(getInstanceIdForActivity(tree, taskKey))//关闭相关任务
				.setAnnotation("进行了驳回到第一个任务节点操作")
				.startBeforeActivity(toActId)//启动目标活动节点
				.setVariables(taskVariable)//流程的可变参数赋值
				.execute();
		// camunda获流程的审批记录详情示例
		//所有的审批意见都会存到 ACT_HI_COMMENT 的表中，因此需要如下接口获取
		List<Comment> taskComments = taskService.getTaskComments(task.getId());
		for (Comment taskComment : taskComments) {
			System.out.println(taskComment.toString());
		}
	}

	private String getInstanceIdForActivity(ActivityInstance activityInstance, String activityId) {
		ActivityInstance instance = getChildInstanceForActivity(activityInstance, activityId);
		if (instance != null) {
			return instance.getId();
		}
		return null;
	}

	private ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
		if (activityId.equals(activityInstance.getActivityId())) {
			return activityInstance;
		}
		for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
			ActivityInstance instance = getChildInstanceForActivity(childInstance, activityId);
			if (instance != null) {
				return instance;
			}
		}
		return null;
	}

	//完整示例
	@Test
	public void taskGetComment() {
		List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId("d66d9908-9a71-11ea-9906-40e230303674")
				.orderByHistoricActivityInstanceStartTime()
				.asc()
				.list();
		List<Map<String, Object>> result = new ArrayList<>(list.size());
		System.out.println(list.size());
		for (HistoricActivityInstance historicActivityInstance : list) {
			Map<String, Object> map = new HashMap<>(5);
			String taskId = historicActivityInstance.getTaskId();
			List<Comment> taskComments = taskService.getTaskComments(taskId);
			System.out.println(taskComments.size());
			map.put("activityName", historicActivityInstance.getActivityName());
			map.put("activityType", matching(historicActivityInstance.getActivityType()));
			map.put("assignee", historicActivityInstance.getAssignee() == null ? "无" : historicActivityInstance.getAssignee());
			map.put("startTime", DateFormatUtils.format(historicActivityInstance.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
			map.put("endTime", DateFormatUtils.format(historicActivityInstance.getEndTime(), "yyyy-MM-dd HH:mm:ss"));
			map.put("costTime", getDatePoor(historicActivityInstance.getEndTime(), historicActivityInstance.getStartTime()));

			if (taskComments.size() > 0) {
				map.put("message", taskComments.get(0).getFullMessage());
			} else {
				map.put("message", "无");
			}
			result.add(map);
		}
		// String json = JSONArray.fromObject(list).toString();
		// System.out.println(JSONObject.toJSONString());
	}

	private String matching(String ActivityType) {
		String value = "";
		switch (ActivityType) {
			case "startEvent":
				value = "流程开始";
				break;
			case "userTask":
				value = "用户处理";
				break;
			case "noneEndEvent":
				value = "流程结束";
				break;
			default:
				value = "未知节点";
				break;
		}
		return value;
	}

	public String getDatePoor(Date endDate, Date nowDate) {

		long nd = 1000 * 24 * 60 * 60;
		long nh = 1000 * 60 * 60;
		long nm = 1000 * 60;
		long ns = 1000;
		// 获得两个时间的毫秒时间差异
		long diff = endDate.getTime() - nowDate.getTime();
		// 计算差多少天
		long day = diff / nd;
		// 计算差多少小时
		long hour = diff % nd / nh;
		// 计算差多少分钟
		long min = diff % nd % nh / nm;
		// 计算差多少秒//输出结果
		long sec = diff % nd % nh % nm / ns;
		return day + "天" + hour + "小时" + min + "分钟" + sec + "秒";
	}
	//结果展示
	/** [
	 {
	 "costTime": "0天0小时0分钟0秒",
	 "activityName": "开始",
	 "startTime": "2020-05-20 16:13:50",
	 "assignee": "无",
	 "endTime": "2020-05-20 16:13:50",
	 "activityType": "流程开始",
	 "message": "无"
	 },
	 {
	 "costTime": "0天0小时1分钟2秒",
	 "activityName": "数据处理",
	 "startTime": "2020-05-20 16:13:50",
	 "assignee": "ass001",
	 "endTime": "2020-05-20 16:14:53",
	 "activityType": "用户处理",
	 "message": "无"
	 },
	 {
	 "costTime": "0天0小时1分钟25秒",
	 "activityName": "审批",
	 "startTime": "2020-05-20 16:14:53",
	 "assignee": "0000",
	 "endTime": "2020-05-20 16:16:18",
	 "activityType": "用户处理",
	 "message": "驳回原因:数据又遗漏"
	 },
	 {
	 "costTime": "0天0小时0分钟25秒",
	 "activityName": "数据处理",
	 "startTime": "2020-05-20 16:16:18",
	 "assignee": "ass001",
	 "endTime": "2020-05-20 16:16:44",
	 "activityType": "用户处理",
	 "message": "无"
	 },
	 {
	 "costTime": "0天0小时0分钟19秒",
	 "activityName": "审批",
	 "startTime": "2020-05-20 16:16:44",
	 "assignee": "0000",
	 "endTime": "2020-05-20 16:17:03",
	 "activityType": "用户处理",
	 "message": "驳回原因:数据有空值"
	 },
	 {
	 "costTime": "0天0小时0分钟5秒",
	 "activityName": "数据处理",
	 "startTime": "2020-05-20 16:17:03",
	 "assignee": "ass001",
	 "endTime": "2020-05-20 16:17:08",
	 "activityType": "用户处理",
	 "message": "无"
	 },
	 {
	 "costTime": "0天0小时0分钟19秒",
	 "activityName": "审批",
	 "startTime": "2020-05-20 16:17:08",
	 "assignee": "0000",
	 "endTime": "2020-05-20 16:17:28",
	 "activityType": "用户处理",
	 "message": "数据审批通过"
	 },
	 {
	 "costTime": "0天0小时0分钟0秒",
	 "activityName": "结束",
	 "startTime": "2020-05-20 16:17:28",
	 "assignee": "无",
	 "endTime": "2020-05-20 16:17:28",
	 "activityType": "流程结束",
	 "message": "无"
	 }
	 ] **/

}
