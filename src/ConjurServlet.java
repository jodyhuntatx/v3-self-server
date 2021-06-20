/*
 * Defines REST endpoints to:
 */

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;

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
    String conjurUrl = "https://conjur-master-mac";
    String conjurAccount = "dev";
    ConjurJava.initConnection( conjurUrl, conjurAccount);

    // turn off all cert validation - FOR DEMO ONLY
    disableCertValidation(); 

    java.lang.System.setProperty("https.protocols", "TLSv1.2");
  }

  // +++++++++++++++++++++++++++++++++++++++++
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)  
        throws ServletException, IOException {  
  
    response.setContentType("text/html");  
  }
  
  // ==========================================
  // void disableCertValidation()
  //   from: https://nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
  //
  private static void disableCertValidation() {
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }
      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }
      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    }
    };
 
    // Install the all-trusting trust manager
    try {
	        SSLContext sc = SSLContext.getInstance("SSL");
        	sc.init(null, trustAllCerts, new java.security.SecureRandom());
        	HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch(NoSuchAlgorithmException e) {
		e.printStackTrace();
    } catch(KeyManagementException e) {
		e.printStackTrace();
    }

    // Create all-trusting host name verifier
    HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
    };
 
    // Install the all-trusting host verifier
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

  } // disableSSL
 
} // CybrServlet
