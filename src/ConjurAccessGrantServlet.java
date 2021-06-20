/*
 * Defines REST endpoints to:
 * - grant access Conjur identity
 * - revoke access to Conjur identity 
 # NOTE: All role grants & revocations are applied at root - use fully qualified names for role & identity
 */

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ###########################################
// POST - grant access Conjur identity
// DELETE - revoke access to Conjur identity 

public class ConjurAccessGrantServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(ConjurAccessGrantServlet.class.getName());

  // +++++++++++++++++++++++++++++++++++++++++
  // Grants group role name to Conjur host identity 
  // NOTE: All role grants are applied at root - use fully qualified names for role & identity
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String identityName = request.getParameter("identityName");
    String groupRoleName = request.getParameter("groupRoleName");
    logger.log(Level.INFO, "Granting the group role " + groupRoleName + " to identity " + identityName);
    String policyResult = ConjurJava.loadPolicy("append", "root", "- !grant\n  role: !group " + groupRoleName + "\n  member: !host " + identityName);
    response.getOutputStream().println(policyResult);
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // Deletes Conjur host identity with given name at policy branch
  // NOTE: All role revocations are applied at root - use fully qualified names for role & identity
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String identityName = request.getParameter("identityName");
    String groupRoleName = request.getParameter("groupRoleName");
    logger.log(Level.INFO, "Revoking the group role " + groupRoleName + " to identity " + identityName);
    String policyResult = ConjurJava.loadPolicy("delete", "root", "- !revoke\n  role: !group " + groupRoleName + "\n  member: !host " + identityName);
    response.getOutputStream().println(policyResult);
  }

} // ConjurAccessGrantServlet
