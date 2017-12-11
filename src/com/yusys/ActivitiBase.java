package com.yusys;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

public class ActivitiBase {

	protected String processDefinitionId;
	
	protected RepositoryService repositoryService;

	protected RuntimeService runtimeService;

	protected TaskService taskService;

	protected HistoryService historyService;
	
	protected IdentityService identityService;

	private static Properties loadConfig(String configFilePath) throws IOException {
		Properties prop = new Properties();
		Reader reader = new InputStreamReader(new FileInputStream(configFilePath), "UTF-8");
		prop.load(reader);
		reader.close();
		return prop;
	}

	private void addTaskInfoToMap(Map<String, List<TaskInfo>> allTaskMap, TaskInfo task) {
		String processInstanceId = task.getProcessInstanceId();
		if (! allTaskMap.containsKey(processInstanceId)) {
			allTaskMap.put(processInstanceId, new ArrayList<TaskInfo>());
		}
		List<TaskInfo> taskInfoList = allTaskMap.get(processInstanceId);
		if (taskInfoList.size() > 0 && taskInfoList.get(taskInfoList.size() - 1).getId().equals(task.getId())) {
			taskInfoList.set(taskInfoList.size() - 1, task);
		} else {
			taskInfoList.add(task);
		}
	}

	private void removeUserTask(List<UserTask> userTaskList, TaskInfo taskInfo) {
		for (int i = 0; i < userTaskList.size(); i ++) {
			if (userTaskList.get(i).getName().equals(taskInfo.getName())) {
				userTaskList.remove(i);
				break;
			}
		}
	}

	protected void listTask() {
		List<UserTask> userTaskList = new ArrayList<UserTask>();
		BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
		if (model != null) {
			Collection<FlowElement> flowElements = model.getMainProcess().getFlowElements();
			for (FlowElement e : flowElements) {
				if (e instanceof UserTask) {
					userTaskList.add((UserTask)e);
				}
			}
		}
		System.out.println();
		
		Map<String, String> businessKeyMap = new HashMap<String, String>();
		List<HistoricProcessInstance> hpiList = historyService.createHistoricProcessInstanceQuery()
				.processDefinitionId(processDefinitionId)
				.list();
		for (HistoricProcessInstance hpi : hpiList) {
			businessKeyMap.put(hpi.getId(), hpi.getBusinessKey());
		}

		List<ProcessInstance> piList = runtimeService.createProcessInstanceQuery()
				.processDefinitionId(processDefinitionId)
				.list();
		for (ProcessInstance pi : piList) {
			String businessKey = businessKeyMap.get(pi.getId());
			if (businessKey == null) {
				businessKeyMap.put(pi.getId(), pi.getBusinessKey());
			} else if (! businessKey.equals(pi.getBusinessKey())) {
				System.err.println("Error: businessKey different");
			}
		}

		Map<String, List<TaskInfo>> allTaskMap = new HashMap<String, List<TaskInfo>>();

		List<HistoricTaskInstance> htiList = historyService.createHistoricTaskInstanceQuery()
				.processDefinitionId(processDefinitionId)
				.orderByTaskCreateTime().asc().list();
		if (CollectionUtils.isNotEmpty(htiList)) {
			for (HistoricTaskInstance task : htiList) {
				addTaskInfoToMap(allTaskMap, task);
			}
		}
		
		List<Task> taskList = taskService.createTaskQuery().processDefinitionId(processDefinitionId)
				.active().list();
		if (CollectionUtils.isNotEmpty(taskList)) {
			for (Task task : taskList) {
				addTaskInfoToMap(allTaskMap, task);
			}
		}

		for (Iterator<String> it = allTaskMap.keySet().iterator(); it.hasNext(); ) {
			String processInstanceId = it.next();
			System.out.println("BusinessKey: " + businessKeyMap.get(processInstanceId));
			System.out.println();
			List<UserTask> tempUserTaskList = new ArrayList<UserTask>(userTaskList);
			List<TaskInfo> taskInfoList = allTaskMap.get(processInstanceId);
			for (TaskInfo taskInfo : taskInfoList) {
				System.out.println("TaskId: " + taskInfo.getId());
				System.out.println("TaskName: " + taskInfo.getName());
				if (taskInfo.getOwner() != null) {
					System.out.println("TaskOwner: " + taskInfo.getOwner());
				}
				if (! (taskInfo instanceof HistoricTaskInstance)) {
					List<IdentityLink> identityLinkList = taskService.getIdentityLinksForTask(taskInfo.getId());
					if (CollectionUtils.isNotEmpty(identityLinkList)) {
						for (IdentityLink identityLink : identityLinkList) {
							if (! StringUtils.isEmpty(identityLink.getUserId())) {
								System.out.println("TaskCandidateUser: " + identityLink.getUserId());
							}
							if (! StringUtils.isEmpty(identityLink.getGroupId())) {
								System.out.println("TaskCandidateGroup: " + identityLink.getGroupId());
							}
						}
					}
				}
				if (taskInfo.getAssignee() != null) {
					System.out.println("TaskAssignee: " + taskInfo.getAssignee());
				}
				System.out.println("TaskProcessDefinitionId: " + taskInfo.getProcessDefinitionId());
				System.out.println("TaskProcessInstanceId: " + taskInfo.getProcessInstanceId());
				if (taskInfo instanceof HistoricTaskInstance) {
					HistoricTaskInstance hti = (HistoricTaskInstance)taskInfo;
					if (hti.getEndTime() != null) {
						System.out.println("TaskEndTime: " + hti.getEndTime());
					}
					if (hti.getDeleteReason() != null) {
						System.out.println("TaskDeleteReason: " + hti.getDeleteReason());
					}
				}
				if (taskInfo instanceof HistoricTaskInstance) {
					List<HistoricVariableInstance> hviList = historyService.createHistoricVariableInstanceQuery()
							.processInstanceId(taskInfo.getProcessInstanceId())
							.list();
					if (CollectionUtils.isNotEmpty(hviList)) {
						System.out.println("HistoryServiceVariables:");
						for (HistoricVariableInstance hvi : hviList) {
							System.out.println("\t" + hvi.getVariableName() + " = " + hvi.getValue());
						}
					}
				} else {
					Map<String, Object> variables = taskService.getVariables(taskInfo.getId());
					if (MapUtils.isNotEmpty(variables)) {
						System.out.println("TaskServiceVariables:");
						for (Iterator<Entry<String, Object>> it2 = variables.entrySet().iterator(); it2.hasNext(); ) {
							Entry<String, Object> entry = it2.next();
							System.out.println("\t" + entry.getKey() + " = " + entry.getValue());
						}
					}
				}
				System.out.println();
				removeUserTask(tempUserTaskList, taskInfo);
			}
			for (UserTask userTask : tempUserTaskList) {
				System.out.println("TaskId: " + userTask.getId());
				System.out.println("TaskName: " + userTask.getName());
				if (userTask.getOwner() != null) {
					System.out.println("TaskOwner: " + userTask.getOwner());
				}
				if (userTask.getAssignee() != null) {
					System.out.println("TaskAssignee: " + userTask.getAssignee());
				}
				List<String> groupIdList = userTask.getCandidateGroups();
				if (CollectionUtils.isNotEmpty(groupIdList)) {
					for (String groupId : groupIdList) {
						System.out.println("TaskCandidateGroup: " + groupId);
					}
				}
				List<String> userIdList = userTask.getCandidateUsers();
				if (CollectionUtils.isNotEmpty(userIdList)) {
					for (String userId : userIdList) {
						System.out.println("TaskCandidateUser: " + userId);
					}
				}
			}
			System.out.println("-----------------------------------------------------------------------------");
			System.out.println();
		}
	}

	protected String initByBpmnFile(String bpmnFileName) {
		ProcessEngine processEngine = ProcessEngineConfiguration
				.createStandaloneInMemProcessEngineConfiguration()
				.buildProcessEngine();
		repositoryService = processEngine.getRepositoryService();
		runtimeService = processEngine.getRuntimeService();
		taskService = processEngine.getTaskService();
		historyService = processEngine.getHistoryService();

		Deployment deployment = repositoryService.createDeployment()
				.addClasspathResource(bpmnFileName).deploy();
		if (deployment == null) {
			System.err.println("Deployment FAIL");
			System.exit(-1);
		}
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
		if (processDefinition == null) {
			System.err.println("ProcessDefinition Query FAIL");
			System.exit(-1);
		}
		processDefinitionId = processDefinition.getId();
		System.out.println("processDefinition.id = " + processDefinitionId);
		System.out.println("processDefinition.key = " + processDefinition.getKey());
		
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("logicSysNo", "rpt");
		variables.put("orgNo", "010010");
		variables.put("applyUser", "employee1");
		variables.put("days", 3);
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
				processDefinition.getKey(), variables);
		if (processInstance == null) {
			System.err.println("StartProcessInstanceByKey FAIL");
			System.exit(-1);
		}
		return processInstance.getId();
	}
	
	protected void initByConfigFile(String configFilePath, String processDefinitionKey) throws IOException {
		Properties prop = loadConfig(configFilePath);

		ProcessEngine processEngine = ProcessEngineConfiguration
				.createProcessEngineConfigurationFromResource("activiti.cfg.xml")
				.setJdbcDriver(prop.getProperty("jdbc.driverClassName"))
				.setJdbcUrl(prop.getProperty("jdbc.url"))
				.setJdbcUsername(prop.getProperty("jdbc.username"))
				.setJdbcPassword(prop.getProperty("jdbc.password"))
				.buildProcessEngine();
		repositoryService = processEngine.getRepositoryService();
		runtimeService = processEngine.getRuntimeService();
		taskService = processEngine.getTaskService();
		historyService = processEngine.getHistoryService();
		identityService = processEngine.getIdentityService();

		if (processDefinitionKey != null) {
			ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
					.processDefinitionKey(processDefinitionKey)
					.singleResult();
			if (processDefinition == null) {
				System.err.println("ProcessDefinition Query FAIL");
				System.exit(-1);
			}
			processDefinitionId = processDefinition.getId();
			System.out.println("processDefinition.id = " + processDefinitionId);
			System.out.println("processDefinition.key = " + processDefinition.getKey());
		}
	}
}
