package com.han.camunda.bpm.example.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * @Description 任务监听
 * @Date 2020/9/16 15:45
 * @Author hanyf
 */
@Component
public class MyTaskListener implements TaskListener {

	@Override
	public void notify(DelegateTask delegateTask) {
		System.out.println("==============");
		System.out.println("event:" + delegateTask.getEventName());
		System.out.println("办理人:" + delegateTask.getAssignee());
		System.out.println("任务id:" + delegateTask.getId());
		System.out.println("任务名称:" + delegateTask.getName());
		System.out.println("=========================");
	}
}
