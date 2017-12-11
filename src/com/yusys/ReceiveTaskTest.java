package com.yusys;

import org.activiti.engine.runtime.Execution;
import org.activiti.engine.task.Task;

public class ReceiveTaskTest extends ActivitiBase {

	private void run() {
		String processInstanceId = initByBpmnFile("receiveTask.bpmn");

		while (true) {
			Task task = taskService.createTaskQuery().singleResult();
			if (task == null) {
				Execution execution = runtimeService.createExecutionQuery()
						.processInstanceId(processInstanceId).singleResult();
				if (execution == null) {
					break;
				}
				runtimeService.setVariable(execution.getId(), "approved", "false");
				runtimeService.signal(execution.getId());
			} else {
				taskService.claim(task.getId(), "leader1");
				taskService.complete(task.getId());
			}
		}
		this.listTask();
	}

	public static void main(String[] args) {
		ReceiveTaskTest at = new ReceiveTaskTest();
		at.run();
	}
}
