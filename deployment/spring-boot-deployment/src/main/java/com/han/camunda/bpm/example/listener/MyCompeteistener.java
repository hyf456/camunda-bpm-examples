package com.han.camunda.bpm.example.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

import java.util.ArrayList;

/**
 * @Description 填写请假单时监听器
 * @Date 2020/9/18 13:44
 * @Author hanyf
 */
public class MyCompeteistener implements TaskListener {
	@Override
	public void notify(DelegateTask delegateTask) {
		ArrayList<String> list = new ArrayList<>();
		list.add("王一");
		list.add("王二");
		list.add("王三");

		delegateTask.setVariable("assignees",list);
	}
}
