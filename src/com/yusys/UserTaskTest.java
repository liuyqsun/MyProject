package com.yusys;

import org.activiti.engine.task.Task;

public class UserTaskTest extends ActivitiBase {

	private void run() {
		initByBpmnFile("userTask.bpmn");

		// 处理 usertask1
		Task task = taskService.createTaskQuery().taskCandidateUser("newLeader1").singleResult();
		if (task == null) {
			System.err.println("newLeader1 Task Query FAIL");
			return;
		}
		taskService.claim(task.getId(), "newLeader1");
		taskService.setVariable(task.getId(), "approved", true);
		taskService.complete(task.getId());
		
		// 处理 usertask2
		task = taskService.createTaskQuery().taskCandidateUser("newLeader2").singleResult();
		if (task == null) {
			System.err.println("newLeader2 Task Query FAIL");
			return;
		}
		taskService.claim(task.getId(), "newLeader2");

		// 流程取消
		runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "强制取消");
		this.listTask();

		task = taskService.createTaskQuery().taskCandidateUser("newLeader1").singleResult();
		if (task == null) {
			System.out.println("已完成的任务无法通过taskCandidateUser查询到");
		}
		task = taskService.createTaskQuery().taskCandidateUser("newLeader2").singleResult();
		if (task == null) {
			System.err.println("newLeader2 Task Query FAIL");
		}
	}

	public static void main(String[] args) {
		UserTaskTest at = new UserTaskTest();
		at.run();
	}
}
