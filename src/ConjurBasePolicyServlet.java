/*
 * Defines REST endpoints to:
 * - create base policy for project
 * - delete base policy for project
 */

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ###########################################
// POST = append conjur base policy for project
// DELETE = delete conjur base policy for project

public class ConjurBasePolicyServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(ConjurBasePolicyServlet.class.getName());

  // +++++++++++++++++++++++++++++++++++++++++
  // Appends Conjur base policy with given name at root policy branch
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String projectName = request.getParameter("projectName");
    String adminName = request.getParameter("adminName");

    // Base policy for project is applied at root to create:
    //  - admin role for project
    //  - project policy with admin role as owner
    String policyText =   "- !host " + adminName + "\n"
                        + "- !policy\n"
                        + "  id: " + projectName + "\n"
                        + "  owner: !host " + adminName + "\n"
                        + "  body:\n";

    logger.log(Level.INFO, "Appending base policy: " + policyText + " at root policy branch.");
    String policyResult = ConjurJava.loadPolicy("append", "root", policyText);
    response.getOutputStream().println(policyResult);
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // Deletes Conjur policy with given name at root
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String projectName = request.getParameter("projectName");
    String adminName = request.getParameter("adminName");
    String policyText = "- !delete\n  record: !host " + adminName + "\n- !delete\n  record: !policy " + projectName;
    logger.log(Level.INFO, "Deleting base policy with: " + policyText + " at root policy branch.");
    String policyResult = ConjurJava.loadPolicy("delete", "root", policyText);
    response.getOutputStream().println(policyResult);
  }

} // ConjurBasePolicyServlet
