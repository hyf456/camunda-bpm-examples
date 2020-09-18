package org.camunda.bpm.example;

import com.han.camunda.bpm.example.CamundaSpringBootExampleHanApplication;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * @Description TODO
 * @Date 2020/9/18 14:15
 * @Author hanyf
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CamundaSpringBootExampleHanApplication.class)
public class QueryNodeTest {

	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private TaskService taskService;
	@Resource
	private HistoryService historyService;

	/**
	 * 根据流程实例id获取上一个节点的信息
	 */
	@Test
	public void queryUpOneNode(){
		// Task task = taskService.createTaskQuery()
		// 		.taskAssignee("lisi")
		// 		.singleResult();
		List<Task> taskList = taskService.createTaskQuery()
				.taskAssignee("lisi")
				.list();
		Task task = taskList.get(0);
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

		System.out.println(taskInstance.getTaskDefinitionKey());
	}

	/**
	 * 根据流程定义id获取当前节点的下一节点
	 */
	@Test
	public void getNexNode(){
		Task task = taskService.createTaskQuery()
				.taskAssignee("zhangsan")
				.singleResult();

		//根据流程定义id获取bpmnModel对象
		String processDefinitionId = task.getProcessDefinitionId();
		// BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
		BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);
		//获取当前节点信息
		// FlowNode flowNode = (FlowNode) bpmnModelInstance.getFlowElement(task.getTaskDefinitionKey());
		FlowNode flowNode =(FlowNode) bpmnModelInstance.getModelElementById(task.getTaskDefinitionKey());

		FlowNode myFlowNode = (FlowNode) bpmnModelInstance.getDocumentElement();
		//获取当前节点输出连线
		Collection<SequenceFlow> outgoingFlows = flowNode.getOutgoing();
		//遍历输出连线
		for (SequenceFlow outgoingFlow : outgoingFlows) {
			//获取输出节点元素
			// FlowElement targetFlowElement = outgoingFlow.getTargetFlowElement();
			FlowElement targetFlowElement = outgoingFlow.getTarget();

			//排除非用户任务接点
			if(targetFlowElement instanceof UserTask){
				//获取输出节点id==名称
				System.out.println(outgoingFlow.getTarget().getId()+"==="
						+outgoingFlow.getTarget().getName());
			}
		}
	}

}
