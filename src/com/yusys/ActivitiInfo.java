package com.yusys;

import java.io.IOException;
import java.util.List;

import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Activiti工作流检查与配置程序<br>
 * <br>
 * 使用方式：<br>
 * java --cancel <ProcessInstanceId> <数据库配置文件> ActivitiInfo<br>
 * 或者：<br>
 * java --listdefine <ProcessDefinitionKey> <数据库配置文件> ActivitiInfo<br>
 * 或者：<br>
 * java --listtask <ProcessDefinitionKey> <数据库配置文件> ActivitiInfo<br>
 * <br>
 * 数据库配置文件格式诸如：<br>
 * <br>
 * jdbc.driverClassName = oracle.jdbc.driver.OracleDriver<br>
 * jdbc.url = jdbc:oracle:thin:@192.168.251.157:1521:ycorcl<br>
 * jdbc.username = yanshi<br>
 * jdbc.password = yanshi<br>
 */
public class ActivitiInfo extends ActivitiBase {

	private void cancel(String[] args, String processInstanceId) throws IOException {
		initByConfigFile(args[0], null);
		
		runtimeService.deleteProcessInstance(processInstanceId, "force delete");
	}

	private void listDefine(String[] args, String processDefinitionKey) throws IOException {
		initByConfigFile(args[0], processDefinitionKey);

		RepositoryServiceImpl repositoryServiceImpl = (RepositoryServiceImpl)repositoryService;
		ProcessDefinition processDefinition = repositoryServiceImpl.createProcessDefinitionQuery()
				.processDefinitionKey(processDefinitionKey)
				.singleResult();
		ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity)repositoryServiceImpl
				.getDeployedProcessDefinition(processDefinition.getId());

		// 获得当前流程定义模型的所有任务节点
		List<ActivityImpl> activitilist = processDefinitionEntity.getActivities();
		for (ActivityImpl activityImpl : activitilist) {
			System.out.println("TaskDefinitionKey = " + activityImpl.getId());
		}
	}

	private void listTask(String[] args, String processDefinitionKey) throws IOException {
		initByConfigFile(args[0], processDefinitionKey);

		listTask();
	}

	public static void main(String[] args) {
		ActivitiInfo ai = new ActivitiInfo();
		try {
			Options options = new Options();
			options.addOption(null, "listdefine", true, null);
			options.addOption(null, "listtask", true, null);
			options.addOption(null, "cancel", true, null);
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("listdefine")) {
				ai.listDefine(cmd.getArgs(), cmd.getOptionValue("listdefine"));
			} else if (cmd.hasOption("listtask")) {
				ai.listTask(cmd.getArgs(), cmd.getOptionValue("listtask"));
			} else if (cmd.hasOption("cancel")) {
				ai.cancel(cmd.getArgs(), cmd.getOptionValue("cancel"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
