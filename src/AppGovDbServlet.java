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
  public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

    String arFilter= request.getParameter("filter");
    String whereFilter = "";
    String jsonArrayName = "";
    switch(arFilter.toLowerCase()) {
      case "unapproved":
	whereFilter = "NOT accreq.approved AND NOT accreq.rejected";
	jsonArrayName = "unapproved";
	break;
      case "unprovisioned":
	whereFilter = "accreq.approved AND NOT accreq.rejected AND NOT accreq.provisioned";
	jsonArrayName = "unprovisioned";
	break;
      case "provisioned":
	whereFilter = "accreq.provisioned";
	jsonArrayName = "provisioned";
	break;
      case "revoked":
	whereFilter = "accreq.revoked";
	jsonArrayName = "revoked";
	break;
      case "rejected":
	whereFilter = "accreq.rejected";
	jsonArrayName = "rejected";
	break;
      default:
        response.sendError(500, "{Unknown access request filter: "+arFilter+".\nAccepted values: unapproved, unprovisioned, provisioned, revoked, rejected.}");
	return;
    }

    // connect to database
    try {
      AppGovDbServlet.dbConn = DriverManager.getConnection(AppGovDbServlet.DB_URL,
                                                         AppGovDbServlet.DB_USER,
                                                         AppGovDbServlet.DB_PASSWORD);
      AppGovDbServlet.dbConn.setAutoCommit(false);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Error connecting to appgovdb.");
      e.printStackTrace();
    }

    Connection conn =  AppGovDbServlet.dbConn;
    String querySql = "";
    PreparedStatement prepStmt = null;
    String returnJson = "";
    try {
      // Query for project/admin name to get project ID
      querySql = "SELECT proj.name, appid.name, safes.name, accreq.environment, accreq.datetime, accreq.id"
                 + " FROM projects proj, accessrequests accreq, appidentities appid, safes"
                 + " WHERE " + whereFilter
                 + " AND accreq.project_id = proj.id"
                 + " AND accreq.app_id = appid.id"
                 + " AND accreq.safe_id = safes.id";
      prepStmt = conn.prepareStatement(querySql);
      ResultSet rs = prepStmt.executeQuery();
      String arRecord = "";
      while(rs.next() ) {
        if(returnJson != "") {
          returnJson = returnJson + ",";
	}
        arRecord = "{	\"projectName\": \"" + rs.getString(1) + "\""
        		+ ",\"appId\": \"" + rs.getString(2) + "\""
			+ ",\"safeName\": \"" + rs.getString(3) + "\""
			+ ",\"environment\": \"" + rs.getString(4) + "\""
			+ ",\"dateTime\": \"" + rs.getString(5) + "\""
			+ ",\"accReqId\": " + rs.getString(6) + "}";
	returnJson = returnJson + arRecord;
      }
      returnJson = "{\"" + jsonArrayName + "\": [" + returnJson + "]}";
      prepStmt.close();
      logger.log(Level.INFO, "Access request query:"
                                + "\n  query template: " + querySql
                                + "\n  returnJson: " + returnJson);
    } catch (SQLException e) {
      logger.log(Level.INFO, "Error querying access query:\n  query template: " + querySql);
      e.printStackTrace();
    }
    response.getOutputStream().println(returnJson);

  } // doGet

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

    // parse account json output into AccessRequestParameters structure
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
    String querySql = "";
    PreparedStatement prepStmt = null;

    // Foreign-key dependencies require that project, identity, and safe records
    // are created first, then access requests and accounts

    // Write project variables to projects table and get DB-assigned projectId for foreign keys
    String projectId = "";
    try {
      querySql = "INSERT IGNORE INTO projects(name,admin_user) VALUES(?,?)"; // IGNORE duplicate index value
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, arParms.projectName);
      prepStmt.setString(2, arParms.projectName + "-admin");
      prepStmt.executeUpdate();
      conn.commit();

      // Query for project/admin name to get project ID
      querySql = "SELECT id FROM projects WHERE name = ? AND admin_user = ?";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, arParms.projectName);
      prepStmt.setString(2, arParms.projectName + "-admin");
      ResultSet rs = prepStmt.executeQuery();
      if(rs.next() ) {		// if something returned, get project ID
        projectId = String.valueOf(rs.getInt(1));
      }
      else {
	throw new SQLException("Unable to get project id after INSERT.");
      }
      prepStmt.close();
      logger.log(Level.INFO, "write project record:"
				+ "\n  query template: " + querySql
				+ "\n  values: " + arParms.projectName + ", " + arParms.projectName + "-admin"
				+ "\n  projectId: " + projectId);
    } catch (SQLException e) {
      logger.log(Level.INFO, "Error getting/adding project for name/admin: "+arParms.projectName +"/"+ arParms.projectName+"-admin"
				+ "\n  query template: " + querySql
				+ "\n  values: " + arParms.projectName + ", " + arParms.projectName + "-admin");
      e.printStackTrace();
    }


    // Write identity variables to appidentities table
    String appId = "";
    try {
      querySql = "INSERT IGNORE INTO appidentities(project_id, name, authn_method) VALUES(?,?,?)";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, projectId);
      prepStmt.setString(2, arParms.appIdName);
      prepStmt.setString(3, arParms.appAuthnMethod);
      prepStmt.executeUpdate();
      conn.commit();

      // Query for project_id & identity name to get appId
      prepStmt = conn.prepareStatement("SELECT id FROM appidentities WHERE project_id = ? AND name = ?");
      prepStmt.setString(1, projectId);
      prepStmt.setString(2, arParms.appIdName);
      ResultSet rs = prepStmt.executeQuery();
      if(rs.next() ) {		// if something returned, get project ID
        appId = String.valueOf(rs.getInt(1));
      }
      else {
	throw new SQLException("Unable to get app id after INSERT.");
      }
      prepStmt.close();
      logger.log(Level.INFO, "write identity record:"
                                + "\n  query template: " + querySql
                                + "\n  values: "
				+ projectId + ", " + arParms.appIdName + ", " + arParms.appAuthnMethod
				+ "\n  appId: " + appId);
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Write safe variables to safes table
    String safeId = "";
    try {
      querySql = "INSERT IGNORE INTO safes(name, vault_name, cpm_name) VALUES(?,?,?)";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, arParms.pasSafeName);
      prepStmt.setString(2, arParms.pasVaultName);
      prepStmt.setString(3, arParms.pasCpmName);
      prepStmt.executeUpdate();
      conn.commit();

      // Query for safe_name,vault_name to get safeId
      prepStmt = conn.prepareStatement("SELECT id FROM safes WHERE name = ? AND vault_name = ?");
      prepStmt.setString(1, arParms.pasSafeName);
      prepStmt.setString(2, arParms.pasVaultName);
      ResultSet rs = prepStmt.executeQuery();
      if(rs.next() ) {          // if something returned, get safe ID
        safeId = String.valueOf(rs.getInt(1));
      }
      else {
        throw new SQLException("Unable to get safe id after INSERT.");
      }
      logger.log(Level.INFO, "write safe record:"
                                + "\n  query template: " + querySql
                                + "\n  values: "
				+ arParms.pasSafeName + ", " + arParms.pasVaultName + ", " + arParms.pasCpmName
				+ "\n  safeId: " + safeId);
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Write accessrequest variables to accessrequests table and get DB-assigned accReqId for foreign keys
    try {
      LocalDateTime currentDateTime = java.time.LocalDateTime.now();
      String timeStamp = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
      querySql = "INSERT IGNORE INTO accessrequests "
                + "(project_id, app_id, safe_id, datetime, approved, environment, lob_name, requestor) "
                + "VALUES(?,?,?,?,?,?,?,?)";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, projectId);
      prepStmt.setString(2, appId);
      prepStmt.setString(3, safeId);
      prepStmt.setString(4, timeStamp);
      prepStmt.setString(5, arParms.approved.toString());
      prepStmt.setString(6, arParms.environment);
      prepStmt.setString(7, arParms.pasLobName);
      prepStmt.setString(8, arParms.requestor);
      prepStmt.executeUpdate();
      prepStmt.close();
      conn.commit();
      prepStmt.close();
      logger.log(Level.INFO, "write access request record:"
                                + "\n  query template: " + querySql
                                + "\n  values: "
				+ projectId + ", "
				+ appId + ", "
				+ safeId + ", "
				+ timeStamp + ", "
				+ arParms.approved + ", "
				+ arParms.environment + ", "
				+ arParms.pasLobName + ", "
				+ arParms.requestor);
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Get accounts in safe and write account data to cybraccounts table
    // This info is used to track app identity access to resources brokered through CyberArk accounts
    // Currently only database accounts with non-null database properties are supported.

    String pasAccountJson = PASJava.getAccounts(arParms.pasSafeName);
    gson = new Gson();				// parse json output into PASAccountList
    PASAccountList accList = (PASAccountList) gson.fromJson(pasAccountJson, PASAccountList.class );
    try {
      querySql = "INSERT INTO cybraccounts "
			+ "(safe_id, name, platform_id, secret_type, username, address, resource_type, resource_name)"
			+ "VALUES "
			+ "(?,?,?,?,?,?,?,?)";
      prepStmt = conn.prepareStatement(querySql);
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

        prepStmt.setString(1, safeId);
        prepStmt.setString(2, accList.value[i].name);
        prepStmt.setString(3, accList.value[i].platformId);
        prepStmt.setString(4, accList.value[i].secretType);
        prepStmt.setString(5, accList.value[i].userName);
        prepStmt.setString(6, accList.value[i].address);
        prepStmt.setString(7, resourceType);
        prepStmt.setString(8, resourceName);
        prepStmt.executeUpdate();
        conn.commit();
        logger.log(Level.INFO, "write account records :"
                                + "\n  query template: " + querySql
                                + "\n  values: "
                                + safeId + ", "
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

