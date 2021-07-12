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
public class AppGovDbServlet extends HttpServlet {
  /** Logger */
  private static final Logger logger = Logger.getLogger(AppGovDbServlet.class.getName());

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
    PASJava.initConnection(Config.pasIpAddress);
    Config.disableCertValidation();
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
	whereFilter = "accreq.provisioned AND NOT accreq.revoked";
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
      AppGovDbServlet.dbConn = DriverManager.getConnection(Config.appGovDbUrl,
							   Config.appGovDbUser,
							   Config.appGovDbPassword);
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
      conn.commit();
      prepStmt.close();
      logger.log(Level.INFO, "Access request query:"
                                + "\n  query template: " + querySql
                                + "\n  returnJson: " + returnJson);
    } catch (SQLException e) {
      logger.log(Level.INFO, "Error querying access query:\n  query template: " + querySql);
      e.printStackTrace();
    }

    // close the database connection
    try {
      AppGovDbServlet.dbConn.close();
    } catch (SQLException e) {
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
      AppGovDbServlet.dbConn = DriverManager.getConnection(Config.appGovDbUrl,
							   Config.appGovDbUser,
							   Config.appGovDbPassword);
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
      conn.commit();
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
      conn.commit();
      prepStmt.close();
      logger.log(Level.INFO, "write identity record:"
                                + "\n  query template: " + querySql
                                + "\n  values: "
				+ projectId + ", " + arParms.appIdName + ", " + arParms.appAuthnMethod
				+ "\n  appId: " + appId);
      conn.commit();
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
      conn.commit();
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Write accessrequest variables to accessrequests table and get DB-assigned accReqId 
    long accReqId = 0;
    try {
      LocalDateTime currentDateTime = java.time.LocalDateTime.now();
      String timeStamp = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
      querySql = "INSERT IGNORE INTO accessrequests "
                + "(project_id, app_id, safe_id, datetime, approved, environment, lob_name, requestor) "
                + "VALUES(?,?,?,?,?,?,?,?)";
      prepStmt = conn.prepareStatement(querySql, Statement.RETURN_GENERATED_KEYS);
      prepStmt.setString(1, projectId);
      prepStmt.setString(2, appId);
      prepStmt.setString(3, safeId);
      prepStmt.setString(4, timeStamp);
      prepStmt.setString(5, arParms.approved.toString());
      prepStmt.setString(6, arParms.environment);
      prepStmt.setString(7, arParms.pasLobName);
      prepStmt.setString(8, arParms.requestor);
      int affectedRows = prepStmt.executeUpdate();
      if (affectedRows == 0) {
        throw new SQLException("Creating user failed, no rows affected.");
      }
      try (ResultSet generatedKeys = prepStmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          accReqId = generatedKeys.getLong(1);
        }
        else {
          throw new SQLException("Insert of access request failed, no ID obtained.");
        }
      }
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

    // autoprovision requests for dev environment safes
    // this should be a property of the resource, not the request
    if (arParms.environment.equals("dev")) {
        String requestUrl = Config.selfServeBaseUrl + "/provision"
   		                               + "?accReqId=" + Long.toString(accReqId);
        logger.log(Level.INFO, "Autoprovisioning for dev environment: " + requestUrl);
        String provisioningResponse = JavaREST.httpPost(requestUrl, "", "");
    }

/*
    Code below retrieves accounts in safe and write account property data to cybraccounts table.
    This is the only function that needs access to PAS, and therefore may not really
    belong in this servlet.
    This info is used to track app identity access to resources brokered through CyberArk accounts
    Currently only database accounts with non-null database properties are supported.
    This info might be useful for understanding the implication of granting the requested access.  JH
*/
    PASJava.logon(Config.pasAdminUser, Config.pasAdminPassword);
    String pasAccountJson = PASJava.getAccounts(arParms.pasSafeName);
    gson = new Gson();				// parse json output into PASAccountList
    PASAccountList accList = (PASAccountList) gson.fromJson(pasAccountJson, PASAccountList.class );
    try {
      querySql = "INSERT IGNORE INTO cybraccounts "
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
      conn.commit();
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

    response.getOutputStream().println("{\"accessRequestId\": "+Long.toString(accReqId)+"}");

  } // doPost

  // +++++++++++++++++++++++++++++++++++++++++
  // Updates accessrequest records when they change state, e.g. are approved, provisioned, etc.
  // Apparently this should really be "doPatch" but HttpServlet doesn't implement that.
  @Override
  public void doPut(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
    String accReqId = request.getParameter("accReqId");
    String accReqStatus = request.getParameter("status");
    String whereFilter = "accreq.id = " + accReqId;
    String setFields = "";
    String responseText = "";
    switch(accReqStatus.toLowerCase()) {
      case "approved":
	whereFilter = whereFilter + " AND NOT accreq.approved AND NOT accreq.rejected";
	setFields = "approved = 1";
	responseText = "approved";
	break;
      case "provisioned":
	whereFilter = whereFilter + " AND accreq.approved AND NOT accreq.rejected AND NOT accreq.provisioned";
	setFields = "provisioned = 1";
	responseText = "provisioned";
	break;
      case "revoked":
	whereFilter = whereFilter + " AND accreq.provisioned";
	setFields = "revoked = 1";
	responseText = "revoked";
	break;
      case "rejected":
	whereFilter = whereFilter;
	setFields = "rejected = 1";
	responseText = "rejected";
	break;
      default:
        response.sendError(500, "{Unknown status for access request: " + accReqStatus
				+ ".\nAccepted values: approve, provision, revoke, reject.}");
	return;
    }

    // connect to database
    try {
      AppGovDbServlet.dbConn = DriverManager.getConnection(Config.appGovDbUrl,
							   Config.appGovDbUser,
							   Config.appGovDbPassword);
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
      querySql = "UPDATE accessrequests accreq SET " + setFields + " WHERE " + whereFilter;
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.executeUpdate();
      conn.commit();
      prepStmt.close();
      logger.log(Level.INFO, "Access request update:\n  query template: " + querySql);
    } catch (SQLException e) {
      logger.log(Level.INFO, "Error updating access request:\n  query template: " + querySql);
      e.printStackTrace();
    }

    // close the database connection
    try {
      AppGovDbServlet.dbConn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    response.getOutputStream().println("{\"accReqId\": " + accReqId
					+ ",\"status\": \"" + responseText
					+"\"}");

  } // doPut

} // AppGovDbServlet

