/*
 * Defines REST endpoints to:
 *  - Provision an access request, includes creating empty safe & Conjur policies
 *  - Deprovision (revoke) an access request, only includes revoking access to safe

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Random;
import java.sql.*;

// ###########################################
public class ProvisioningServlet extends HttpServlet {
  /** Logger */
  private static final Logger logger = Logger.getLogger(ProvisioningServlet.class.getName());
  private static Connection dbConn = null;

  // +++++++++++++++++++++++++++++++++++++++++
  // Initialize config object from properties file
  @Override
  public void init() {
    try {
      InputStream inputStream = getServletContext().getResourceAsStream(Config.propFileName);
      Config.loadConfigValues(inputStream);
    } catch (IOException e) {
      System.out.println("Exception: " + e);
    }
    PASJava.initConnection(Config.pasIpAddress);
    ConjurJava.initConnection(Config.conjurUrl,Config.conjurAccount);
    Config.disableCertValidation();
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // This servlet makes calls to other servlets to provision resources 
  // implied by access requests, including:
  //  - PAS safe, if it doesn't exist
  //  - Conjur Synchronizer policy for safe
  //  - Conjur project base policy
  //  - Conjur safe consumer role
  //  - Conjur host identity 
  //  - Grant of safe consumer role to host.
  // 
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)  
        throws ServletException, IOException {  
    String accReqId = request.getParameter("accReqId");

    String pasToken = PASJava.logon(Config.pasAdminUser, Config.pasAdminPassword);
    String conjurApiKey = ConjurJava.authnLogin(Config.conjurAdminUser, Config.conjurAdminPassword);
    String conjurToken = ConjurJava.authenticate(Config.conjurAdminUser, conjurApiKey);
    if ( Objects.isNull(pasToken) || Objects.isNull(conjurToken) ) {
      throw new ServletException("Error authenticating, pasToken: "+pasToken+", conjurToken: "+conjurToken);
    }
    try {
      ProvisioningServlet.dbConn = DriverManager.getConnection(Config.appGovDbUrl,
								Config.appGovDbUser,
								Config.appGovDbPassword);
    } catch (SQLException e) {
      e.printStackTrace();
    }

    String safeResponse = createSafe(accReqId);	// creates empty safe if not exists 
						// also sets up Conjur sync policy
    String basePolicyResponse = createBasePolicy(accReqId);
    String safePolicyResponse = createSafePolicy(accReqId);
    String identityPolicyResponse = createIdentityPolicy(accReqId);
    String accessPolicyResponse = grantAccessPolicy(accReqId);

    // mark accessrequest provisioned
    String requestUrl = Config.selfServeBaseUrl+ "/appgovdb?accReqId="+ accReqId
					+ "&status=provisioned";
    String markedProvisionedResponse = JavaREST.httpPut(requestUrl, "", "");

    logger.log(Level.INFO, "Add safe: "
		+ "\n  safeResponse:" + safeResponse + ","
		+ "\n  basePolicyResponse: " + basePolicyResponse
		+ "\n  safePolicyResponse: " + safePolicyResponse
		+ "\n  identityPolicyResponse: " + identityPolicyResponse 
		+ "\n  accessPolicyResponse: " + accessPolicyResponse
		+ "\n  markedProvisionedResponse: " + markedProvisionedResponse);

    response.getOutputStream().println("{"
	+ "\"safeResponse\": " + safeResponse + ", \""
	+ "\nbasePolicyResponse\": \"" + basePolicyResponse + ", \""
	+ "\nsafePolicyResponse\": \"" + safePolicyResponse + ", \""
	+ "\nidentityPolicyResponse\": \"" + identityPolicyResponse + ", \""
	+ "\naccessPolicyResponse\": \"" + accessPolicyResponse + ", \""
	+ "\nmarkedProvisionedResponse\": \"" + markedProvisionedResponse
					+ "\"}");

  } // doPost
  
  // +++++++++++++++++++++++++++++++++++
  // Add a safe w/ Conjur synch policy
  private static String createSafe(String accReqId) {
    System.out.println("starting createSafe with accReqId: " + accReqId);
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String safeResponse = "";
    try {
      String querySql = "SELECT sf.name, sf.cpm_name, sf.vault_name, ar.lob_name "
			+ " FROM accessrequests ar, safes sf "
			+ " WHERE ar.id = ? AND ar.safe_id = sf.id";
      System.out.println("executing query: " + querySql + " with accReqId: " + accReqId);
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) { 		// unique access request id guarantees only one row returned
        String vaultName = rs.getString("sf.vault_name");
        String safeName = rs.getString("sf.name");
        String cpmName = rs.getString("sf.cpm_name");
        String lobName = rs.getString("ar.lob_name");
        requestUrl = Config.selfServeBaseUrl + "/pas/safes"
  						+ "?safeName=" + safeName
                               			+ "&cpmName=" + cpmName
                               			+ "&lobName=" + lobName
                               			+ "&vaultName=" + vaultName;
        logger.log(Level.INFO, "Add safe: " + requestUrl);
        safeResponse = JavaREST.httpPost(requestUrl, "", "");
      }
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return safeResponse;
  }

  // +++++++++++++++++++++++++++++++++++
  // Create Conjur base policy for project, per CyberArk PS best-practices
  private static String createBasePolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String basePolicyResponse = "";
    try {
      String querySql = "SELECT pr.name, pr.admin_user "
		+ "FROM projects pr, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String adminName = rs.getString("pr.admin_user");

        requestUrl = Config.selfServeBaseUrl + "/conjur/basepolicy"
   		                               + "?projectName=" + projectName
   		                               + "&adminName=" + adminName;

        logger.log(Level.INFO, "Add base project policy: " + requestUrl);
        basePolicyResponse = JavaREST.httpPost(requestUrl, "", "");
      }
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return basePolicyResponse;
  } // createBasePolicy

  // +++++++++++++++++++++++++++++++++++
  // Create Conjur safe consumers policy for project
  private static String createSafePolicy(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String safePolicyResponse = "";
    try {
      String querySql = "SELECT pr.name, sf.vault_name, sf.name, ar.lob_name "
		+ "FROM projects pr, accessrequests ar, safes sf "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id AND ar.safe_id = sf.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String vaultName = rs.getString("sf.vault_name");
        String safeName = rs.getString("sf.name");
        String lobName = rs.getString("ar.lob_name");
        requestUrl = Config.selfServeBaseUrl + "/conjur/safepolicy"
     		                               + "?projectName=" + projectName
   		                               + "&vaultName=" + vaultName
   		                               + "&lobName=" + lobName
   		                               + "&safeName=" + safeName;
        logger.log(Level.INFO, "Add safe project policy: " + requestUrl);
        safePolicyResponse = JavaREST.httpPost(requestUrl, "", "");
      }
      prepStmt.close();
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
		+ "FROM projects pr, appidentities appid, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id AND ar.app_id = appid.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String idName = rs.getString("appid.name");
        requestUrl = Config.selfServeBaseUrl + "/conjur/identitypolicy"
   		                               + "?projectName=" + projectName
   		                               + "&identityName=" + idName;
        logger.log(Level.INFO, "Add identity policy: " + requestUrl);
        identityPolicyResponse = identityPolicyResponse + JavaREST.httpPost(requestUrl, "", "");
      }
      prepStmt.close();
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
      String querySql = "SELECT pr.name, appid.name, sf.name "
		+ "FROM projects pr, appidentities appid, accessrequests ar, safes sf "
		+ "WHERE ar.id = ? AND ar.app_id = appid.id AND ar.project_id = pr.id AND ar.safe_id = sf.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String idName = rs.getString("appid.name");
        String safeName = rs.getString("sf.name");
        requestUrl = Config.selfServeBaseUrl + "/conjur/accesspolicy"
   		                             + "?projectName=" + projectName
   		                             + "&identityName=" + idName
   		                             + "&groupRoleName=" + safeName + "/consumers";
        logger.log(Level.INFO, "Add access policy: " + requestUrl);
        accessPolicyResponse = accessPolicyResponse + JavaREST.httpPost(requestUrl, "", "");
      }
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return accessPolicyResponse;
  } //createAccessPolicy

  // +++++++++++++++++++++++++++++++++++++++++
  // Revokes access grant and deletes safe consumers group for a project, thereby 
  // removing a project's access to accounts in the safe, and preventing the
  // project admin from re-granting access.
  // This is not a true inverse of provisioning as it does not delete identities, base policies,
  // or safes.  During development, it became clear that knowing exactly what to 
  // delete is a complex problem.
  // Those functions are preserved should they prove useful in the future.
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String accReqId = request.getParameter("accReqId");

    String pasToken = PASJava.logon(Config.pasAdminUser, Config.pasAdminPassword);
    String conjurApiKey = ConjurJava.authnLogin(Config.conjurAdminUser, Config.conjurAdminPassword);
    String conjurToken = ConjurJava.authenticate(Config.conjurAdminUser, conjurApiKey);
    if ( Objects.isNull(pasToken) || Objects.isNull(conjurToken) ) {
      throw new ServletException("Error authenticating, pasToken: "+pasToken+", conjurToken: "+conjurToken);
    }

    try {
      ProvisioningServlet.dbConn = DriverManager.getConnection(Config.appGovDbUrl,
								Config.appGovDbUser,
								Config.appGovDbPassword);
    } catch (SQLException e) {
      e.printStackTrace();
    }

    String accessPolicyResponse = revokeAccessPolicy(accReqId);
    String safePolicyResponse = deleteSafePolicy(accReqId);
//    String identityPolicyResponse = deleteIdentityPolicy(accReqId);
//    String basePolicyResponse = deleteBasePolicy(accReqId);
//    String accountResponse = deleteAccounts(accReqId);
//    String safeResponse = deleteSafes(accReqId);

    // mark access request revoked

    String requestUrl = Config.selfServeBaseUrl+ "/appgovdb?accReqId="+ accReqId
						+ "&status=revoked";
    String markedRevokedResponse = JavaREST.httpPut(requestUrl, "", "");

    response.getOutputStream().println("{"
					+ accessPolicyResponse + ", "
					+ markedRevokedResponse + "}");

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
      String querySql = "SELECT pr.name, appid.name, sf.name "
		+ "FROM projects pr, appidentities appid, accessrequests ar, safes sf "
		+ "WHERE ar.id = ? AND ar.app_id = appid.id AND ar.project_id = pr.id AND ar.safe_id = sf.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String idName = rs.getString("appid.name");
        String safeName = rs.getString("sf.name");
        requestUrl = Config.selfServeBaseUrl + "/conjur/accesspolicy"
   		                             + "?projectName=" + projectName
   		                             + "&identityName=" + idName
   		                             + "&groupRoleName=" + safeName + "/consumers";
        logger.log(Level.INFO, "Delete access policy: " + requestUrl);
        accessPolicyResponse = accessPolicyResponse + Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
      prepStmt.close();
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
		+ "FROM projects pr, appidentities appid, accessrequests ar "
		+ "WHERE ar.id = ? AND ar.project_id = pr.id AND ar.app_id = appid.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String idName = rs.getString("appid.name");
        requestUrl = Config.selfServeBaseUrl + "/conjur/identitypolicy"
   		                               + "?projectName=" + projectName
   		                               + "&identityName=" + idName;
        logger.log(Level.INFO, "Delete identity policy: " + requestUrl);
        identityPolicyResponse = identityPolicyResponse + Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
      prepStmt.close();
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
      String querySql = "SELECT pr.name, sf.name, sf.vault_name, ar.lob_name "
		+ " FROM accessrequests ar, projects pr, safes sf "
		+ " WHERE ar.id = ? AND ar.project_id = pr.id AND ar.safe_id = sf.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String safeName = rs.getString("sf.name");
        String vaultName = rs.getString("sf.vault_name");
        String lobName = rs.getString("ar.lob_name");
        requestUrl = Config.selfServeBaseUrl + "/conjur/safepolicy"
     		                               + "?projectName=" + projectName
   		                               + "&vaultName=" + vaultName
   		                               + "&lobName=" + lobName
   		                               + "&safeName=" + safeName;
        logger.log(Level.INFO, "Add safe project policy: " + requestUrl);
        safePolicyResponse = Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
      prepStmt.close();
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
      String querySql = "SELECT pr.name, pr.admin_user"
		+ " FROM projects pr, accessrequests ar"
		+ " WHERE ar.id = ? AND ar.project_id = pr.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) {
        String projectName = rs.getString("pr.name");
        String adminName = rs.getString("pr.admin_user");

        requestUrl = Config.selfServeBaseUrl + "/conjur/basepolicy"
   		                               + "?projectName=" + projectName
   		                               + "&adminName=" + adminName;
        logger.log(Level.INFO, "Add base project policy: " + requestUrl);
        basePolicyResponse = Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
      prepStmt.close();
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
      String querySql = "SELECT sf.name, ca.name, ca.platform_id, ca.address, ca.username,c a.secret_type"
		+ " FROM accessrequests ar, projects pr, safes sf, cybraccounts ca"
		+ " WHERE ar.id = ? AND ar.project_id = pr.id AND ar.safe_id = sf.id and ca.safe_id = sf.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while (rs.next()) {
        String safeName = rs.getString("sf.name");
        String accountName = rs.getString("ca.name");
        String platformId = rs.getString("ca.platform_id");
        String accountAddress = rs.getString("ca.address");
        String accountUsername = rs.getString("ca.username");
        String accountSecretType = rs.getString("ca.secret_type");
        requestUrl = Config.selfServeBaseUrl + "/pas/accounts"
						+ "?safeName=" + safeName
						+ "&accountName=" + accountName
                                		+ "&platformId=" + platformId
                                		+ "&address=" + accountAddress
                                		+ "&userName=" + accountUsername
                                		+ "&secretType=" + accountSecretType
                                		+ "&secretValue=" + "RAndo498578x";
        logger.log(Level.INFO, "Add account: " + requestUrl);
	accountResponse = accountResponse + Integer.toString(JavaREST.httpDelete(requestUrl, "")) + ",";
	prepStmt.close();
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
      String querySql = "SELECT sf.name, sf.cpm_name, sf.vault_name, ar.lob_name"
			+ " FROM accessrequests ar, safes sf WHERE ar.id = ? AND ar.safe_id = sf.id";
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while(rs.next()) { 		// unique access request id guarantees only one row returned
        String safeName = rs.getString("sf.name");
        String cpmName = rs.getString("sf.cpm_name");
        String vaultName = rs.getString("sf.vault_name");
        String lobName = rs.getString("ar.lob_name");

        requestUrl = Config.selfServeBaseUrl + "/pas/safes"
  						+ "?safeName=" + safeName
                               			+ "&cpmName=" + cpmName
                               			+ "&lobName=" + lobName
                               			+ "&vaultName=" + vaultName;
        logger.log(Level.INFO, "Add safe: " + requestUrl);
        safeResponse = Integer.toString(JavaREST.httpDelete(requestUrl, ""));
      }
      prepStmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return safeResponse;
  } // deleteSafes

  // +++++++++++++++++++++++++++++++++++
  // DEPRECATED - preserved for reference
  // Automated account creation is not supported
  // Add specified accounts to the safe 
  private static String addAccounts(String accReqId) {
    Connection conn = ProvisioningServlet.dbConn;
    String requestUrl = "";
    String accountResponse = "";
    String querySql = "SELECT sf.name, ca.name, ca.platform_id, ca.address, ca.username, ca.secret_type"
		+ " FROM accessrequests ar, cybraccounts ca, projects pr, safes sf "
		+ " WHERE ar.id = ? AND ar.project_id = pr.id AND ar.safe_id = sf.id AND ca.safe_id = sf.id";
    try {
      PreparedStatement prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      while (rs.next()) {
        String safeName = rs.getString("sf.name");
        String accountName = rs.getString("ca.name");
        String platformId = rs.getString("ca.platform_id");
        String accountAddress = rs.getString("ca.address");
        String accountUsername = rs.getString("ca.username");
        String accountSecretType = rs.getString("ca.secret_type");
        requestUrl = Config.selfServeBaseUrl + "/pas/accounts"
				+ "?safeName=" + safeName
                                + "&accountName=" + accountName
                                + "&platformId=" + platformId
                                + "&address=" + accountAddress
                                + "&userName=" + accountUsername
                                + "&secretType=" + accountSecretType
                                + "&secretValue=" + "RAndo498578x";
        logger.log(Level.INFO, "Add account: " + requestUrl);
	accountResponse = accountResponse + JavaREST.httpPost(requestUrl, "", "") + ",";
	prepStmt.close();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return accountResponse;
  } // addAccounts

} // ProvisioningServlet
