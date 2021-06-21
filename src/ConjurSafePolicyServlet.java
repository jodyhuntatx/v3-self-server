/*
 * Defines REST endpoints to:
 * - create a safe consumers group in the project policy and grant Synchronizer safe consumers role to the group
 * - delete the safe consumers group in the project policy
 */

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ###########################################
// POST = append conjur safe consumers group to a project and grant Synchroniser consumers role to it
// DELETE = delete consumers group for a project

public class ConjurSafePolicyServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(ConjurSafePolicyServlet.class.getName());

  // +++++++++++++++++++++++++++++++++++++++++
  // Appends Conjur base policy with given name at root policy branch
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String projectName = request.getParameter("projectName");
    String vaultName = request.getParameter("vaultName");
    String lobName = request.getParameter("lobName");
    String safeName = request.getParameter("safeName");

    // The safe/consumers group role is necessary because policies created by the Synchronizer can
    // only be applied at the root policy, necessitating root admin privileges. The safe/consumers role
    // in effect aliases the Synchronizer group role as a group under the policy. This also separates
    // Synchronizer-managed policies from the project.

    // first create group in policy
    String policyText = "- !group " + safeName + "/consumers";
    logger.log(Level.INFO, "Appending policy: " + policyText + " at policy branch: " + projectName);
    String groupResult = ConjurJava.loadPolicy("append", projectName, policyText);
 
    // then grant Synchronizer consumers group role to policy group (must be applied at root)
    policyText =   "- !grant\n"
		 + "  role: !group " + vaultName + "/" + lobName + "/" + safeName + "/delegation/consumers\n"
		 + "  member: !group " + projectName + "/" + safeName + "/consumers";
    logger.log(Level.INFO, "Appending policy: " + policyText + " at root policy branch.");
    String grantResult = ConjurJava.loadPolicy("append", "root", policyText);
    response.getOutputStream().println("{" + groupResult + "," + grantResult + "}");
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // Deletes safe consumers group for a project, removing access to accounts in the safe
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String projectName = request.getParameter("projectName");
    String safeName = request.getParameter("safeName");
    String policyText = "- !delete\n  record: !group " + safeName + "/consumers";
    logger.log(Level.INFO, "Deleting safe consumers group with: " + policyText + " at policy branch: " + projectName);
    String policyResult = ConjurJava.loadPolicy("delete", projectName, policyText);
    response.getOutputStream().println(policyResult);
  }

} // ConjurBasePolicyServlet
