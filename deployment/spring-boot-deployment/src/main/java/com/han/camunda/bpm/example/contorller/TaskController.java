package com.han.camunda.bpm.example.contorller;

import com.han.camunda.bpm.example.reject.CommonTaskVO;
import com.han.camunda.bpm.example.reject.RejectModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 流程实例任务的Rest实现
 */
@Component
public class TaskController {
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private RejectModification rejectModification;

    public Boolean rollbackTaskCmd(CommonTaskVO commonTaskVO) {
        // RollbackTaskCmd rollbackTaskCmd = new RollbackTaskCmd(commonTaskVO.getTaskId(), commonTaskVO.getActivityId());
        // ((ProcessEngineConfigurationImpl)processEngine.getProcessEngineConfiguration()).getCommandExecutorTxRequired()
        //         .execute(rollbackTaskCmd);
        // rejectTask.rejectTask(commonTaskVO.getDestinationTaskId(), commonTaskVO.getTaskId(), commonTaskVO.getRemark());
        rejectModification.reject(commonTaskVO);
        return true;
    }


}
