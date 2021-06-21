/*
 * Defines REST endpoints to:
 * - create an identity in a project policy
 * - delete the identity from a project policy
 */

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ###########################################
// POST = append conjur identity consumers group to a project and grant Synchroniser consumers role to it
// DELETE = delete identity from a project

public class ConjurIdentityPolicyServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(ConjurIdentityPolicyServlet.class.getName());

  // +++++++++++++++++++++++++++++++++++++++++
  // Appends Conjur host identity with given name to project policy
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String projectName = request.getParameter("projectName");
    String identityName = request.getParameter("identityName");
    String policyText = "- !host " + identityName;
    logger.log(Level.INFO, "Appending host identity: " + policyText + " at policy branch: " + projectName);
    String policyResult = ConjurJava.loadPolicy("append", projectName, policyText);
    response.getOutputStream().println(policyResult);
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // Deletes identity from a project
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String projectName = request.getParameter("projectName");
    String identityName = request.getParameter("identityName");
    String policyText = "- !delete\n  record: !host " + identityName;
    logger.log(Level.INFO, "Deleting identity with: " + policyText + " at policy branch: " + projectName);
    String policyResult = ConjurJava.loadPolicy("delete", projectName, policyText);
    response.getOutputStream().println(policyResult);
  }

} // ConjurBasePolicyServlet
