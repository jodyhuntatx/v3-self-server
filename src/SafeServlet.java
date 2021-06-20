/*
 * Defines REST endpoints to:
 * - create safe in EPV
 */

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ###########################################
// GET = list safes
// POST = add safe
// PUT = update safe with LOB member and load corresponding Conjur sync policy.
// DELETE = delete safe and corresponding Conjur sync policy.
public class SafeServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(SafeServlet.class.getName());


  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    response.getOutputStream().println(PASJava.listSafes());
  } // list safes

  // +++++++++++++++++++++++++++++++++++++++++
  // add safe
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String vaultName = request.getParameter("vaultName");
    String safeName = request.getParameter("safeName");
    String lobName = request.getParameter("lobName");
    String cpmName = request.getParameter("cpmName");
		
    String addSafeResponse = PASJava.addSafe(safeName, cpmName);
    String addSafeMemberResponse = PASJava.addSafeMember(safeName, lobName);

    // generate policy - REST method accepts text - no need to create a file
    String policyText = "---\n"
                        + "- !policy\n"
                        + "  id: " + vaultName + "\n"
                        + "  body:\n"
                        + "  - !group " + lobName + "-admins\n"
                        + "  - !policy\n"
                        + "    id: " + lobName + "\n"
                        + "    owner: !group /" + vaultName + "/" + lobName + "-admins\n"
                        + "    body:\n"
                        + "    - !group " + safeName + "-admins\n"
                        + "    - !policy\n"
                        + "      id: " + safeName + "\n"
                        + "      body:\n"
                        + "      - !policy\n"
                        + "        id: delegation\n"
                        + "        owner: !group /" + vaultName + "/" + lobName + "/" + safeName + "-admins\n"
                        + "        body:\n"
                        + "        - !group consumers\n";

    // load policy using default "append" method
    String loadPolicyResponse = ConjurJava.loadPolicy("append", "root", policyText);

    response.getOutputStream().println("{"
					+ "\"addSafeResponse\": "
                                        + addSafeResponse
					+ ","
					+ "\"addSafeMemberResponse\": "
                                        + addSafeMemberResponse
                                        + ","
					+ "\"loadPolicyResponse\": "
                                        + loadPolicyResponse
                                        + "}"
                                        );
  } // add safe

  // +++++++++++++++++++++++++++++++++++++++++
  // update safe
  @Override
  public void doPut(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String vaultName = request.getParameter("vaultName");
    String safeName = request.getParameter("safeName");
    String lobName = request.getParameter("lobName");

    String addSafeMemberResponse = PASJava.addSafeMember(safeName, lobName);

    // generate policy - REST method accepts text - no need to create a file
    String policyText = "---\n"
                        + "- !policy\n"
                        + "  id: " + vaultName + "\n"
                        + "  body:\n"
                        + "  - !group " + lobName + "-admins\n"
                        + "  - !policy\n"
                        + "    id: " + lobName + "\n"
                        + "    owner: !group /" + vaultName + "/" + lobName + "-admins\n"
                        + "    body:\n"
                        + "    - !group " + safeName + "-admins\n"
                        + "    - !policy\n"
                        + "      id: " + safeName + "\n"
                        + "      body:\n"
                        + "      - !policy\n"
                        + "        id: delegation\n"
                        + "        owner: !group /" + vaultName + "/" + lobName + "/" + safeName + "-admins\n"
                        + "        body:\n"
                        + "        - !group consumers\n";

    // load policy using default "append" method
    String loadPolicyResponse = ConjurJava.loadPolicy("append", "root", policyText);

    response.getOutputStream().println("{"
					+ "\"addSafeMemberResponse\": "
					+ addSafeMemberResponse
					+ ","
					+ "\"loadPolicyResponse\": "
					+ loadPolicyResponse
					+ "}"
					);
  } // update safe

  // +++++++++++++++++++++++++++++++++++++++++
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String vaultName = request.getParameter("vaultName");
    String safeName = request.getParameter("safeName");
    String lobName = request.getParameter("lobName");

    String delSafeResponse = PASJava.deleteSafe(safeName);
    // Delete consumers group for safe
    String policyText = "---\n"
			+ "- !delete\n"
			+ "  record: !group " + vaultName
			+ "/" + lobName
			+ "/" + safeName
			+ "/delegation/consumers"
			+ "\n"
    			+ "- !delete\n"
			+ "  record: !group " + vaultName
			+ "/" + lobName
			+ "/" + safeName
			+ "-admins"
			+ "\n"
    			+ "- !delete\n"
			+ "  record: !policy " + vaultName
			+ "/" + lobName
			+ "/" + safeName;
    String delPolicyResponse = ConjurJava.loadPolicy("delete", "root", policyText);
    response.getOutputStream().println("{"
					+ "\"delSafeResponse\": "
					+ delSafeResponse
					+ ","
					+ "\"delPolicyResponse\": "
					+ delPolicyResponse
					+ "}"
					);
  } // delete safe

} // SafeServlet
