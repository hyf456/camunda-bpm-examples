package org.camunda.bpm.example.back;// package com.yestae.teaflow;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.identity.Authentication;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omg.CORBA.SystemException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description 撤回
 * @Date 2020/9/16 10:57
 * @Author hanyf
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CamundaJpaTest08 {

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
    public void repositoryDeploy(){
        Deployment deploy = repositoryService.createDeployment()
                .addClasspathResource("processes/BackOff.bpmn")
                .addClasspathResource("processes/BackOff.png")
                .name("测试撤回-1")
                .deploy();
        System.out.println("部署ID:"+deploy.getId());
        System.out.println("部署名称"+deploy.getName());
    }

    /**
     * 发布流程
     */
    @Test
    public void runtimeRelease(){

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("withdraw");
        System.out.println("流程实例ID:"+pi.getId());
        System.out.println("流程定义ID:"+pi.getProcessDefinitionId());
    }

    /**
     * 查询及完成任务
     */
    @Test
    public void taskQueryComplete(){
        List<Task> list = taskService.createTaskQuery()
                .taskAssignee("zhangsan")
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
     *撤回上一个节点
     */
    @Test
    public void taskQueryComplete2() throws Exception{
        //1 当前任务
        Task task = taskService.createTaskQuery()
                .taskAssignee("lisi")
                .singleResult();
        if(task==null) {
            throw new RuntimeException("流程未启动或已执行完成，无法撤回");
        }
        List<HistoricTaskInstance> htlist = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .list();

        String myTaskId = null;
        HistoricTaskInstance myTask = null;

        for (HistoricTaskInstance hti : htlist) {
            //回退到zhangsan也就是任务A,业务中这里就是当前登录的用户名
            if(hti.getAssignee().equals("zhangsan")){
                myTaskId=hti.getId();
                myTask=hti;
                break;
            }
        }
        if(myTask==null){
            throw new RuntimeException("该任务非当前用户提交，无法撤回");
        }
        String processDefinitionId = myTask.getProcessDefinitionId();
        BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);
        // BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        String myActivityId = null;
        List<HistoricActivityInstance> haiList =
                historyService
                        .createHistoricActivityInstanceQuery()
                        .executionId(myTask.getExecutionId())
                        .finished()
                        .list();

        for (HistoricActivityInstance hai : haiList) {
            if(myTaskId.equals(hai.getTaskId())) {
                myActivityId = hai.getActivityId();
                break;
            }
        }
        FlowNode myFlowNode =
                (FlowNode) bpmnModelInstance.getDocumentElement();
        Execution execution = runtimeService.createExecutionQuery()
                .executionId(task.getExecutionId()).singleResult();
        String activityId = execution.getId();
        System.out.println(activityId);
        bpmnModelInstance.getDocumentElement().getElementType();

        // FlowNode flowNode = (FlowNode) bpmnModelInstance.getMainProcess().getFlowElement(activityId);
        FlowNode flowNode = (FlowNode) bpmnModelInstance.getModelElementById(activityId);

        //记录原活动方向
        List<SequenceFlow> oriSequenceFlows = new ArrayList<SequenceFlow>();
        oriSequenceFlows.addAll(flowNode.getOutgoing());

        //清理活动方向
        flowNode.getOutgoing().clear();

        //建立新方向
        // List<SequenceFlow> newSequenceFlowList = new ArrayList<SequenceFlow>();
        // SequenceFlow newSequenceFlow = new SequenceFlow();
        // newSequenceFlow.setId("newSequenceFlowId");
        // newSequenceFlow.setSource(flowNode);
        // newSequenceFlow.setTarget(myFlowNode);
        // newSequenceFlowList.add(newSequenceFlow);
        // flowNode.setExtensionElements(newSequenceFlow);
        // // Authentication.setAuthenticatedUserId("zhangsan");
        // taskService.addComment(task.getId(), task.getProcessInstanceId(), "撤回");
        // //完成任务
        // taskService.complete(task.getId());
        // //恢复原方向
        // flowNode.setOutgoingFlows(oriSequenceFlows);
    }
}
