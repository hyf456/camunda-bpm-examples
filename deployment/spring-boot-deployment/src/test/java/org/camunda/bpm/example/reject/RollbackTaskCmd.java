package org.camunda.bpm.example.reject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.HistoricActivityInstanceQueryImpl;
import org.camunda.bpm.engine.impl.HistoricTaskInstanceQueryImpl;
import org.camunda.bpm.engine.impl.Page;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 
/**
 * 退回任务.
 */
public class RollbackTaskCmd implements Command<Object> {
    /** logger. */
    private static Logger logger = LoggerFactory
            .getLogger(RollbackTaskCmd.class);

    /** task id. */
    private String taskId;

    /** activity id. */
    private String activityId;

    /** user id. */
    private String userId;

    /** use last assignee. */
    private boolean useLastAssignee = false;

    /** 需要处理的多实例节点. */
    private Set<String> multiInstanceExecutionIds = new HashSet<String>();

    private boolean isParallel = true;

    /**
     * 指定taskId和跳转到的activityId，自动使用最后的assignee.
     */
    public RollbackTaskCmd(String taskId, String activityId) {
        this.taskId = taskId;
        this.activityId = activityId;
        this.useLastAssignee = true;
    }

    /**
     * 指定taskId和跳转到的activityId, userId.
     */
    public RollbackTaskCmd(String taskId, String activityId, String userId) {
        this.taskId = taskId;
        this.activityId = activityId;
        this.userId = userId;
    }

    /**
     * 退回流程.
     *
     * @return 0-退回成功 1-流程结束 2-下一结点已经通过,不能退回
     */
    public Integer execute(CommandContext commandContext) {
        // 获得任务
        TaskEntity taskEntity = this.findTask(commandContext);

        // 找到想要回退到的节点
        ActivityImpl targetActivity = this.findTargetActivity(commandContext,
                taskEntity);
        logger.info("rollback to {}", this.activityId);
        logger.info("{}", targetActivity.getProperties());

        String type = (String) targetActivity.getProperty("type");

        if ("userTask".equals(type)) {
            logger.info("rollback to userTask");
            this.rollbackUserTask(commandContext, taskEntity, targetActivity);
        } else if ("startEvent".equals(type)) {
            logger.info("rollback to startEvent");
            this.rollbackStartEvent(commandContext);
        } else {
            throw new IllegalStateException("cannot rollback " + type);
        }

        return 0;
    }

    /**
     * 回退到userTask.
     */
    public Integer rollbackUserTask(CommandContext commandContext, TaskEntity taskEntity, ActivityImpl targetActivity) {

        // 找到想要回退对应的节点历史
        HistoricActivityInstanceEntity historicActivityInstanceEntity = this
                .findTargetHistoricActivity(commandContext, taskEntity,
                        targetActivity);

        // 找到想要回退对应的任务历史
        HistoricTaskInstanceEntity historicTaskInstanceEntity = this
                .findTargetHistoricTask(commandContext, taskEntity,
                        targetActivity);

        logger.info("historic activity instance is : {}",
                historicActivityInstanceEntity.getId());

        Graph graph = new ActivitiHistoryGraphBuilder(
                historicTaskInstanceEntity.getProcessInstanceId()).build();

        Node node = graph.findById(historicActivityInstanceEntity.getId());

        if (!checkCouldRollback(node, taskEntity.getProcessInstanceId())) {
            logger.info("cannot rollback {}", taskId);

            return 2;
        }

        if (this.isSameBranch(historicTaskInstanceEntity)) {
            // 如果退回的目标节点的executionId与当前task的executionId一样，说明是同一个分支
            // 只删除当前分支的task
            TaskEntity targetTaskEntity = Context.getCommandContext()
                    .getTaskEntityManager().findTaskById(this.taskId);
            this.deleteActiveTask(targetTaskEntity);
        } else {
            // 否则认为是从分支跳回主干
            // 删除所有活动中的task
            this.deleteActiveTasks(historicTaskInstanceEntity.getProcessInstanceId());

            // 获得期望退回的节点后面的所有节点历史
            List<String> historyNodeIds = new ArrayList<String>();
            collectNodes(node, historyNodeIds);
//            this.deleteHistoryActivities(historyNodeIds);
        }

        Map<String, Object> map = targetActivity.getProperties();
        if (map.containsKey("multiInstance")) {
            this.rollbackProcessMultiInstance(commandContext, taskEntity, historicTaskInstanceEntity, historicActivityInstanceEntity, targetActivity);
        } else {
            // 恢复期望退回的任务和历史
            this.processHistoryTask(commandContext, taskEntity, historicTaskInstanceEntity, historicActivityInstanceEntity, targetActivity);
        }
        logger.info("activiti is rollback {}",
                historicTaskInstanceEntity.getName());

        return 0;
    }

    private VariableInstanceEntity insertVariableInstanceEntity(String name, Object value, String executionId, String processInstanceId) {
        VariableTypes variableTypes = Context.getProcessEngineConfiguration().getVariableTypes();
        VariableType newType = variableTypes.findVariableType(value);
        VariableInstanceEntity variableInstance = VariableInstanceEntity.create(name, newType, value);
        variableInstance.setExecutionId(executionId);
        variableInstance.setProcessInstanceId(processInstanceId);
        return variableInstance;
    }

    /**
     * 回退到startEvent.
     */
    public Integer rollbackStartEvent(CommandContext commandContext) {
        // 获得任务
        TaskEntity taskEntity = this.findTask(commandContext);

        // 找到想要回退到的节点
        ActivityImpl targetActivity = this.findTargetActivity(commandContext,
                taskEntity);

        if (taskEntity.getExecutionId().equals(
                taskEntity.getProcessInstanceId())) {
            // 如果退回的目标节点的executionId与当前task的executionId一样，说明是同一个分支
            // 只删除当前分支的task
            TaskEntity targetTaskEntity = Context.getCommandContext()
                    .getTaskEntityManager().findTaskById(this.taskId);
            this.deleteActiveTask(targetTaskEntity);
        } else {
            // 否则认为是从分支跳回主干
            // 删除所有活动中的task
            this.deleteActiveTasks(taskEntity.getProcessInstanceId());
        }

        // 把流程指向任务对应的节点
        ExecutionEntity executionEntity = Context.getCommandContext()
                .getExecutionEntityManager()
                .findExecutionById(taskEntity.getExecutionId());
        executionEntity.setActivity(targetActivity);

        // 创建HistoricActivityInstance
        Context.getCommandContext().getHistoryManager()
                .recordActivityStart(executionEntity);

        // 处理多实例
//        this.processMultiInstance();

        return 0;
    }

    /**
     * 获得当前任务.
     */
    public TaskEntity findTask(CommandContext commandContext) {
        TaskEntity taskEntity = commandContext.getTaskEntityManager()
                .findTaskById(taskId);

        return taskEntity;
    }

    /**
     * 查找回退的目的节点.
     */
    public ActivityImpl findTargetActivity(CommandContext commandContext,
                                           TaskEntity taskEntity) {
        if (activityId == null) {
            String historyTaskId = this.findNearestUserTask(commandContext);
            HistoricTaskInstanceEntity historicTaskInstanceEntity = commandContext
                    .getHistoricTaskInstanceEntityManager()
                    .findHistoricTaskInstanceById(historyTaskId);
            this.activityId = historicTaskInstanceEntity.getTaskDefinitionKey();
        }

        String processDefinitionId = taskEntity.getProcessDefinitionId();
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                processDefinitionId).execute(commandContext);

        return processDefinitionEntity.findActivity(activityId);
    }

    /**
     * 找到想要回退对应的节点历史.
     */
    public HistoricActivityInstanceEntity findTargetHistoricActivity(
            CommandContext commandContext, TaskEntity taskEntity,
            ActivityImpl activityImpl) {
        HistoricActivityInstanceQueryImpl historicActivityInstanceQueryImpl = new HistoricActivityInstanceQueryImpl();
        historicActivityInstanceQueryImpl.activityId(activityImpl.getId());
        historicActivityInstanceQueryImpl.processInstanceId(taskEntity
                .getProcessInstanceId());
        historicActivityInstanceQueryImpl
                .orderByHistoricActivityInstanceEndTime().desc();

        List<HistoricActivityInstance> list = commandContext
                .getHistoricActivityInstanceEntityManager()
                .findHistoricActivityInstancesByQueryCriteria(
                        historicActivityInstanceQueryImpl, new Page(0, 99));
        HistoricActivityInstanceEntity historicActivityInstanceEntity = new HistoricActivityInstanceEntity();
        for (HistoricActivityInstance historicActivityInstance : list) {
            if (StringTools.isValue(historicActivityInstance.getEndTime())) {
                historicActivityInstanceEntity = (HistoricActivityInstanceEntity) historicActivityInstance;
            }
        }
        return historicActivityInstanceEntity;
    }

    /**
     * 找到想要回退对应的节点历史.
     */
    public HistoricActivityInstanceEntity findTargetHistoricActivity(
            CommandContext commandContext, TaskEntity taskEntity,
            String taskDefKey) {
        HistoricActivityInstanceQueryImpl historicActivityInstanceQueryImpl = new HistoricActivityInstanceQueryImpl();
        historicActivityInstanceQueryImpl.activityId(taskDefKey);
        historicActivityInstanceQueryImpl.processInstanceId(taskEntity
                .getProcessInstanceId());
        historicActivityInstanceQueryImpl
                .orderByHistoricActivityInstanceEndTime().desc();

        return (HistoricActivityInstanceEntity) commandContext.getHistoricActivityInstanceEntityManager().findHistoricActivityInstancesByQueryCriteria(historicActivityInstanceQueryImpl, new Page(0, 99)).get(0);
    }

    public List<HistoricActivityInstance> findTargetHistoricActivityList(
            CommandContext commandContext, String activityId,
            String processInstanceId) {
        HistoricActivityInstanceQueryImpl historicActivityInstanceQueryImpl = new HistoricActivityInstanceQueryImpl();
        historicActivityInstanceQueryImpl.activityId(activityId);
        historicActivityInstanceQueryImpl.processInstanceId(processInstanceId);
        historicActivityInstanceQueryImpl.orderByHistoricActivityInstanceEndTime().desc();

        return commandContext
                .getHistoricActivityInstanceEntityManager()
                .findHistoricActivityInstancesByQueryCriteria(
                        historicActivityInstanceQueryImpl, new Page(0, 99));
    }

    /**
     * 找到想要回退对应的节点历史.
     */
    public List<HistoricActivityInstance> findTargetHistoricActivityList(
            CommandContext commandContext, TaskEntity taskEntity,
            ActivityImpl activityImpl) {
        HistoricActivityInstanceQueryImpl historicActivityInstanceQueryImpl = new HistoricActivityInstanceQueryImpl();
        historicActivityInstanceQueryImpl.activityId(activityImpl.getId());
        historicActivityInstanceQueryImpl.processInstanceId(taskEntity
                .getProcessInstanceId());
        historicActivityInstanceQueryImpl
                .orderByHistoricActivityInstanceEndTime().desc();

        return commandContext
                .getHistoricActivityInstanceEntityManager()
                .findHistoricActivityInstancesByQueryCriteria(
                        historicActivityInstanceQueryImpl, new Page(0, 99));
    }

    /**
     * 找到想要回退对应的任务历史.
     */
    public HistoricTaskInstanceEntity findTargetHistoricTask(
            CommandContext commandContext, TaskEntity taskEntity,
            ActivityImpl activityImpl) {
        HistoricTaskInstanceQueryImpl historicTaskInstanceQueryImpl = new HistoricTaskInstanceQueryImpl();
        historicTaskInstanceQueryImpl.taskDefinitionKey(activityImpl.getId());
        historicTaskInstanceQueryImpl.processInstanceId(taskEntity.getProcessInstanceId());
        historicTaskInstanceQueryImpl.setFirstResult(0);
        historicTaskInstanceQueryImpl.setMaxResults(1);
        historicTaskInstanceQueryImpl.orderByTaskCreateTime().asc();

        HistoricTaskInstanceEntity historicTaskInstanceEntity = (HistoricTaskInstanceEntity) commandContext
                .getHistoricTaskInstanceEntityManager()
                .findHistoricTaskInstancesByQueryCriteria(
                        historicTaskInstanceQueryImpl).get(0);

        return historicTaskInstanceEntity;
    }

    /**
     * 判断想要回退的目标节点和当前节点是否在一个分支上.
     */
    public boolean isSameBranch(
            HistoricTaskInstanceEntity historicTaskInstanceEntity) {
        TaskEntity taskEntity = Context.getCommandContext()
                .getTaskEntityManager().findTaskById(taskId);

        return taskEntity.getExecutionId().equals(
                historicTaskInstanceEntity.getExecutionId());
    }

    /**
     * 查找离当前节点最近的上一个userTask.
     */
    public String findNearestUserTask(CommandContext commandContext) {
        TaskEntity taskEntity = commandContext.getTaskEntityManager()
                .findTaskById(taskId);

        if (taskEntity == null) {
            logger.debug("cannot find task : {}", taskId);

            return null;
        }

        Graph graph = new ActivitiHistoryGraphBuilder(
                taskEntity.getProcessInstanceId()).build();
//        commandContext.getHistoricActivityInstanceEntityManager().findHistoricActivityInstancesByQueryCriteria(historicActivityInstanceQuery, page)
        HistoricActivityInstanceEntity hisActEntity = findTargetHistoricActivity(commandContext, taskEntity, taskEntity.getTaskDefinitionKey());
//        commandContext.getHistoricActivityInstanceEntityManager().findHistoricActivityInstance(taskEntity.getTaskDefinitionKey(), taskEntity.getProcessInstanceId());
        Node node = graph.findById(hisActEntity.getId());
        if (node == null)
            return null;
        String previousHistoricActivityInstanceId = this.findIncomingNode(
                graph, node, taskEntity.getProcessInstanceId());

        if (previousHistoricActivityInstanceId == null) {
            logger.debug(
                    "cannot find previous historic activity instance : {}",
                    taskEntity);

            return null;
        }
        List<HistoricActivityInstance> hisTaskList = findTargetHistoricActivityList(commandContext, previousHistoricActivityInstanceId, hisActEntity.getProcessInstanceId());
        for (HistoricActivityInstance historicActivityInstance : hisTaskList) {
            if (StringTools.isValue(historicActivityInstance.getEndTime())) {
                return historicActivityInstance.getTaskId();
            }
        }
        return hisTaskList.get(0).getTaskId();
    }

    /**
     * 查找进入的连线.
     */
    public String findIncomingNode(Graph graph, Node node, String processInstanceId) {
        for (Edge edge : graph.getEdges()) {
            Node src = edge.getSrc();
            Node dest = edge.getDest();
            String srcType = src.getType();

            if (!dest.getId().equals(node.getId())) {
                continue;
            }

            if ("userTask".equals(srcType)) {
                boolean isSkip = isSkipActivity(src.getId(), processInstanceId);

                if (isSkip) {
                    return this.findIncomingNode(graph, src, processInstanceId);
                } else {
                    return src.getName();
                }
            } else if (srcType.endsWith("Gateway")) {
                return this.findIncomingNode(graph, src, processInstanceId);
            } else {
                logger.info("cannot rollback, previous node is not userTask : "
                        + src.getId() + " " + srcType + "(" + src.getName()
                        + ")");

                return null;
            }
        }

        logger.info("cannot rollback, this : " + node.getId() + " "
                + node.getType() + "(" + node.getName() + ")");

        return null;
    }


    /**
     * 判断是否可回退.
     */
    public boolean checkCouldRollback(Node node, String processInstanceId) {
        // TODO: 如果是catchEvent，也应该可以退回，到时候再说
        for (Edge edge : node.getOutgoingEdges()) {
            Node dest = edge.getDest();
            String type = dest.getType();

            if ("userTask".equals(type)) {
                if (!dest.isActive()) {
                    boolean isSkip = isSkipActivity(dest.getId(), processInstanceId);

                    if (isSkip) {
                        return checkCouldRollback(dest, processInstanceId);
                    } else {
                        // logger.info("cannot rollback, " + type + "("
                        // + dest.getName() + ") is complete.");
                        // return false;
                        return true;
                    }
                }
            } else if (type.endsWith("Gateway")) {
                return checkCouldRollback(dest, processInstanceId);
            } else {
                logger.info("cannot rollback, " + type + "(" + dest.getName()
                        + ") is complete.");

                return false;
            }
        }

        return true;
    }

    /**
     * 删除活动状态任务.
     */
    public void deleteActiveTasks(String processInstanceId) {
        List<TaskEntity> taskEntities = Context.getCommandContext()
                .getTaskEntityManager()
                .findTasksByProcessInstanceId(processInstanceId);

        for (TaskEntity taskEntity : taskEntities) {
            this.deleteActiveTask(taskEntity);
        }
    }

    /**
     * 遍历节点.
     */
    public void collectNodes(Node node, List<String> historyNodeIds) {
        logger.info("node : {}, {}, {}", node.getId(), node.getType(),
                node.getName());

        for (Edge edge : node.getOutgoingEdges()) {
            logger.info("edge : {}", edge.getName());

            Node dest = edge.getDest();
            historyNodeIds.add(dest.getId());
            collectNodes(dest, historyNodeIds);
        }
    }

    /**
     *
     * <p>Title: 退回多实例环节  并行/串行</p>
     * <p>Description: </p>
     * @param commandContext
     * @param taskEntity
     * @param historicTaskInstanceEntity
     * @param historicActivityInstanceEntity
     * @param targetActivity
     * @date 2018年8月31日
     * @author zhuzubin
     */
    public void rollbackProcessMultiInstance(CommandContext commandContext,
                                             TaskEntity taskEntity, HistoricTaskInstanceEntity historicTaskInstanceEntity,
                                             HistoricActivityInstanceEntity historicActivityInstanceEntity, ActivityImpl targetActivity) {

        //查找历史处理人
        HistoricTaskInstanceQueryImpl query = new HistoricTaskInstanceQueryImpl();
        query.taskDefinitionKey(targetActivity.getId());
        query.processInstanceId(historicTaskInstanceEntity.getProcessInstanceId());
        query.orderByHistoricTaskInstanceEndTime().desc();
        query.setFirstResult(0);
        query.setMaxResults(99);
        List<HistoricTaskInstance> list = Context.getCommandContext().getHistoricTaskInstanceEntityManager().findHistoricTaskInstancesByQueryCriteria(query);
        //过虑重复退回任务
        for (int i = 0; i < list.size() - 1; i++) {
            for (int j = list.size() - 1; j > i; j--) {
                if (list.get(j).getTaskDefinitionKey().equals(list.get(i).getTaskDefinitionKey()) && list.get(j).getAssignee().equals(list.get(i).getAssignee())) {
                    list.remove(j);
                }
            }
        }
        // 删除旧的Execution
        String processDefinitionId = taskEntity.getProcessDefinitionId();
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                processDefinitionId).execute(commandContext);

        ExecutionEntity executionEntity = taskEntity.getExecution(); //taskEntity.getExecution();
        ActivityImpl activityImpl = processDefinitionEntity.findActivity(taskEntity.getTaskDefinitionKey());
        Map<String, Object> currMap = activityImpl.getProperties();
        if (currMap.containsKey("multiInstance")) { // 删除当前多实例任务
            executionEntity = taskEntity.getExecution().getParent().getParent(); //taskEntity.getExecution();
            this.deleteExecution(taskEntity);
        }
        executionEntity.setProcessInstance(executionEntity);
        executionEntity.setProcessDefinitionId(historicActivityInstanceEntity.getProcessDefinitionId());
        executionEntity.setActivity(targetActivity);
        executionEntity.setActive(false);
        executionEntity.setConcurrent(false);
        executionEntity.setCachedEntityState(0);
        // 创建HistoricActivityInstance
        if (currMap.containsKey("multiInstance")) {
            Context.getCommandContext().getHistoryManager().recordExecutionReplacedBy(taskEntity.getExecution().getParent().getParent(), executionEntity);
        } else {
            Context.getCommandContext().getHistoryManager().recordExecutionReplacedBy(taskEntity.getExecution(), executionEntity);
        }
        ExecutionEntity executionEntity_parent = new ExecutionEntity();
        executionEntity_parent.setParentId(executionEntity.getId());
        executionEntity_parent.setCachedEntityState(6);
        executionEntity_parent.setProcessDefinitionKey(historicActivityInstanceEntity.getProcessDefinitionKey());
        executionEntity_parent.setProcessInstance(executionEntity);
        executionEntity_parent.setProcessDefinitionId(historicActivityInstanceEntity.getProcessDefinitionId());
        executionEntity_parent.setActivity(targetActivity);
        if (targetActivity.getProperties().get("multiInstance").equals("sequential")) { // 串行任务
            this.isParallel = false;
            executionEntity_parent.setActive(true);
            executionEntity_parent.setConcurrent(false);
            executionEntity_parent.setScope(true);
            Context.getCommandContext().getExecutionEntityManager().insert(executionEntity_parent);
            // 创建HistoricActivityInstance
            Context.getCommandContext().getHistoryManager().recordActivityStart(executionEntity_parent);
            TaskEntity task = TaskEntity.create(new Date());
            task.setExecutionId(executionEntity_parent.getId());
            this.setTaskEntity(task, historicTaskInstanceEntity, list.get(list.size() - 1).getAssignee());
            Context.getCommandContext().getTaskEntityManager().insert(task);
            // 创建HistoricTaskInstance
            Context.getCommandContext().getHistoryManager().recordTaskCreated(task, executionEntity_parent);
            Context.getCommandContext().getHistoryManager().recordTaskId(task);
            // 更新ACT_HI_ACTIVITY里的assignee字段
            Context.getCommandContext().getHistoryManager().recordTaskAssignment(task);
            this.createVariable(list.size(), 1, executionEntity_parent.getId(), executionEntity.getProcessInstanceId(), list.get(list.size() - 1).getAssignee());
        } else { //并行任务
            executionEntity_parent.setActive(false);
            executionEntity_parent.setConcurrent(false);
            executionEntity_parent.setScope(true);
            Context.getCommandContext().getExecutionEntityManager().insert(executionEntity_parent);
            // 创建多实例任务
            this.createVariable(list.size(), list.size(), executionEntity_parent.getId(), executionEntity.getProcessInstanceId(), "");
            int i = 0;
            for (HistoricTaskInstance historicTaskInstance : list) {
                if (StringTools.isValue(historicTaskInstance.getAssignee())) {
                    ExecutionEntity executionEntity_c = new ExecutionEntity();
                    executionEntity_c.setParentId(executionEntity_parent.getId());
                    executionEntity_c.setActive(true);
                    executionEntity_c.setScope(false);
                    executionEntity_c.setConcurrent(true);
                    executionEntity_c.setActivity(targetActivity);
                    executionEntity_c.setCachedEntityState(7);
                    executionEntity_c.setProcessDefinitionKey(historicActivityInstanceEntity.getProcessDefinitionKey());
                    executionEntity_c.setProcessInstance(executionEntity);
                    executionEntity_c.setProcessDefinitionId(historicActivityInstanceEntity.getProcessDefinitionId());
                    Context.getCommandContext().getExecutionEntityManager().insert(executionEntity_c);
                    // 创建HistoricActivityInstance
                    Context.getCommandContext().getHistoryManager().recordActivityStart(executionEntity_c);
                    TaskEntity task = TaskEntity.create(new Date());
                    task.setExecutionId(executionEntity_c.getId());
                    this.setTaskEntity(task, historicTaskInstanceEntity, historicTaskInstance.getAssignee());
                    Context.getCommandContext().getTaskEntityManager().insert(task);
                    // 创建HistoricTaskInstance
                    Context.getCommandContext().getHistoryManager().recordTaskCreated(task, executionEntity_c);
                    Context.getCommandContext().getHistoryManager().recordTaskId(task);
                    // 更新ACT_HI_ACTIVITY里的assignee字段
                    Context.getCommandContext().getHistoryManager().recordTaskAssignment(task);
                    String[] varName_ = {"loopCounter", "processUser"};
                    for (String name : varName_) {
                        VariableTypes variableTypes = Context.getProcessEngineConfiguration().getVariableTypes();
                        VariableInstanceEntity variableInstance = this.insertVariableInstanceEntity(name, i, executionEntity_c.getId(), historicTaskInstanceEntity.getProcessInstanceId());
                        switch (name) {
                            case "loopCounter":
                                variableInstance.setLongValue(Long.valueOf(i));
                                variableInstance.setTextValue(i + "");
                                break;
                            case "processUser":
                                VariableType newType = variableTypes.findVariableType(historicTaskInstance.getAssignee());
                                variableInstance.setType(newType);
                                variableInstance.setLongValue(null);
                                variableInstance.setTextValue(historicTaskInstance.getAssignee());
                                break;
                        }
                        Context.getCommandContext().getVariableInstanceEntityManager().insert(variableInstance);
                    }
                    i++;
                }
            }
        }
    }

    /**
     *
     * <p>Title: 创建变量参数</p>
     * <p>Description: </p>
     * @param size
     * @param activeInstanceSize
     * @param executionEntityId
     * @param processInstanceId
     * @param userId
     * @date 2018年8月31日
     * @author zhuzubin
     */
    public void createVariable(int size, int activeInstanceSize, String executionEntityId, String processInstanceId, String userId) {
        List<String> varName = new ArrayList<String>();
        varName.add("nrOfInstances");
        varName.add("nrOfCompletedInstances");
        varName.add("nrOfActiveInstances");
        if (!this.isParallel) {
            varName.add("loopCounter");
            varName.add("processUser");
        }
        for (String name : varName) {
            VariableTypes variableTypes = Context.getProcessEngineConfiguration().getVariableTypes();
            VariableType newType = variableTypes.findVariableType(size);
            VariableInstanceEntity variableInstance = this.insertVariableInstanceEntity(name, size, executionEntityId, processInstanceId);
            switch (name) {
                case "nrOfInstances":
                    variableInstance.setLongValue(Long.valueOf(size));
                    break;
                case "nrOfCompletedInstances":
                    newType = variableTypes.findVariableType(0);
                    variableInstance.setType(newType);
                    variableInstance.setLongValue(0L);
                    variableInstance.setTextValue("0");
                    break;
                case "nrOfActiveInstances":
                    variableInstance.setLongValue(Long.valueOf(activeInstanceSize));
                    if (!this.isParallel) {
                        variableInstance.setTextValue(activeInstanceSize + "");
                    }
                    break;
                case "loopCounter":
                    variableInstance.setLongValue(0L);
                    variableInstance.setTextValue("0");
                    break;
                case "processUser":
                    newType = variableTypes.findVariableType(userId);
                    variableInstance.setType(newType);
                    variableInstance.setLongValue(null);
                    variableInstance.setTextValue(userId);
                    break;
            }
            Context.getCommandContext().getVariableInstanceEntityManager().insert(variableInstance);
        }
    }

    public void deleteExecution(TaskEntity taskEntity) {
        // 删除未处理任务信息
        List<TaskEntity> taskEntities = Context.getCommandContext().getTaskEntityManager().findTasksByProcessInstanceId(taskEntity.getProcessInstanceId());
        for (TaskEntity taskEntity2 : taskEntities) {
            List<VariableInstanceEntity> varLis = Context.getCommandContext().getVariableInstanceEntityManager().findVariableInstancesByExecutionId(taskEntity2.getExecutionId());
            for (VariableInstanceEntity variableInstanceEntity : varLis) {
                Context.getCommandContext().getVariableInstanceEntityManager().delete(variableInstanceEntity);
            }
            Context.getCommandContext().getExecutionEntityManager().delete(taskEntity2.getExecution());
        }
        // 获取多实例同节点处理任务
        HistoricTaskInstanceQueryImpl historicTaskInstanceQueryImpl = new HistoricTaskInstanceQueryImpl();
        historicTaskInstanceQueryImpl.taskDefinitionKey(taskEntity.getTaskDefinitionKey());
        historicTaskInstanceQueryImpl.processInstanceId(taskEntity.getProcessInstanceId());
        historicTaskInstanceQueryImpl.setFirstResult(0);
        historicTaskInstanceQueryImpl.setMaxResults(999);
        historicTaskInstanceQueryImpl.orderByTaskCreateTime().asc();
        List<HistoricTaskInstance> historicTaskInstanceList = (List<HistoricTaskInstance>) Context.getCommandContext()
                .getHistoricTaskInstanceEntityManager().findHistoricTaskInstancesByQueryCriteria(historicTaskInstanceQueryImpl);
        if (historicTaskInstanceList != null && historicTaskInstanceList.size() > 0) {
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstanceList) {
                ExecutionEntity executionEntity = Context.getCommandContext().getExecutionEntityManager().findExecutionById(historicTaskInstance.getExecutionId());
                if (executionEntity != null) {
                    List<VariableInstanceEntity> hisVarLis = Context.getCommandContext().getVariableInstanceEntityManager().findVariableInstancesByExecutionId(executionEntity.getId());
                    for (VariableInstanceEntity variableInstanceEntity : hisVarLis) {
                        Context.getCommandContext().getVariableInstanceEntityManager().delete(variableInstanceEntity);
                    }
                    Context.getCommandContext().getExecutionEntityManager().delete(executionEntity);
                }
            }
        }
        // 删除多实例父节点信息
        ExecutionEntity parent = Context.getCommandContext().getExecutionEntityManager().findExecutionById(taskEntity.getExecution().getParentId());
        List<VariableInstanceEntity> varLis = Context.getCommandContext().getVariableInstanceEntityManager().findVariableInstancesByExecutionId(parent.getId());
        for (VariableInstanceEntity variableInstanceEntity : varLis) {
            Context.getCommandContext().getVariableInstanceEntityManager().delete(variableInstanceEntity);
        }
        Context.getCommandContext().getExecutionEntityManager().delete(parent);
    }

    public void deleteExecution(ExecutionEntity executionEntity) {
        Context.getCommandContext().getExecutionEntityManager().delete(executionEntity);
    }

    /**
     * 根据任务历史，创建待办任务.
     */
    public void processHistoryTask(CommandContext commandContext,
                                   TaskEntity taskEntity,
                                   HistoricTaskInstanceEntity historicTaskInstanceEntity,
                                   HistoricActivityInstanceEntity historicActivityInstanceEntity, ActivityImpl targetActivity) {
        if (this.userId == null) {
            if (this.useLastAssignee) {
                this.userId = historicTaskInstanceEntity.getAssignee();
            } else {
                String processDefinitionId = taskEntity.getProcessDefinitionId();
                ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(processDefinitionId).execute(commandContext);
                TaskDefinition taskDefinition = processDefinitionEntity
                        .getTaskDefinitions().get(
                                historicTaskInstanceEntity
                                        .getTaskDefinitionKey());

                if (taskDefinition == null) {
                    String message = "cannot find taskDefinition : "
                            + historicTaskInstanceEntity.getTaskDefinitionKey();
                    logger.info(message);
                    throw new IllegalStateException(message);
                }

                if (taskDefinition.getAssigneeExpression() != null) {
                    logger.info("assignee expression is null : {}",
                            taskDefinition.getKey());
                    this.userId = (String) taskDefinition
                            .getAssigneeExpression().getValue(taskEntity);
                }
            }
        }

        // 创建新任务
        TaskEntity task = TaskEntity.create(new Date());
        task.setExecutionId(taskEntity.getExecutionId());
        // 把流程指向任务对应的节点
        ExecutionEntity executionEntity = taskEntity.getExecution();
        String processDefinitionId = taskEntity.getProcessDefinitionId();
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                processDefinitionId).execute(commandContext);
        ActivityImpl activityImpl = processDefinitionEntity.findActivity(taskEntity.getTaskDefinitionKey());
        Map<String, Object> currMap = activityImpl.getProperties();
        if (currMap.containsKey("multiInstance")) { // 删除当前多实例任务
            if (currMap.get("multiInstance").equals("sequential")) {
                executionEntity = taskEntity.getExecution().getParent();
                // 获取删除当前任务 execution
                this.deleteExecution(taskEntity.getExecution());
            } else {
                executionEntity = taskEntity.getExecution().getParent().getParent(); //taskEntity.getExecution();
                this.deleteExecution(taskEntity);
            }
            task.setExecutionId(executionEntity.getId());

        }
        this.setTaskEntity(task, historicTaskInstanceEntity, this.userId);
        Context.getCommandContext().getTaskEntityManager().insert(task);
        executionEntity.setProcessInstance(executionEntity);
        executionEntity.setProcessDefinitionId(historicActivityInstanceEntity.getProcessDefinitionId());
        executionEntity.setActivity(targetActivity);
//        // 创建HistoricActivityInstance
//        if (currMap.containsKey("multiInstance")) {
//            if (currMap.get("multiInstance").equals("sequential")) {
//                Context.getCommandContext().getHistoryManager().recordExecutionReplacedBy(taskEntity.getExecution().getParent(), executionEntity);
//            } else {
//                Context.getCommandContext().getHistoryManager().recordExecutionReplacedBy(taskEntity.getExecution().getParent().getParent(), executionEntity);
//            }
//        } else {
//            Context.getCommandContext().getHistoryManager().recordExecutionReplacedBy(taskEntity.getExecution(), executionEntity);
//        }
        // 创建HistoricActivityInstance
        Context.getCommandContext().getHistoryManager().recordActivityStart(executionEntity);
        // 创建HistoricTaskInstance
        Context.getCommandContext().getHistoryManager().recordTaskCreated(task, executionEntity);
        Context.getCommandContext().getHistoryManager().recordTaskId(task);
        // 更新ACT_HI_ACTIVITY里的assignee字段
        Context.getCommandContext().getHistoryManager().recordTaskAssignment(task);

    }

    public void setTaskEntity(TaskEntity task, HistoricTaskInstanceEntity historicTaskInstanceEntity, String userId) {
        task.setProcessDefinitionId(historicTaskInstanceEntity.getProcessDefinitionId());
        task.setAssigneeWithoutCascade(userId);
        task.setParentTaskIdWithoutCascade(historicTaskInstanceEntity.getParentTaskId());
        task.setNameWithoutCascade(historicTaskInstanceEntity.getName());
        task.setTaskDefinitionKey(historicTaskInstanceEntity.getTaskDefinitionKey());
        task.setFormKey(historicTaskInstanceEntity.getFormKey());
        task.setPriority(historicTaskInstanceEntity.getPriority());
        task.setProcessInstanceId(historicTaskInstanceEntity.getProcessInstanceId());
        task.setDescriptionWithoutCascade(historicTaskInstanceEntity.getDescription());
        task.setTenantId(historicTaskInstanceEntity.getTenantId());
    }

    /**
     * 获得历史节点对应的节点信息.
     */
    public ActivityImpl getActivity(
            HistoricActivityInstanceEntity historicActivityInstanceEntity) {
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                historicActivityInstanceEntity.getProcessDefinitionId())
                .execute(Context.getCommandContext());

        return processDefinitionEntity
                .findActivity(historicActivityInstanceEntity.getActivityId());
    }

    /**
     * 删除未完成任务.
     */
    public void deleteActiveTask(TaskEntity taskEntity) {
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                taskEntity.getProcessDefinitionId()).execute(Context
                .getCommandContext());

        ActivityImpl activityImpl = processDefinitionEntity
                .findActivity(taskEntity.getTaskDefinitionKey());

        if (this.isMultiInstance(activityImpl)) {
            logger.info("{} is multiInstance", taskEntity.getId());
            this.multiInstanceExecutionIds.add(taskEntity.getExecution()
                    .getParent().getId());
            logger.info("append : {}", taskEntity.getExecution().getParent()
                    .getId());
            List<VariableInstanceEntity> varLis = Context.getCommandContext().getVariableInstanceEntityManager().findVariableInstancesByExecutionId(taskEntity.getExecutionId());
            for (VariableInstanceEntity variableInstanceEntity : varLis) {
                Context.getCommandContext().getVariableInstanceEntityManager().delete(variableInstanceEntity);
            }
        }
        Context.getCommandContext().getTaskEntityManager().deleteTask(taskEntity, "回退", false);
    }

    /**
     * 判断跳过节点.
     */
    public boolean isSkipActivity(String historyActivityId, String processInstanceId) {
//        JdbcTemplate jdbcTemplate = ApplicationContextHelper
//                .getBean(JdbcTemplate.class);
//        String historyTaskId = jdbcTemplate.queryForObject(
//                "SELECT TASK_ID_ FROM ACT_HI_ACTINST WHERE ID_=?",
//                String.class, historyActivityId);
//        HistoricActivityInstanceEntity hisActivityEntity = Context.getCommandContext().getHistoricActivityInstanceEntityManager().findHistoricActivityInstance(historyActivityId, processInstanceId);
//        HistoricTaskInstanceEntity historicTaskInstanceEntity = Context
//                .getCommandContext().getHistoricTaskInstanceEntityManager()
//                .findHistoricTaskInstanceById(hisActivityEntity.getTaskId());
//        String deleteReason = historicTaskInstanceEntity.getDeleteReason();

        return false;
    }

    /**
     * 判断是否会签.
     */
    public boolean isMultiInstance(PvmActivity pvmActivity) {
        return pvmActivity.getProperty("multiInstance") != null;
    }


}