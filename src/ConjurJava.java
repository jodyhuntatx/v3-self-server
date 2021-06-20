import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class ConjurJava {
    /** Logger */
    private static final Logger logger = Logger.getLogger(ConjurJava.class.getName());

  /******************************************************************
   * 			PUBLIC MEMBERS
   *
   * void initJavaKeyStore(file,password) - opens Java key store containing server cert
   * void initConnection(url,account) - sets private members for appliance URL and account 
   * void getHealth() - basic Conjur health check
   * String authnLogin(uname,password) - Logs in human user with password, returns user's API key 
   * String authenticate(name,apikey) - authenticates with API key, sets private access token member and returns token
   * void setAccessToken(token) - sets private access token member, use with authn-k8s
   * String search(searchstr) - returns json array for variables where id or annotations match searchstr
   * String variableValue(varname) - gets variable value by name using private members
   * void loadPolicy(method,branchId,policyText) - loads policy text at branchId using method
   *
   ******************************************************************/

    // ===============================================================
    // void initJavaKeyStore() - opens Java key store containing server cert
    //
    public static void initJavaKeyStore(String _jksFile, String _jksPassword) {
	  System.setProperty("javax.net.ssl.trustStore", _jksFile);
	  System.setProperty("javax.net.ssl.trustStorePassword", _jksPassword);
	  System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }

    // ===============================================================
    // void initConnection() - sets private appliance URL and account members
    //
    public static void initConnection(String _applianceUrl, String _account) {
	conjurApplianceUrl = _applianceUrl;
	conjurAccount = _account;
    }

    // ===============================================================
    // void getHealth() - basic health check
    //
    public static void getHealth() {
	System.out.println( JavaREST.httpGet(conjurApplianceUrl + "/health", "") );
    }

    // ===============================================================
    // String authnLogin() - Logs in human user with password, returns user's API key 
    //
    public static String authnLogin(String _user, String _password) {
	String authHeader = "Basic " + base64Encode(_user + ":" + _password);
	String requestUrl = conjurApplianceUrl
				+ "/authn/" + conjurAccount + "/login";
	String authnApiKey = JavaREST.httpGet(requestUrl, authHeader);
	return authnApiKey;
    }

    // ===============================================================
    // String authenticate() - authenticates with API key, sets private access token member and returns token
    //
    public static String authenticate(String _authnLogin, String _apiKey) {
	String requestUrl = conjurApplianceUrl;
	try {
	    requestUrl = requestUrl + "/authn/" + conjurAccount + "/" 
				+ URLEncoder.encode(_authnLogin, "UTF-8")+ "/authenticate";
            logger.log(Level.INFO, "Authenticate requestUrl: " + requestUrl);
	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	}

	String rawToken = JavaREST.httpPost(requestUrl, _apiKey, "");
        logger.log(Level.INFO, "Raw token: " + rawToken);
	conjurAccessToken = base64Encode(rawToken);
        logger.log(Level.INFO, "Access token: " + conjurAccessToken);
	return conjurAccessToken;
    }

    // ===============================================================
    // void setAccessToken() - sets private access token member, use with authn-k8s
    //
    public static void setAccessToken(String _rawToken) {
	conjurAccessToken = base64Encode(_rawToken);
    }

    // ===============================================================
    // String search() - returns json array for variables where id or annotations match searchStr
    //
    public static String search(String _searchStr) {
	String authHeader = "Token token=\"" + conjurAccessToken + "\"";
	String requestUrl = conjurApplianceUrl
				+ "/resources/" + conjurAccount + "?kind=variable" 
				+ "&search=" + _searchStr.replace(" ","%20");
        logger.log(Level.INFO, "Search request: " + requestUrl);
  	return JavaREST.httpGet(requestUrl, authHeader);
    }

    // ===============================================================
    // String variableValue() - gets variable value by name using private members
    //
    public static String variableValue(String _varId) {
	String authHeader = "Token token=\"" + conjurAccessToken + "\"";
	String requestUrl = conjurApplianceUrl;
	try {
	    // Java URLEncoder encodes a space as + instead of %20 - Conjur REST doesn't accept +
	    requestUrl = requestUrl + "/secrets/" + conjurAccount 
				+ "/variable/" 
				+ URLEncoder.encode(_varId, "UTF-8").replace("+","%20");
            logger.log(Level.INFO, "Variable requestUrl: " + requestUrl);
	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	}
	return JavaREST.httpGet(requestUrl, authHeader);
    }

    // ===============================================================
    // String loadPolicy() - loads policy at a given branch using specfied method
    //
    public static String loadPolicy(String _method, String _branchId, String _policyText) {
	String authHeader = "Token token=\"" + conjurAccessToken + "\"";
	String requestUrl = conjurApplianceUrl + "/policies/" + conjurAccount + "/policy/" + _branchId;
	String loadPolicyResponse = "";
        String debugStr = "";

	switch(_method) {
	    case "delete":
		debugStr = "loadPolicy:\n"
			   + "  requestUrl: " + requestUrl + "\n"
			   + "  method: delete/patch\n"
			   + "policyText:\n"
			   + _policyText
			   + "\n";
                logger.log(Level.INFO, debugStr);
		loadPolicyResponse = JavaREST.httpPatch(requestUrl, _policyText, authHeader);
		break;
	    case "replace":
		System.out.println("\"replace/put\" policy load method not implemented.");
		break;
	    default:
		debugStr = "loadPolicy:\n"
			   + "  requestUrl: " + requestUrl + "\n"
			   + "  method: append/post\n"
			   + "policyText:\n"
			   + _policyText
			   + "\n";
                logger.log(Level.INFO, debugStr);
		loadPolicyResponse = JavaREST.httpPost(requestUrl, _policyText, authHeader);
	} // switch

	return loadPolicyResponse;
    
} // loadPolicy

  /******************************************************************
   * 			PRIVATE MEMBERS
   ******************************************************************/

    private static String conjurApplianceUrl;;
    private static String conjurAccount;
    private static String conjurAccessToken;

    // ===============================================================
    // String base64Encode() - base64 encodes argument and returns encoded string
    //
    private static String base64Encode(String input) {
	String encodedString = "";
	try {
	    encodedString = Base64.getEncoder().encodeToString(input.getBytes("utf-8"));
	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	}
	return encodedString;
    } // base64Encode

} // ConjurJava
