package com.han.camunda.bpm.example.listener;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * @Description 开始监听
 * @Date 2020/9/16 15:47
 * @Author hanyf
 */
@Component
public class MyStartListener implements ExecutionListener {

	@Override
	public void notify(DelegateExecution delegateExecution) throws Exception {
		System.out.println("流程启动");
		System.out.println(delegateExecution.getEventName());
		System.out.println(delegateExecution.getProcessDefinitionId());
		System.out.println(delegateExecution.getProcessInstanceId());
		System.out.println(delegateExecution.getProcessBusinessKey());
	}
}
