package com.han.camunda.bpm.example.command;

import org.camunda.bpm.engine.impl.cfg.CommandChecker;
import org.camunda.bpm.engine.impl.cmd.FindActiveActivityIdsCmd;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionManager;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.util.EnsureUtil;

import java.util.Iterator;
import java.util.List;

/**
 * @Description
 * @Date 2020/9/18 13:51
 * @Author hanyf
 */
public class DeleteTaskCommand extends FindActiveActivityIdsCmd {

	public DeleteTaskCommand(String executionId) {
		super(executionId);
	}

	@Override
	public List<String> execute(CommandContext commandContext) {
		EnsureUtil.ensureNotNull("executionId", this.executionId);
		ExecutionManager executionManager = commandContext.getExecutionManager();
		ExecutionEntity execution = executionManager.findExecutionById(this.executionId);
		EnsureUtil.ensureNotNull("execution " + this.executionId + " doesn't exist", "execution", execution);
		this.checkGetActivityIds(execution, commandContext);
		return execution.findActiveActivityIds();
	}

	@Override
	protected void checkGetActivityIds(ExecutionEntity execution, CommandContext commandContext) {
		Iterator var3 = commandContext.getProcessEngineConfiguration().getCommandCheckers().iterator();

		while(var3.hasNext()) {
			CommandChecker checker = (CommandChecker)var3.next();
			checker.checkReadProcessInstance(execution);
		}

	}
}