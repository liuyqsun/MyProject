package com.yusys;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Transport {

	private void run() throws ClassNotFoundException, SQLException {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		Connection conn1 = DriverManager.getConnection("jdbc:oracle:thin:@127.0.0.1:1521/orcl",
				"activiti", "activiti");
		conn1.setAutoCommit(false);
		Connection conn2 = DriverManager.getConnection("jdbc:oracle:thin:@192.168.251.158:1521/ycorcl",
				"rptfrs", "rptfrs");
		conn2.setAutoCommit(false);
		Statement st = conn1.createStatement();
		PreparedStatement pst = conn2.prepareStatement("update act_hi_procinst t set t.business_key_=? where proc_inst_id_=?");
		ResultSet rst = st.executeQuery("select proc_inst_id_,text_ from act_hi_varinst where name_='taskInstanceId'");
		while (rst.next()) {
			pst.setString(1, rst.getString(2));
			pst.setString(2, rst.getString(1));
			pst.executeUpdate();
		}
		pst.close();
		conn2.commit();
		conn2.close();
		st.close();
		conn1.close();
	}

	public static void main(String[] args) {
		Transport t = new Transport();
		try {
			t.run();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
