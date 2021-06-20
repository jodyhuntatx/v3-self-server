/*
 * Defines REST endpoints to:
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

  // +++++++++++++++++++++++++++++++++++++++++
  // Initialize connection to database
  @Override
  public void init() {
    ProvisioningServlet.DB_URL = "jdbc:mysql://localhost/appgovdb";
    ProvisioningServlet.DB_USER = "root";
    ProvisioningServlet.DB_PASSWORD = "Cyberark1";
    ProvisioningServlet.CYBR_BASE_URL="http://localhost:8080/cybr";
  }

  // +++++++++++++++++++++++++++++++++++++++++
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)  
        throws ServletException, IOException {  
    String accReqId = request.getParameter("accReqId");

    // These 4 variables are used multiple times
    Connection conn = null;
    String querySql = "";
    PreparedStatement prepStmt = null;
    String requestUrl = "";

    // Add a safe w/ Conjur synch policy
    String safeResponse = "";
    try {
      conn = DriverManager.getConnection(ProvisioningServlet.DB_URL,
					 ProvisioningServlet.DB_USER,
					 ProvisioningServlet.DB_PASSWORD);
      querySql = "SELECT safe_name, cpm_name, lob_name, vault_name FROM accessrequests WHERE id = ?";
      prepStmt = conn.prepareStatement(querySql);
      prepStmt.setString(1, accReqId);
      ResultSet rs = prepStmt.executeQuery();
      rs.next(); 
      String safeName = rs.getString("safe_name");
      String cpmName = rs.getString("cpm_name");
      String lobName = rs.getString("lob_name");
      String vaultName = rs.getString("vault_name");

      requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/safes"
				+ "?safeName=" + safeName
                                + "&cpmName=" + cpmName
                                + "&lobName=" + lobName
                                + "&vaultName=" + vaultName;
      logger.log(Level.INFO, "Add safe: " + requestUrl);
      safeResponse = JavaREST.httpPost(requestUrl, "", "");
    } catch (SQLException e) {
      e.printStackTrace();
    }

    // Add an account to the safe
    String accountResponse = "";
    querySql = "SELECT ar.safe_name, ca.name,ca.platform_id,ca.address,ca.username,ca.secret_type "
		+ "FROM accessrequests ar, cybraccounts ca, projects pr "
		+ "WHERE pr.id = ca.project_id AND pr.id = ar.id AND ar.id = ?";
    try {
      conn = DriverManager.getConnection(ProvisioningServlet.DB_URL,
                                         ProvisioningServlet.DB_USER,
                                         ProvisioningServlet.DB_PASSWORD);
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
        requestUrl = ProvisioningServlet.CYBR_BASE_URL + "/accounts"
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

    response.getOutputStream().println("{" + safeResponse + "," + accountResponse + "}");

  // Create a Conjur identity
//  authnResponse=$($CURL --request POST --url "$BASE_URL/conjuridentities?identityName=$CONJUR_IDENTITY&policyBranch=root")

  // Grant role to Conjur identity
//  authnResponse=$($CURL --request POST --url "$BASE_URL/accessgrants?identityName=$CONJUR_IDENTITY&groupRoleName=$CONJUR_GROUP_ROLE")

  }
  
} // ProvisioningServlet
