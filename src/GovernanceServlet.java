/*
  Help on how to write a servlet that accepts json input payloads:
   https://stackoverflow.com/questions/3831680/httpservletrequest-get-json-post-data

 MySQL best-practice to avoid dangling connections at server:
  - Create connection
  - Create cursor/prepared statement
  - Create Query string
  - Execute the query
  - Commit the query
  - Close cursor/prepared statement
  - Close the connection
*/

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;

import com.google.gson.Gson;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ###########################################
public class GovernanceServlet extends HttpServlet {
  /** Logger */
  private static final Logger logger = Logger.getLogger(GovernanceServlet.class.getName());

  private static Connection dbConn = null;

  // +++++++++++++++++++++++++++++++++++++++++
  // Initialize values in case no one else has
  @Override
  public void init() {
    try {
      InputStream inputStream = getServletContext().getResourceAsStream(Config.propFileName);
      Config.loadConfigValues(inputStream);
    } catch (IOException e) {
      System.out.println("Exception: " + e);
    } 
  }

  // +++++++++++++++++++++++++++++++++++++++++
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

    String jsonArrayName = "";
    // connect to database
    try {
      GovernanceServlet.dbConn = DriverManager.getConnection(Config.appGovDbUrl,
							   Config.appGovDbUser,
							   Config.appGovDbPassword);
      GovernanceServlet.dbConn.setAutoCommit(false);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Error connecting to appgovdb.");
      e.printStackTrace();
    }

    Connection conn =  GovernanceServlet.dbConn;
    String querySql = "";
    PreparedStatement prepStmt = null;
    String returnJson = "";
    try {
      // Get distinct provisioned project IDs
      querySql = "SELECT DISTINCT ar.project_id, pr.name"
		+ " FROM accessrequests ar, projects pr"
		+ " WHERE ar.provisioned AND NOT ar.revoked"
		+ " AND ar.project_id = pr.id";
      prepStmt = conn.prepareStatement(querySql);
      ResultSet rsPr = prepStmt.executeQuery();
      String prJson = "";
      while(rsPr.next() ) {		// for each provisioned project ID
        if(prJson != "") {
          prJson = prJson + ",";
	}
        int prId = rsPr.getInt(1);
        String prName = rsPr.getString(2);
        querySql = "SELECT appid.name, ca.resource_type, ca.resource_name, ca.username, sf.name"
		+ " FROM appidentities appid, accessrequests ar, safes sf, cybraccounts ca"
		+ " WHERE ar.provisioned AND NOT ar.revoked"
		+ " AND ar.project_id = ?"
		+ " AND appid.project_id = ?"
		+ " AND ar.app_id = appid.id"
		+ " AND ar.safe_id = ca.safe_id"
		+ " AND ca.safe_id = sf.id";
        logger.log(Level.INFO, "Provisioned projects query:"
                                + "\n  query template: " + querySql
				+ "\n  projectId: " + Integer.toString(prId));
        prepStmt = conn.prepareStatement(querySql);
        prepStmt.setInt(1, prId);
        prepStmt.setInt(2, prId);
        ResultSet rsId = prepStmt.executeQuery();
        String idJson = "";
        while(rsId.next() ) {		// for app identity in project
          if(idJson != "") {
            idJson = idJson + ",";
	  }
          String idRecord = "{\"appId\": \"" + rsId.getString(1) + "\""
			+ ",\"resourceType\": \"" + rsId.getString(2) + "\""
			+ ",\"resourceName\": \"" + rsId.getString(3) + "\""
			+ ",\"username\": \"" + rsId.getString(4) + "\""
			+ ",\"safeName\": \"" + rsId.getString(5) + "\"}";
	  idJson = idJson + idRecord;
	}
        prJson = prJson + "{\"projectName\": \"" + prName + "\",\n"
			+ "\"identities\": [" + idJson + "]}";
      }
      returnJson = "{\"projects\": [" + prJson + "]}";
      conn.commit();
      prepStmt.close();
    } catch (SQLException e) {
      logger.log(Level.INFO, "Error id identity access query:\n  query template: " + querySql);
      e.printStackTrace();
    }

    // close the database connection
    try {
      GovernanceServlet.dbConn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    response.getOutputStream().println(returnJson);

  } // doGet

} // GovernanceServlet
