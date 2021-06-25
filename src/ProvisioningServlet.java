/*
 * Defines REST endpoints to:
 *  - Provision an access request
 *  - Deprovision an access request
 */

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.sql.*;

// ###########################################
public class ProvisioningServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(ProvisioningServlet.class.getName());
    private static String DB_URL = "";
    private static String DB_USER = "";
    private static String DB_PASSWORD = "";
    private static String CYBR_BASE_URL="";
    private static Connection dbConn = null;

  // +++++++++++++++++++++++++++++++++++++++++
  // Initialize connection to database
  @Override
  public void init() {
    ProvisioningServlet.DB_URL = "jdbc:mysql://conjurmaster2.northcentralus.cloudapp.azure.com/appgovdb?autoReconnect=true";
    ProvisioningServlet.DB_USER = "root";
    ProvisioningServlet.DB_PASSWORD = "Cyberark1";
    ProvisioningServlet.CYBR_BASE_URL="http://localhost:8080/cybr";
    try {
      ProvisioningServlet.dbConn = DriverManager.getConnection(ProvisioningServlet.DB_URL,
								ProvisioningServlet.DB_USER,
								ProvisioningServlet.DB_PASSWORD);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // Provisions PAS safe, accounts, Conjur Sync policy, Conjur project policy, identities and access grants
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)  
        throws ServletException, IOException {  
    String accReqId = request.getParameter("accReqId");

    // These 4 variables are used multiple times
    Connection conn = ProvisioningServlet.dbConn;
    String querySql = "";
    PreparedStatement prepStmt = null;
    String requestUrl = "";

    // Add a safe w/ Conjur synch policy
    String safeResponse = "";
    try {
      querySql = "SELECT safe_name, cpm_name, lob_name, vault_name FROM accessrequests WHERE id = ?";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) { 		// unique access request id guarantees only one row returned
        String safeName = rs.getString("safe_name");
        String cpmName = rs.getString("cpm_name");
        String lobName = rs.getString("lob_name");
        String vaultName = rs.getString("vault_name");

        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/pas/safes"
  							+ "?safeName=" + safeName
                                			+ "&cpmName=" + cpmName
                                			+ "&lobName=" + lobName
                                			+ "&vaultName=" + vaultName;
        logger.log(Level.INFO, "Add safe: " + requestUrl);
        safeResponse = JavaREST.httpPost(requestUrl, "", "");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Add specified accounts to the safe
    String accountResponse = "";
    querySql = "SELECT ar.safe_name, ca.name,ca.platform_id,ca.address,ca.username,ca.secret_type "
		+ "FROM accessrequests ar, cybraccounts ca, projects pr "
		+ "WHERE pr.id = ca.project_id AND pr.id = ar.id AND ar.id = ?";
    try {
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while (rs.next()) {
        String safeName = rs.getString("ar.safe_name");
        String accountName = rs.getString("ca.name");
        String platformId = rs.getString("ca.platform_id");
        String accountAddress = rs.getString("ca.address");
        String accountUsername = rs.getString("ca.username");
        String accountSecretType = rs.getString("ca.secret_type");
        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/pas/accounts"
				+ "?safeName=" + safeName
                                + "&accountName=" + accountName
                                + "&platformId=" + platformId
                                + "&address=" + accountAddress
                                + "&userName=" + accountUsername
                                + "&secretType=" + accountSecretType
                                + "&secretValue=" + "RAndo498578x";
        logger.log(Level.INFO, "Add account: " + requestUrl);
	accountResponse = accountResponse + JavaREST.httpPost(requestUrl, "", "") + ",";
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Create Conjur base policy for project, per CyberArk PS best-practices
    String basePolicyResponse = "";
    try {
      querySql = "SELECT pr.name, pr.admin "
		+ "FROM projects pr, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String adminName = rs.getString("pr.admin");

        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/conjur/basepolicy"
   			                               + "?projectName=" + projectName
   			                               + "&adminName=" + adminName;

        logger.log(Level.INFO, "Add base project policy: " + requestUrl);
        basePolicyResponse = JavaREST.httpPost(requestUrl, "", "");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Create Conjur safe policy for project
    String safePolicyResponse = "";
    try {
      querySql = "SELECT pr.name, ar.vault_name, ar.lob_name, ar.safe_name "
		+ "FROM projects pr, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String vaultName = rs.getString("ar.vault_name");
        String lobName = rs.getString("ar.lob_name");
        String safeName = rs.getString("ar.safe_name");
        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/conjur/safepolicy"
     			                               + "?projectName=" + projectName
   			                               + "&vaultName=" + vaultName
   			                               + "&lobName=" + lobName
   			                               + "&safeName=" + safeName;
        logger.log(Level.INFO, "Add safe project policy: " + requestUrl);
        safePolicyResponse = JavaREST.httpPost(requestUrl, "", "");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Create Conjur identity policy for project
    String identityPolicyResponse = "";
    try {
      querySql =  "SELECT pr.name, appid.name "
		+ "FROM projects pr, appidentities appid "
		+ "WHERE appid.accreq_id = ? AND appid.project_id = pr.id";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String idName = rs.getString("appid.name");
        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/conjur/identitypolicy"
   			                               + "?projectName=" + projectName
   			                               + "&identityName=" + idName;
        logger.log(Level.INFO, "Add identity policy: " + requestUrl);
        identityPolicyResponse = identityPolicyResponse + JavaREST.httpPost(requestUrl, "", "");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Grant safe/consumers group role to identity
    String accessPolicyResponse = "";
    try {
      querySql = "SELECT pr.name, appid.name, ar.safe_name "
		+ "FROM projects pr, appidentities appid, accessrequests ar "
		+ "WHERE ar.id = ? AND appid.accreq_id = ar.id AND ar.project_id = pr.id";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String idName = rs.getString("appid.name");
        String safeName = rs.getString("ar.safe_name");
        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/conjur/accesspolicy"
   			                             + "?projectName=" + projectName
   			                             + "&identityName=" + idName
   			                             + "&groupRoleName=" + safeName + "/consumers";
        logger.log(Level.INFO, "Add access policy: " + requestUrl);
        accessPolicyResponse = accessPolicyResponse + JavaREST.httpPost(requestUrl, "", "");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    response.getOutputStream().println("{"
					+ safeResponse + ","
					+ accountResponse + ","
					+ basePolicyResponse + ","
					+ safePolicyResponse + ","
					+ identityPolicyResponse + ","
					+ accessPolicyResponse + "}");
  } // doPost
  
    // +++++++++++++++++++++++++++++++++++++++++
  // Deletes safe consumers group for a project, removing access to accounts in the safe
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String accReqId = request.getParameter("accReqId");

    // These 4 variables are used multiple times
    Connection conn = ProvisioningServlet.dbConn;
    String querySql = "";
    PreparedStatement prepStmt = null;
    String requestUrl = "";

    // Revoke safe/consumers group role for identity
    String accessPolicyResponse = "";
    try {
      querySql = "SELECT pr.name, appid.name, ar.safe_name "
		+ "FROM projects pr, appidentities appid, accessrequests ar "
		+ "WHERE ar.id = ? AND appid.accreq_id = ar.id AND ar.project_id = pr.id";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String idName = rs.getString("appid.name");
        String safeName = rs.getString("ar.safe_name");
        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/conjur/accesspolicy"
   			                             + "?projectName=" + projectName
   			                             + "&identityName=" + idName
   			                             + "&groupRoleName=" + safeName + "/consumers";
        logger.log(Level.INFO, "Delete access policy: " + requestUrl);
        accessPolicyResponse = accessPolicyResponse + Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Delete Conjur identity(s) for project
    String identityPolicyResponse = "";
    try {
      querySql =  "SELECT pr.name, appid.name "
		+ "FROM projects pr, appidentities appid "
		+ "WHERE appid.accreq_id = ? AND appid.project_id = pr.id";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String idName = rs.getString("appid.name");
        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/conjur/identitypolicy"
   			                               + "?projectName=" + projectName
   			                               + "&identityName=" + idName;
        logger.log(Level.INFO, "Delete identity policy: " + requestUrl);
        identityPolicyResponse = identityPolicyResponse + Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Delete Conjur safe policy(s) for project
    String safePolicyResponse = "";
    try {
      querySql = "SELECT pr.name, ar.vault_name, ar.lob_name, ar.safe_name "
		+ "FROM projects pr, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String vaultName = rs.getString("ar.vault_name");
        String lobName = rs.getString("ar.lob_name");
        String safeName = rs.getString("ar.safe_name");
        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/conjur/safepolicy"
     			                               + "?projectName=" + projectName
   			                               + "&vaultName=" + vaultName
   			                               + "&lobName=" + lobName
   			                               + "&safeName=" + safeName;
        logger.log(Level.INFO, "Add safe project policy: " + requestUrl);
        safePolicyResponse = Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Delete Conjur base policy for project
    String basePolicyResponse = "";
    try {
      querySql = "SELECT pr.name, pr.admin "
		+ "FROM projects pr, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String adminName = rs.getString("pr.admin");

        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/conjur/basepolicy"
   			                               + "?projectName=" + projectName
   			                               + "&adminName=" + adminName;

        logger.log(Level.INFO, "Add base project policy: " + requestUrl);
        basePolicyResponse = Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Delete accounts from project safe(s)
    String accountResponse = "";
    querySql = "SELECT ar.safe_name, ca.name,ca.platform_id,ca.address,ca.username,ca.secret_type "
		+ "FROM accessrequests ar, cybraccounts ca, projects pr "
		+ "WHERE pr.id = ca.project_id AND pr.id = ar.id AND ar.id = ?";
    try {
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while (rs.next()) {
        String safeName = rs.getString("ar.safe_name");
        String accountName = rs.getString("ca.name");
        String platformId = rs.getString("ca.platform_id");
        String accountAddress = rs.getString("ca.address");
        String accountUsername = rs.getString("ca.username");
        String accountSecretType = rs.getString("ca.secret_type");
        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/pas/accounts"
				+ "?safeName=" + safeName
                                + "&accountName=" + accountName
                                + "&platformId=" + platformId
                                + "&address=" + accountAddress
                                + "&userName=" + accountUsername
                                + "&secretType=" + accountSecretType
                                + "&secretValue=" + "RAndo498578x";
        logger.log(Level.INFO, "Add account: " + requestUrl);
	accountResponse = accountResponse + Integer.toString(JavaREST.httpDelete(requestUrl, "")) + ",";
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Delete safe and Conjur synch policy
    String safeResponse = "";
    try {
      querySql = "SELECT safe_name, cpm_name, lob_name, vault_name FROM accessrequests WHERE id = ?";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) { 		// unique access request id guarantees only one row returned
        String safeName = rs.getString("safe_name");
        String cpmName = rs.getString("cpm_name");
        String lobName = rs.getString("lob_name");
        String vaultName = rs.getString("vault_name");

        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/pas/safes"
  							+ "?safeName=" + safeName
                                			+ "&cpmName=" + cpmName
                                			+ "&lobName=" + lobName
                                			+ "&vaultName=" + vaultName;
        logger.log(Level.INFO, "Add safe: " + requestUrl);
        safeResponse = Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    response.getOutputStream().println("{"
					+ safeResponse + ","
					+ accountResponse + ","
					+ basePolicyResponse + ","
					+ safePolicyResponse + ","
					+ identityPolicyResponse + ","
					+ accessPolicyResponse + "}");
  } //doDelete

} // ProvisioningServlet
