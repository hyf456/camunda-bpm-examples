package com.han.camunda.bpm.example.listener;

import org.camunda.bpm.engine.delegate.BaseDelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateListener;

/**
 * @Description TODO
 * @Date 2020/9/16 15:50
 * @Author hanyf
 */
public class MyDelegateListener implements DelegateListener {
	@Override
	public void notify(BaseDelegateExecution baseDelegateExecution) throws Exception {
		System.out.println("-------------------------------");
		System.out.println(baseDelegateExecution.getEventName());
		System.out.println(baseDelegateExecution.getId());
		System.out.println(baseDelegateExecution.getBusinessKey());
		System.out.println("-------------------------------");
	}
}
