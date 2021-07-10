import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;

/* DEBT - grep for DEBT to find known issues

In looking around for where to put the config file, it seemed simplest
to put it in the base directory for the servlets, versus classpath or 
other location.. It also seemed best to store these values in a class/structure.
However that servlet directory context is only known to servlets, so they 
have to open the file and pass the input stream to the loadConfigValues 
function here. Which is kinda goofy, and also means the Config struct may
get initialized more than once.

Merits revisiting, especially to secure the passwords. :) JH

*/
  

public class Config {
  /** Logger */
  private static final Logger logger = Logger.getLogger(Config.class.getName());

  public static String propFileName = "/WEB-INF/cybrselfserve.properties";

  public static String conjurAdminUser;
  public static String conjurAdminPassword;
  public static String conjurUrl;
  public static String conjurAccount;

  public static String pasAdminUser;
  public static String pasAdminPassword;
  public static String pasIpAddress;

  public static String appGovDbUrl;
  public static String appGovDbUser;
  public static String appGovDbPassword;

  public static String selfServeBaseUrl;

  public static void loadConfigValues(InputStream inputStream) throws IOException {
    try {
      Properties prop = new Properties();
      if (inputStream != null) {
	prop.load(inputStream);
      } else {
	throw new FileNotFoundException("property file '" + propFileName + "' not found.");
      }

      Config.conjurAdminUser = Config.getProperty(prop, "conjurAdminUser");
      Config.conjurAdminPassword = Config.getProperty(prop, "conjurAdminPassword");
      Config.conjurUrl = Config.getProperty(prop, "conjurUrl");
      Config.conjurAccount = Config.getProperty(prop, "conjurAccount");

      Config.pasAdminUser = Config.getProperty(prop, "pasAdminUser");
      Config.pasAdminPassword = Config.getProperty(prop, "pasAdminPassword");
      Config.pasIpAddress = Config.getProperty(prop, "pasIpAddress");

      Config.appGovDbUrl = Config.getProperty(prop, "appGovDbUrl");
      Config.appGovDbUser = Config.getProperty(prop, "appGovDbUser");
      Config.appGovDbPassword = Config.getProperty(prop, "appGovDbPassword"); 
      Config.selfServeBaseUrl = Config.getProperty(prop, "selfServeBaseUrl");

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error getting properties:", e);
      e.printStackTrace();
      
    } finally {
      inputStream.close();
    }
  } // init

  // ++++++++++++++++++++++++++++++++++++ 
  private static String getProperty(Properties prop, String propKey) throws IOException {
    String propValue = prop.getProperty(propKey);
    if ( propValue == null || propValue.trim().isEmpty() ) {
      throw new IOException("Null or empty value for property: '" + propKey + "'");
    }
    return propValue;
  } 


  // ==========================================
  // void disableCertValidation()
  //   from: https://nakov.com/blog/2009/07/16/disable-certificate-validation-in-java-ssl-connections/
  //
  public static void disableCertValidation() {
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

  } // disableCertValidation
}
