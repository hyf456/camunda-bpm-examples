package com.han.camunda.bpm.example.error;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import java.io.Serializable;

/**
 * @Description 错误时间
 * @Date 2020/9/17 16:40
 * @Author hanyf
 */
public class ErrorBoundaryDelegate implements JavaDelegate, Serializable {

	@Override
	public void execute(DelegateExecution delegateExecution) {
		String errorCode = "aaa";
		System.out.println("抛出错误errorCode："+ errorCode);
		throw new BpmnError(errorCode);
	}
}
