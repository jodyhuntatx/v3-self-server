/*
  Help on how to write a servlet that accepts json input payloads:
   https://stackoverflow.com/questions/3831680/httpservletrequest-get-json-post-data

 MySQL best-practice to avoid dangling connections at server:
  - Create connection
  - Create cursor
  - Create Query string
  - Execute the query
  - Commit the query
  - Close the cursor
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
import java.io.IOException;

import com.google.gson.Gson;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ###########################################
public class AppGovDbServlet extends HttpServlet {
  /** Logger */
  private static final Logger logger = Logger.getLogger(AppGovDbServlet.class.getName());

  private static String DB_URL = "";
  private static String DB_USER = "";
  private static String DB_PASSWORD = "";
  private static Connection dbConn = null;

  // +++++++++++++++++++++++++++++++++++++++++
  // Initialize connection to database
//DEBT - replace w/ calls to properties file
  @Override
  public void init() {
    String pasIp = "192.168.2.163";
    PASJava.initConnection( pasIp );

    AppGovDbServlet.DB_URL = "jdbc:mysql://conjurmaster2.northcentralus.cloudapp.azure.com/appgovdb?autoReconnect=true";
    AppGovDbServlet.DB_USER = "root";
    AppGovDbServlet.DB_PASSWORD = "Cyberark1";
  }

  // +++++++++++++++++++++++++++++++++++++++++
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

    String jb = "";
    String line = null;
    try {
      BufferedReader reader = request.getReader();
      while ((line = reader.readLine()) != null)
        jb = jb + line;
    } catch (Exception e) {
      logger.log(Level.INFO, "Error parsing POST data payload: " + jb);
    }

    // parse account json output into PASSafeList
    Gson gson = new Gson();
    AccessRequestParameters arParms = (AccessRequestParameters ) gson.fromJson(jb, AccessRequestParameters.class );

    // connect to database
    try {
      AppGovDbServlet.dbConn = DriverManager.getConnection(AppGovDbServlet.DB_URL,
                                                         AppGovDbServlet.DB_USER,
                                                         AppGovDbServlet.DB_PASSWORD);
      AppGovDbServlet.dbConn.setAutoCommit(false);
    } catch (SQLException e) {
      e.printStackTrace();
    }

    Connection conn =  AppGovDbServlet.dbConn;

    //Write project variables to projects table and get DB-assigned projectId for foreign keys
    String projectId = "";
    try {
      String querySql = "INSERT IGNORE INTO projects(name,admin) VALUES(?,?)";
      PreparedStatement prepStmt = conn.prepareStatement(querySql,Statement.RETURN_GENERATED_KEYS);
      prepStmt.setString(1, arParms.projectName);
      prepStmt.setString(2, arParms.projectName + "-admin");
      prepStmt.executeUpdate();
      conn.commit();

      // INSERT IGNORE may not have created a new project, so lastrowid may be null
      //   have to query for project name to get projectId
      prepStmt = conn.prepareStatement("SELECT id FROM projects WHERE name = ?");
      prepStmt.setString(1, arParms.projectName);
      ResultSet rs = prepStmt.executeQuery();
      if(rs.next()) {
        projectId = String.valueOf(rs.getInt(1));
      }

      logger.log(Level.INFO, "write project record:"
				+ "\n  query template: " + querySql
				+ "\n  values: " + arParms.projectName + ", " + arParms.projectName + "-admin"
				+ "\n  projectId: " + projectId);
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Write accessrequest variables to accessrequests table and get DB-assigned accReqId for foreign keys
    String accReqId = "";
    try {
      LocalDateTime currentDateTime = java.time.LocalDateTime.now();
      String timeStamp = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
      String querySql = "INSERT IGNORE INTO accessrequests "
                + "(approved, project_id, datetime, environment, vault_name, safe_name, requestor, cpm_name, lob_name) "
                + "VALUES(?,?,?,?,?,?,?,?,?)";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, arParms.approved.toString());
      prepStmt.setString(2, projectId);
      prepStmt.setString(3, timeStamp);
      prepStmt.setString(4, arParms.environment);
      prepStmt.setString(5, arParms.pasVaultName);
      prepStmt.setString(6, arParms.pasSafeName);
      prepStmt.setString(7, arParms.requestor);
      prepStmt.setString(8, arParms.pasCpmName);
      prepStmt.setString(9, arParms.pasLobName);
      prepStmt.executeUpdate();
      prepStmt.close();
      conn.commit();

      // query for timestamp to get access request ID
      prepStmt = conn.prepareStatement("SELECT id FROM accessrequests WHERE datetime = ?");
      prepStmt.setString(1, timeStamp);
      ResultSet rs = prepStmt.executeQuery();
      if(rs.next()) {
        accReqId = String.valueOf(rs.getInt(1));
      }
      prepStmt.close();
      logger.log(Level.INFO, "write access request record:"
                                + "\n  query template: " + querySql
                                + "\n  values: "
				+ arParms.approved + ", "
				+ projectId + ", "
				+ timeStamp + ", "
				+ arParms.environment + ", "
				+ arParms.pasVaultName + ", "
				+ arParms.pasSafeName + ", "
				+ arParms.requestor + ", "
				+ arParms.pasCpmName + ", "
				+ arParms.pasLobName
				+ "\n  accReqId: " + accReqId);
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Write identity variables to appidentities table
    try {
      String querySql = "INSERT IGNORE INTO appidentities " 
                + "(project_id, accreq_id, name, auth_method) "
                + "VALUES(?,?,?,?)";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, projectId);
      prepStmt.setString(2, accReqId);
      prepStmt.setString(3, arParms.appIdName);
      prepStmt.setString(4, arParms.appAuthnMethod);
      prepStmt.executeUpdate();
      conn.commit();
      logger.log(Level.INFO, "write project record:"
                                + "\n  query template: " + querySql
                                + "\n  values: "
				+ projectId + ", " + accReqId + ", " + arParms.appIdName + ", " + arParms.appAuthnMethod);
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Get accounts in safe and write account data to cybraccounts table
    // This info is used to track app identity access to resources brokered through CyberArk accounts
    // Only database accounts with non-null database properties are supported.
    String pasAccountJson = PASJava.getAccounts(arParms.pasSafeName);
    gson = new Gson();				// parse json output into PASAccountList
    PASAccountList accList = (PASAccountList) gson.fromJson(pasAccountJson, PASAccountList.class );
    try {
      String querySql = "INSERT INTO cybraccounts "
			+ "(project_id, accreq_id, name, platform_id, secret_type, username, address, resource_type, resource_name)"
			+ "VALUES "
			+ "(?,?,?,?,?,?,?,?,?)";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      for(int i = 0; i < accList.value.length; i++) {

        // determine if a database based on account platform properties, skip if not a database or has no database named
        String resourceType = "";
        String resourceName = "";
        if(accList.value[i].platformAccountProperties != null) {
          if(accList.value[i].platformAccountProperties.Database != null) {
	    resourceType = "database";
	    resourceName = accList.value[i].platformAccountProperties.Database;
	  }
	}
        if(resourceType == "") {
          logger.log(Level.INFO, "Access for account \'" + accList.value[i].name + "\' will not be recorded. Only PAS database accounts with non-empty property values for 'database' are supported.");
	  continue;
	}

        prepStmt.setString(1, projectId);
        prepStmt.setString(2, accReqId);
        prepStmt.setString(3, accList.value[i].name);
        prepStmt.setString(4, accList.value[i].platformId);
        prepStmt.setString(5, accList.value[i].secretType);
        prepStmt.setString(6, accList.value[i].userName);
        prepStmt.setString(7, accList.value[i].address);
        prepStmt.setString(8, resourceType);
        prepStmt.setString(9, resourceName);
        prepStmt.executeUpdate();
        conn.commit();
        logger.log(Level.INFO, "write account records :"
                                + "\n  query template: " + querySql
                                + "\n  values: "
                                + projectId + ", "
                                + accReqId + ", "
				+ accList.value[i].name + ", "
				+ accList.value[i].platformId + ", "
				+ accList.value[i].secretType + ", "
				+ accList.value[i].userName + ", "
				+ accList.value[i].address + ", "
                                + resourceType + ", "
                                + resourceName);
      }
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // close the database connection
    try {
      AppGovDbServlet.dbConn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

  } // doPost

} // AppGovDbServlet

