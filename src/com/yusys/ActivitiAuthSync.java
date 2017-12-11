package com.yusys;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;

public class ActivitiAuthSync {

	private static Properties loadConfig(String configFilePath) throws IOException {
		Properties prop = new Properties();
		Reader reader = new InputStreamReader(new FileInputStream(configFilePath), "UTF-8");
		prop.load(reader);
		reader.close();
		return prop;
	}

	private void run(String[] args) throws IOException, ClassNotFoundException, SQLException {
		Properties prop = loadConfig(args[0]);
		Class.forName(prop.getProperty("jdbc.driverClassName"));

		ProcessEngine processEngine = ProcessEngineConfiguration
				.createProcessEngineConfigurationFromResource("activiti.cfg.xml")
				.setJdbcDriver(prop.getProperty("jdbc.driverClassName"))
				.setJdbcUrl(prop.getProperty("jdbc.url"))
				.setJdbcUsername(prop.getProperty("jdbc.username"))
				.setJdbcPassword(prop.getProperty("jdbc.password"))
				.buildProcessEngine();
		
		IdentityService identityService = processEngine.getIdentityService();
	
		Connection conn = DriverManager.getConnection(prop.getProperty("jdbc.url"),
				prop.getProperty("jdbc.username"),
				prop.getProperty("jdbc.password"));
		conn.setAutoCommit(false);
		Statement st = conn.createStatement();
		ResultSet rst = st.executeQuery("SELECT * FROM bione_user_info WHERE user_sts=1");
		while (rst.next()) {
			String userId = rst.getString("user_id");
			String userNo = rst.getString("user_no");
			User user = identityService.createUserQuery().userId(userId).singleResult();
			if (user == null) {
				user = identityService.newUser(userId);
				System.out.println("Add User: " + userNo);
			} else {
				System.out.println("Modify User: " + userNo);
			}
			user.setFirstName(userNo);
			user.setLastName(rst.getString("user_name"));
			identityService.saveUser(user);
		}
		rst.close();
		rst = st.executeQuery("SELECT * FROM bione_role_info WHERE role_sts=1");
		while (rst.next()) {
			String roleId = rst.getString("role_id");
			String roleName = rst.getString("role_name");
			Group group = identityService.createGroupQuery().groupId(roleId).singleResult();
			if (group == null) {
				group = identityService.newGroup(roleId);
				System.out.println("Add Group: " + roleName);
			} else {
				System.out.println("Modify Group: " + roleName);
			}
			group.setName(roleName);
			identityService.saveGroup(group);
		}
		rst.close();
		st.close();
		conn.rollback();
		conn.close();
	}

	public static void main(String[] args) {
		ActivitiAuthSync aas = new ActivitiAuthSync();
		try {
			aas.run(args);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
