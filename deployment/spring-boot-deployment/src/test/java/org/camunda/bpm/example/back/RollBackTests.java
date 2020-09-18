package org.camunda.bpm.example.back;

import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.RepositoryServiceImpl;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.PvmTransition;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.camunda.bpm.engine.impl.pvm.process.TransitionImpl;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.impl.VariableMapImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description 撤回
 * @Date 2020/9/15 10:57
 * @Author hanyf
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RollBackTests {

	private final String key = "groupTaskDelegate";//key值
	private final int version = 1;//版本号

	//Service接口的父类，可以直接获取下边的Service
	@Autowired
	private ProcessEngine processEngine;
	//Activiti的七大Service类
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private HistoryService historyService;
	@Autowired
	private ManagementService managementService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private IdentityService identityService;
	@Autowired
	private FormService formService;

	// @Before(value = "rollBackTests")
	// public void init() {
	// 	// ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext-activiti.xml");
	// 	// repositoryService = (RepositoryService) context.getBean("repositoryService");
	// 	// runtimeService = (RuntimeService) context.getBean("runtimeService");
	// 	// historyService = (HistoryService) context.getBean("historyService");
	// 	// managementService = (ManagementService) context.getBean("managementService");
	// 	// identityService = (IdentityService) context.getBean("identityService");
	// 	// formService = (FormService) context.getBean("formService");
	//
	// 	processEngine = ProcessEngines.getDefaultProcessEngine();
	// 	repositoryService = processEngine.getRepositoryService();
	// 	runtimeService = processEngine.getRuntimeService();
	// 	historyService = processEngine.getHistoryService();
	// 	managementService = processEngine.getManagementService();
	// 	taskService = processEngine.getTaskService();
	// 	identityService = processEngine.getIdentityService();
	// 	formService = processEngine.getFormService();
	// 	System.out.println("========== 初始化完成 ==========");
	// }

	@Test
	public void test(){
		rollBackToAssignWoekFlow("9228090476101633","Activity_0dncrlb");
	}

	/**
	 * 撤回
	 * @param processInstanceId
	 * @param destTaskkey
	 */
	public void rollBackToAssignWoekFlow(String processInstanceId,String destTaskkey){
		// 取得当前任务.当前任务节点
		destTaskkey ="Activity_0dncrlb";
		// HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
		VariableMapImpl variables;
		ExecutionEntity entity = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(processInstanceId).singleResult();
		ProcessDefinitionEntity definition = (ProcessDefinitionEntity)((RepositoryServiceImpl)repositoryService)
				.getDeployedProcessDefinition(entity.getProcessDefinitionId());
		// variables = entity.getProcessVariables();
		variables = entity.getVariables();
		//当前活动环节
		ActivityImpl currActivityImpl = definition.findActivity(entity.getActivityId());
		//目标活动节点
		ActivityImpl nextActivityImpl = ((ProcessDefinitionImpl) definition).findActivity(destTaskkey);
		if(currActivityImpl !=null){
			//所有的出口集合
			List<PvmTransition> pvmTransitions = currActivityImpl.getOutgoingTransitions();
			List<PvmTransition> oriPvmTransitions = new ArrayList<PvmTransition>();
			for(PvmTransition transition : pvmTransitions){
				oriPvmTransitions.add(transition);
			}
			//清除所有出口
			pvmTransitions.clear();
			//建立新的出口
			List<TransitionImpl> transitionImpls = new ArrayList<TransitionImpl>();
			TransitionImpl tImpl = currActivityImpl.createOutgoingTransition();
			tImpl.setDestination(nextActivityImpl);
			transitionImpls.add(tImpl);

			List<Task> list = taskService.createTaskQuery().processInstanceId(entity.getProcessInstanceId())
					.taskDefinitionKey(entity.getActivityId()).list();
			for(Task task:list){
				taskService.complete(task.getId(), variables);
				historyService.deleteHistoricTaskInstance(task.getId());
			}

			for(TransitionImpl transitionImpl:transitionImpls){
				currActivityImpl.getOutgoingTransitions().remove(transitionImpl);
			}

			for(PvmTransition pvmTransition:oriPvmTransitions){
				pvmTransitions.add(pvmTransition);
			}
		}
	}


	// @Test
	// public void test(){
	// 	rollBackToAssignWoekFlow("5001","usertask2");
	// }
	//
	// /**
	//  * 撤回
	//  * @param processInstanceId
	//  * @param destTaskkey
	//  */
	// public void rollBackToAssignWoekFlow(String processInstanceId,String destTaskkey){
	// 	// 取得当前任务.当前任务节点
	// 	destTaskkey ="usertask2";
	// 	// HistoricTaskInstance currTask = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
	// 	Map<String, Object> variables;
	// 	ExecutionEntity entity = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(processInstanceId).singleResult();
	// 	ProcessDefinitionEntity definition = (ProcessDefinitionEntity)((RepositoryServiceImpl)repositoryService)
	// 			.getDeployedProcessDefinition(entity.getProcessDefinitionId());
	// 	variables = entity.getProcessVariables();
	// 	//当前活动环节
	// 	ActivityImpl currActivityImpl = definition.findActivity(entity.getActivityId());
	// 	//目标活动节点
	// 	ActivityImpl nextActivityImpl = ((ProcessDefinitionImpl) definition).findActivity(destTaskkey);
	// 	if(currActivityImpl !=null){
	// 		//所有的出口集合
	// 		List<PvmTransition> pvmTransitions = currActivityImpl.getOutgoingTransitions();
	// 		List<PvmTransition> oriPvmTransitions = new ArrayList<PvmTransition>();
	// 		for(PvmTransition transition : pvmTransitions){
	// 			oriPvmTransitions.add(transition);
	// 		}
	// 		//清除所有出口
	// 		pvmTransitions.clear();
	// 		//建立新的出口
	// 		List<TransitionImpl> transitionImpls = new ArrayList<TransitionImpl>();
	// 		TransitionImpl tImpl = currActivityImpl.createOutgoingTransition();
	// 		tImpl.setDestination(nextActivityImpl);
	// 		transitionImpls.add(tImpl);
	//
	// 		List<Task> list = taskService.createTaskQuery().processInstanceId(entity.getProcessInstanceId())
	// 				.taskDefinitionKey(entity.getActivityId()).list();
	// 		for(Task task:list){
	// 			taskService.complete(task.getId(), variables);
	// 			historyService.deleteHistoricTaskInstance(task.getId());
	// 		}
	//
	// 		for(TransitionImpl transitionImpl:transitionImpls){
	// 			currActivityImpl.getOutgoingTransitions().remove(transitionImpl);
	// 		}
	//
	// 		for(PvmTransition pvmTransition:oriPvmTransitions){
	// 			pvmTransitions.add(pvmTransition);
	// 		}
	// 	}
	// }

}
