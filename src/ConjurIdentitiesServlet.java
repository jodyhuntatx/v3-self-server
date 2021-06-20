/*
 * Defines REST endpoints to:
 * - create identity in Conjur
 * - delete identity in Conjur
 */

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ###########################################
// POST = create identity in Conjur
// DELETE = delete identity in Conjur

public class ConjurIdentitiesServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(ConjurIdentitiesServlet.class.getName());

  // +++++++++++++++++++++++++++++++++++++++++
  // creates Conjur host identity with given name at policy branch
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String identityName = request.getParameter("identityName");
    String policyBranch = request.getParameter("policyBranch");
    logger.log(Level.INFO, "Appending identity " + identityName + " to policy branch " + policyBranch);
    String policyResult = ConjurJava.loadPolicy("append", policyBranch, "- !host\n  id: " + identityName);
    response.getOutputStream().println(policyResult);
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // deletes Conjur host identity with given name at policy branch
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String identityName = request.getParameter("identityName");
    String policyBranch = request.getParameter("policyBranch");
    logger.log(Level.INFO, "Deleting identity " + identityName + " from policy branch " + policyBranch);
    String policyResult =
	ConjurJava.loadPolicy("delete", policyBranch, "- !delete\n  record: !host " + identityName);
    response.getOutputStream().println(policyResult);
  }

} // ConjurIdentitiesServlet
