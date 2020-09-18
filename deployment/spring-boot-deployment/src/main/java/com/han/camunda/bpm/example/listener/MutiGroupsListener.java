package com.han.camunda.bpm.example.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

/**
 * @Description 项目经理审批时监听器
 * @Date 2020/9/18 13:46
 * @Author hanyf
 */
public class MutiGroupsListener implements TaskListener {
	@Override
	public void notify(DelegateTask delegateTask) {
		delegateTask.setVariable("passCount","0");
		delegateTask.setVariable("totalCount", "0");
		delegateTask.setVariable("noPassCount", "0");

	}
}