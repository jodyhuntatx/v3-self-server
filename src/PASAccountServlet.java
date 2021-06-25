/*
 * Defines REST endpoints to:
 * - get account in EPV
 * - create account in EPV
 * - delete account in EPV
 */

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ###########################################
// GET = list accounts
// POST = add account 
// DELETE = delete account
public class PASAccountServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(PASAccountServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String safeName = request.getParameter("safeName");
    response.getOutputStream().println(PASJava.getAccounts(safeName));
  }

  // +++++++++++++++++++++++++++++++++++++++++
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String safeName = request.getParameter("safeName");
    String accountName = request.getParameter("accountName");
    String platformId = request.getParameter("platformId");
    String address = request.getParameter("address");
    String userName = request.getParameter("userName");
    String secretType = request.getParameter("secretType");
    String secretValue = request.getParameter("secretValue");
		
    response.getOutputStream().println(PASJava.addAccount(safeName,accountName,platformId,address,userName,secretType,secretValue));
  }

  // +++++++++++++++++++++++++++++++++++++++++
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
    String accountId = request.getParameter("accountId");
		
    response.getOutputStream().println(PASJava.deleteAccount(accountId));
  }

} // AccountServlet
