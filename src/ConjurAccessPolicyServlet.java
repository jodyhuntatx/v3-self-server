/*
 * Defines REST endpoints to:
 * - grant access to Conjur identity
 * - revoke access for Conjur identity 
 * Note: These endpoints can only be used for group roles defined under the policy, i.e. not roles created by the Synchronizer
 */

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ###########################################
// POST - grant access to Conjur identity
// DELETE - revoke access for Conjur identity 

public class ConjurAccessPolicyServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(ConjurAccessPolicyServlet.class.getName());

  // +++++++++++++++++++++++++++++++++++++++++
  // Grants group role name to Conjur host identity 
  // Note: this can only be used for group roles defined under the policy, i.e. not roles created by the Synchronizer
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String projectName = request.getParameter("projectName");
    String identityName = request.getParameter("identityName");
    String groupRoleName = request.getParameter("groupRoleName");
    String policyText = "- !grant\n  role: !group " + groupRoleName + "\n  member: !host " + identityName;
    logger.log(Level.INFO, "Granting the group role " + groupRoleName + " to identity " + identityName + " at policy branch " + projectName);
    String policyResult = ConjurJava.loadPolicy("append", projectName, policyText);
    response.getOutputStream().println(policyResult);
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // Revokes role for Conjur host identity in a project
  // Note: this can only be used for group roles defined under the policy, i.e. not roles created by the Synchronizer
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String projectName = request.getParameter("projectName");
    String identityName = request.getParameter("identityName");
    String groupRoleName = request.getParameter("groupRoleName");
    String policyText = "- !revoke\n  role: !group " + groupRoleName + "\n  member: !host " + identityName;
    logger.log(Level.INFO, "Revoking the group role " + groupRoleName + " to identity " + identityName);
    String policyResult = ConjurJava.loadPolicy("delete", projectName, policyText);
    response.getOutputStream().println(policyResult);
  }

} // ConjurAccessPolicyServlet
