/*
 * Defines REST endpoints to:
 */

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

// ###########################################
// init
// Login
// Logout
public class PASServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(PASServlet.class.getName());


  // +++++++++++++++++++++++++++++++++++++++++
  // Initialize connection to PAS
  @Override
  public void init() {
    try {
      InputStream inputStream = getServletContext().getResourceAsStream(Config.propFileName);
      Config.loadConfigValues(inputStream);
    } catch (IOException e) {
      System.out.println("Exception: " + e);
    }
    PASJava.initConnection(Config.pasIpAddress);
    Config.disableCertValidation();
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // place holder for basic auth
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)  
        throws ServletException, IOException {  
    response.setContentType("text/html");  
  }
  
} // CybrServlet
