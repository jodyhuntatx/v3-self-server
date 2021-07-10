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
public class ConjurServlet extends HttpServlet {
    /** Logger */
    private static final Logger logger = Logger.getLogger(ConjurServlet.class.getName());

  // +++++++++++++++++++++++++++++++++++++++++
  // Initialize connection to Conjur
  @Override
  public void init() {
    try {
      InputStream inputStream = getServletContext().getResourceAsStream(Config.propFileName);
      Config.loadConfigValues(inputStream);
    } catch (IOException e) {
      System.out.println("Exception: " + e);
    }

    String conjurUrl = Config.conjurUrl;
    String conjurAccount = Config.conjurAccount;
    ConjurJava.initConnection( conjurUrl, conjurAccount);

    // turn off all cert validation - FOR DEMO ONLY
    Config.disableCertValidation(); 

    java.lang.System.setProperty("https.protocols", "TLSv1.2");
  }

  // +++++++++++++++++++++++++++++++++++++++++
  // placeholder for basic auth filter
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)  
        throws ServletException, IOException {  
  
    response.setContentType("text/html");  
  }
  
} // CybrServlet
