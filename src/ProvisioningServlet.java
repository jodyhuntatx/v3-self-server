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

    String safeResponse = createSafe(accReqId);
//    String accountResponse = addAccounts(accReqId);
    String basePolicyResponse = createBasePolicy(accReqId);
    String safePolicyResponse = createSafePolicy(accReqId);
    String identityPolicyResponse = createIdentityPolicy(accReqId);
    String accessPolicyResponse = grantAccessPolicy(accReqId);

    response.getOutputStream().println("{"
					+ safeResponse + ","
					+ basePolicyResponse + ","
					+ safePolicyResponse + ","
					+ identityPolicyResponse + ","
					+ accessPolicyResponse + "}");
//					+ accountResponse + ","
  } // doPost
  
  // +++++++++++++++++++++++++++++++++++
  // Add a safe w/ Conjur synch policy
  private static String createSafe(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String safeResponse = "";
    try {
      String querySql = "SELECT safe_name, cpm_name, lob_name, vault_name FROM accessrequests WHERE id = ?";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return safeResponse;
  }

  // +++++++++++++++++++++++++++++++++++
  // Add specified accounts to the safe
  private static String addAccounts(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String accountResponse = "";
    String querySql = "SELECT ar.safe_name, ca.name,ca.platform_id,ca.address,ca.username,ca.secret_type "
		+ "FROM accessrequests ar, cybraccounts ca, projects pr "
		+ "WHERE pr.id = ca.project_id AND pr.id = ar.id AND ar.id = ?";
    try {
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return accountResponse;
  } // addAccounts

  // +++++++++++++++++++++++++++++++++++
  // Create Conjur base policy for project, per CyberArk PS best-practices
  private static String createBasePolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String basePolicyResponse = "";
    try {
      String querySql = "SELECT pr.name, pr.admin "
		+ "FROM projects pr, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return basePolicyResponse;
  } // createBasePolicy

  // +++++++++++++++++++++++++++++++++++
  // Create Conjur safe policy for project
  private static String createSafePolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String safePolicyResponse = "";
    try {
      String querySql = "SELECT pr.name, ar.vault_name, ar.lob_name, ar.safe_name "
		+ "FROM projects pr, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return safePolicyResponse;
  }

  // +++++++++++++++++++++++++++++++++++
  // Create Conjur identity policy for project
  private static String createIdentityPolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String identityPolicyResponse = "";
    try {
      String querySql =  "SELECT pr.name, appid.name "
		+ "FROM projects pr, appidentities appid "
		+ "WHERE appid.accreq_id = ? AND appid.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return identityPolicyResponse;
  }

  // +++++++++++++++++++++++++++++++++++
  // Grant safe/consumers group role to identity
  private static String grantAccessPolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String accessPolicyResponse = "";
    try {
      String querySql = "SELECT pr.name, appid.name, ar.safe_name "
		+ "FROM projects pr, appidentities appid, accessrequests ar "
		+ "WHERE ar.id = ? AND appid.accreq_id = ar.id AND ar.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return accessPolicyResponse;
  } //createAccessPolicy

  // +++++++++++++++++++++++++++++++++++++++++
  // Deletes safe consumers group for a project, removing access to accounts in the safe
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String accReqId = request.getParameter("accReqId");

    String accessPolicyResponse = revokeAccessPolicy(accReqId);
//    String identityPolicyResponse = deleteIdentityPolicy(accReqId);
//    String safePolicyResponse = deleteSafePolicy(accReqId);
//    String basePolicyResponse = deleteBasePolicy(accReqId);
//    String accountResponse = deleteAccounts(accReqId);
//    String safeResponse = deleteSafes(accReqId);

    response.getOutputStream().println("{"
					+ accessPolicyResponse
					+ "}");
//					+ basePolicyResponse + ","
//					+ safePolicyResponse + ","
//					+ identityPolicyResponse + ","
//					+ safeResponse + ","
//					+ accountResponse + ","
  } //doDelete

  // ++++++++++++++++++++++++++++++++
  // Revoke safe/consumers group role for identity
  private static String revokeAccessPolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String accessPolicyResponse = "";
    try {
      String querySql = "SELECT pr.name, appid.name, ar.safe_name "
		+ "FROM projects pr, appidentities appid, accessrequests ar "
		+ "WHERE ar.id = ? AND appid.accreq_id = ar.id AND ar.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return accessPolicyResponse;
  } // revokeAccessPolicy

  // +++++++++++++++++++++++++++++++++
  // Delete Conjur identity(s) for project
  private static String deleteIdentityPolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String identityPolicyResponse = "";
    try {
      String querySql =  "SELECT pr.name, appid.name "
		+ "FROM projects pr, appidentities appid "
		+ "WHERE appid.accreq_id = ? AND appid.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return identityPolicyResponse;
  } // deleteIdentityPolicy

  // +++++++++++++++++++++++++++++++++
  // Delete Conjur safe policy(s) for project
  private static String deleteSafePolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String safePolicyResponse = "";
    try {
      String querySql = "SELECT pr.name, ar.vault_name, ar.lob_name, ar.safe_name "
		+ "FROM projects pr, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return safePolicyResponse;
  } // deleteSafePolicy

  // ++++++++++++++++++++++++++++++++++
  // Delete Conjur base policy for project
  private static String deleteBasePolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String basePolicyResponse = "";
    try {
      String querySql = "SELECT pr.name, pr.admin "
		+ "FROM projects pr, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return basePolicyResponse;
  } // deleteBasePolicy

  // +++++++++++++++++++++++++++++++++++
  // Delete accounts from project safe(s)
  private static String deleteAccounts(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String accountResponse = "";
    try {
      String querySql = "SELECT ar.safe_name, ca.name,ca.platform_id,ca.address,ca.username,ca.secret_type "
		+ "FROM accessrequests ar, cybraccounts ca, projects pr "
		+ "WHERE pr.id = ca.project_id AND pr.id = ar.id AND ar.id = ?";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return accountResponse;
  } // deleteAccounts

  // ++++++++++++++++++++++++++++++
  // Delete safe and Conjur synch policy
  private static String deleteSafe(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String safeResponse = "";
    try {
      String querySql = "SELECT safe_name, cpm_name, lob_name, vault_name FROM accessrequests WHERE id = ?";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
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
    return safeResponse;
  } // deleteSafes

} // ProvisioningServlet
