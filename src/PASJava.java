import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

// NOTE: this uses all 2nd generation PAS REST APIs

/******************************************************************
 ******************************************************************
			PUBLIC STATIC METHODS
  Initialization:
  - void initConnection(serverHost)
  - JsonString logon(username,password)

  Platforms:
X - PASPlatform[] getPlatforms(filter)

  Safes:
X - JsonString listSafes()
  - JsonString addSafe(safeName,cpmName)
  - JsonString deleteSafe(safeName)
  - JsonString getSafeMembers(safeName)
  - JsonString addSafeMember(safeName,memberName)
  - JsonString removeSafeMember(safeName,memberName)

 Accounts:
  - JsonString addAccount(safeName,accountName,platformId,address,userName,secretType,secretValue)
  - JsonString getAccounts(safeName)
  - JsonString getAccountDetails(keyWords,safeName)
  - JsonString getAccountGroups(safeName)
  - JsonString getAccountGroupMembers(safeName)
  - JsonString deleteAccount(accountId)

******************************************************************
******************************************************************/

public class PASJava {
    /** Logger */
    private static final Logger logger = Logger.getLogger(PASJava.class.getName());


     public static Boolean DEBUG=false; // set to true for debug output


/*****************************************************************
 *****************************************************************
 **			     INITIALIZATION			**
 *****************************************************************
 *****************************************************************/

    // ===============================================================
    // void initConnection() - initializes base server URL
    //
    public static void initConnection(String _pasServerHost) {
	pasServerUrl = "https://" + _pasServerHost + "/passwordvault/api";
    } // initConnection


    // ===============================================================
    // logon(username,password) - logs user in and sets session token
    //
    public static String logon(String _user, String _password) {

	String requestUrl = pasServerUrl + "/auth/Cyberark/Logon";
	String bodyContent = "{"
				+ "\"username\":\"" + _user + "\","
				+ "\"password\":\"" + _password + "\""
			   + "}";

	// get session token and save w/o double quotes
	pasSessionToken = JavaREST.httpPost(requestUrl, bodyContent, "");
	if (pasSessionToken != null) {
	  pasSessionToken = pasSessionToken.replace("\"","");
        }

	if(PASJava.DEBUG) {
	    System.out.println("");
	    System.out.println("====== PASJava.login() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("bodyContent: " + bodyContent);
	    System.out.println("sessionToken: " + pasSessionToken);
	    System.out.println("=============================");
	    System.out.println("");
	}

	return pasSessionToken;

    } //logon

/*****************************************************************
 *****************************************************************
 **			     	PLATFORMS			**
 *****************************************************************
 *****************************************************************/

    // ===============================================================
    // JsonString getPlatforms(filter)
    //
    public static String getPlatforms(String _filter) {
        String requestUrl = pasServerUrl + "/platforms";
        String authHeader = pasSessionToken;

	if(_filter != null) {
	  requestUrl = requestUrl + "?" + _filter;
	}

        if(PASJava.DEBUG) {
	    System.out.println("requestUrl: " + requestUrl);
	}

        String platformResponse = JavaREST.httpGet(requestUrl, authHeader);

        if(PASJava.DEBUG) {
            System.out.println("Raw platform listing:");
            System.out.println(platformResponse);
            System.out.println("");
        }

	return platformResponse;

    } // getPlatforms

/*****************************************************************
 *****************************************************************
 **			     	SAFES				**
 *****************************************************************
 *****************************************************************/

    // ===============================================================
    // JsonString addSafe(safeName) - creates safe w/ given name
    //
    public static String addSafe(String _safeName, String _managingCpm) {

	String requestUrl = pasServerUrl + "/Safes";
	String authHeader = pasSessionToken;
	String bodyContent = "{"
				+ "\"numberOfDaysRetention\": 0,"
				+ "\"numberOfVersionsRetention\": null,"
				+ "\"oLACEnabled\": false,"
				+ "\"autoPurgeEnabled\": true,"
				+ "\"managingCPM\": \"" + _managingCpm + "\","
				+ "\"safeName\": \"" + _safeName + "\","
				+ "\"description\": \"Safe created by java driver.\","
				+ "\"location\": \"\""
			   + "}";

	if(PASJava.DEBUG) {
	    System.out.println("====== PASJava.addSafe() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("authHeader: " + authHeader);
	    System.out.println("bodyContent: " + bodyContent);
	    System.out.println("===================================");
	    System.out.println("");
	}

	String addSafeResponse = JavaREST.httpPost(requestUrl, bodyContent, authHeader);
	if (addSafeResponse == null) {
	    return null;
	}

	if(PASJava.DEBUG) {
	    System.out.println("addSafe response:");
	    System.out.println(addSafeResponse);
	    System.out.println("");
	}

	return addSafeResponse;

    } // addSafe

    // ===============================================================
    // JsonString listSafes() - produces list of safes in vault
    //
    public static String listSafes() {

	String requestUrl = pasServerUrl + "/Safes";
	String authHeader = pasSessionToken;

        if(PASJava.DEBUG) {
            System.out.println("====== PASJava.listSafes() ======");
            System.out.println("requestUrl: " + requestUrl);
            System.out.println("authHeader: " + authHeader);
            System.out.println("===================================");
            System.out.println("");
        }

        String safeResponse = JavaREST.httpGet(requestUrl, authHeader);

        if(PASJava.DEBUG) {
            System.out.println("Raw safe listing:");
            System.out.println(safeResponse);
            System.out.println("");
        }

        return safeResponse;
 
    } // listSafes

    // ===============================================================
    // JsonString deleteSafe(safeName)
    // Description:  returns json record with name of safe to delete and deletion status
    //
    public static String deleteSafe(String _safeName) {
        String requestUrl = pasServerUrl + "/Safes/" + _safeName;
        String authHeader = pasSessionToken;
	String deleteResponse = "";
        Integer responseCode = JavaREST.httpDelete(requestUrl, authHeader);

	deleteResponse = "{ \"safeName\": \"" + _safeName +"\", \"httpCode\": " + responseCode + "}";
	switch(responseCode) {
	  case 200:
	  case 202:
	  case 204:
		if(PASJava.DEBUG) {
	            System.out.println("PAS Safe \"" + _safeName + "\" deleted."); 
		}
		break;
	  default:
       		System.out.println("Cannot delete PAS Safe \"" + _safeName + "\"."); 
	}

	return deleteResponse;

    } // deleteSafe

    // ===============================================================
    // JsonString addSafeMember(safeName,memberName) 
    // Description: adds existing member to safe with LOBUser permissions
    //
    public static String addSafeMember(String _safeName, String _memberName) {

	String requestUrl = pasServerUrl + "/Safes/" + _safeName + "/Members";
	String authHeader = pasSessionToken;
	String bodyContent = "{"
				+ "\"memberName\":\"" + _memberName + "\","
				+ "\"searchIn\": \"Vault\","
				+ "\"membershipExpirationDate\":null,"
				+ "\"permissions\":"
				+ "{"
				+ "\"useAccounts\": true,"
				+ "\"retrieveAccounts\": true,"
				+ "\"listAccounts\": true,"
				+ "\"addAccounts\": true,"
				+ "\"updateAccountContent\": true,"
				+ "\"updateAccountProperties\": true,"
				+ "\"initiateCPMAccountManagementOperations\": true,"
				+ "\"specifyNextAccountContent\": true,"
				+ "\"renameAccounts\": true,"
				+ "\"deleteAccounts\": true,"
				+ "\"unlockAccounts\": true,"
				+ "\"manageSafe\": true,"
				+ "\"manageSafeMembers\": true,"
				+ "\"backupSafe\": true,"
				+ "\"viewAuditLog\": true,"
				+ "\"viewSafeMembers\": true,"
				+ "\"accessWithoutConfirmation\": true,"
				+ "\"createFolders\": true,"
				+ "\"deleteFolders\": true,"
				+ "\"moveAccountsAndFolders\": true,"
				+ "\"requestsAuthorizationLevel1\": true"
				+ "}"
			   + "}";

	if(PASJava.DEBUG) {
	    System.out.println("====== PASJava.addSafeMember() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("authHeader: " + authHeader);
	    System.out.println("bodyContent: " + bodyContent);
	    System.out.println("===================================");
	    System.out.println("");
	}

	String addSafeMemberResponse = JavaREST.httpPost(requestUrl, bodyContent, authHeader);

	if(PASJava.DEBUG) {
	    System.out.println("Raw addSafeMember response:");
	    System.out.println(addSafeMemberResponse);
	    System.out.println("");
	}

	return addSafeMemberResponse;

    } // addSafeMember

    // ===============================================================
    // JsonString getSafeMembers(safeName) 
    // Description: returns list of safe members.
    //
    public static String getSafeMembers(String _safeName) {
	String requestUrl = pasServerUrl + "/Safes/" + _safeName + "/Members";
	String authHeader = pasSessionToken;

	if(PASJava.DEBUG) {
	    System.out.println("====== PASJava.getSafeMembers() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("authHeader: " + authHeader);
	    System.out.println("===================================");
	    System.out.println("");
	}

        String memberResponse = JavaREST.httpGet(requestUrl, authHeader);

        if(PASJava.DEBUG) {
            System.out.println("Raw member listing:");
            System.out.println(memberResponse);
            System.out.println("");
        }

        return memberResponse;
 
    } // getSafeMembers

    // ===============================================================
    // JsonString removeSafeMember(safeName,memberName) 
    // Description: removes existing member from safe.
    //
    public static String removeSafeMember(String _safeName, String _memberName) {

	String requestUrl = pasServerUrl + "/Safes/" + _safeName + "/Members/" + _memberName;
	String authHeader = pasSessionToken;

	if(PASJava.DEBUG) {
	    System.out.println("====== PASJava.removeSafeMember() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("authHeader: " + authHeader);
	    System.out.println("===================================");
	    System.out.println("");
	}

	String deleteResponse = "";
        Integer responseCode = JavaREST.httpDelete(requestUrl, authHeader);

	deleteResponse = "{\"safeName\": " + _safeName
			  + ",\"memberName\": " + _memberName
			  + ",\"httpCode\": " + responseCode
			  + "}";
	switch(responseCode) {
	  case 200:
	  case 202:
	  case 204:
		if(PASJava.DEBUG) {
	            System.out.println("Member name \"" + _memberName + "\" removed from safe \"" + _safeName + "\"."); 
		}
		break;
	  default:
	        System.out.println("Cannot remove member name \"" + _memberName + "\" from safe \"" + _safeName + "\"."); 
	}

	return deleteResponse;

    } // removeSafeMember


/*****************************************************************
 *****************************************************************
 **			     ACCOUNTS				**
 *****************************************************************
 *****************************************************************/

    // ===============================================================
    // JsonString addAccount(safeName,accountName,platformId,address,userName,secretType,secretValue) 
    // Description: creates account in safe
    //
    public static String addAccount(String _safeName,
					String _accountName,
					String _platformId,
					String _address,
					String _userName,
					String _secretType,
					String _secretValue) {

	String requestUrl = pasServerUrl + "/Accounts";
	String authHeader = pasSessionToken;
	String bodyContent = "{"
				+ "\"name\":\"" + _accountName + "\","
				+ "\"address\":\"" + _address + "\","
				+ "\"userName\":\"" + _userName + "\","
				+ "\"platformId\":\"" + _platformId + "\","
				+ "\"safeName\":\"" + _safeName + "\","
				+ "\"secretType\":\"" + _secretType + "\","
				+ "\"secret\":\"" + _secretValue + "\""
			   + "}";

	if(PASJava.DEBUG) {
	    System.out.println("====== PASJava.addAccount() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("authHeader: " + authHeader);
	    System.out.println("bodyContent: " + bodyContent);
	    System.out.println("===================================");
	    System.out.println("");
	}

	String addAccountResponse = JavaREST.httpPost(requestUrl, bodyContent, authHeader);

	if(PASJava.DEBUG) {
	    System.out.println("Raw addAccount response:");
	    System.out.println(addAccountResponse);
	    System.out.println("");
	}

	return addAccountResponse;

    } // addAccount

    // ===============================================================
    // JsonString getAccounts(safeName) 
    // Description: returns json record with array of account objects for all accounts in a safe
    //
    public static String getAccounts(String _safeName) {

	String requestUrl = pasServerUrl + "/accounts?filter=safeName%20eq%20" + _safeName;
	String authHeader = pasSessionToken;

	if(PASJava.DEBUG) {
	    System.out.println("====== PASJava.getAccounts() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("authHeader: " + authHeader);
	    System.out.println("===================================");
	    System.out.println("");
	}

	String accountResponse = JavaREST.httpGet(requestUrl, authHeader);

	if(PASJava.DEBUG) {
	    System.out.println("Raw account listing:");
	    System.out.println(accountResponse);
	    System.out.println("");
	}

	return accountResponse;

    } // getAccounts

    // ===============================================================
    // JsonString getAccountDetails(accountId) - 
    // Description: returns json record with array of detail objects for account matching keywords
    //
    public static String getAccountDetails(String _accountId) {

	String requestUrl = pasServerUrl + "/Accounts/" + _accountId;
	String authHeader = pasSessionToken;

	if(PASJava.DEBUG) {
	    System.out.println("====== PASJava.getAccountDetails() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("authHeader: " + authHeader);
	    System.out.println("=========================================");
	    System.out.println("");
	}

	String detailResponse = JavaREST.httpGet(requestUrl, authHeader);

	if(PASJava.DEBUG) {
	    System.out.println("Raw detail listing:");
	    System.out.println(detailResponse);
	    System.out.println("");
	}

	return detailResponse;

    } // getAccountDetails

    // ===============================================================
    // JsonString deleteAccount(accountId)
    // Description:  returns json record with id of account to delete and deletion status
    //
    public static String deleteAccount(String _accountId) {
        String requestUrl = pasServerUrl + "/Accounts/" + _accountId;
        String authHeader = pasSessionToken;
	String deleteResponse = "";
        Integer responseCode = JavaREST.httpDelete(requestUrl, authHeader);

	deleteResponse = "{\"accountId\": \"" + _accountId + "\",\"httpCode\": \"" + responseCode + "\"}";
	switch(responseCode) {
	  case 200:
	  case 202:
	  case 204:
		if(PASJava.DEBUG) {
	            System.out.println("PAS Account Id \"" + _accountId + "\" deleted."); 
		}
		break;
	  default:
       		System.out.println("Cannot delete PAS Account Id \"" + _accountId + "\"."); 
	}

	return deleteResponse;

    } // deleteAccount

    // ===============================================================
    // JsonString getAccountGroups(safeName)
    // Description: returns json record with array of all account groups in safe
    //
    public static String getAccountGroups(String _safeName) {

	String requestUrl = pasServerUrl + "/AccountGroups?Safe=" + _safeName;
	String authHeader = pasSessionToken;

	if(PASJava.DEBUG) {
	    System.out.println("====== PASJava.getAccountGroups() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("authHeader: " + authHeader);
	    System.out.println("========================================");
	    System.out.println("");
	}

	String groupResponse = JavaREST.httpGet(requestUrl, authHeader);

	if(PASJava.DEBUG) {
	    System.out.println("Raw json account group response:");
	    System.out.println(groupResponse);
	    System.out.println("");
	}

	return groupResponse;

    } // getAccountGroups

    // ===============================================================
    // JsonString getAccountGroupMembers(GroupID)
    // Description: returns json record with array of all members in account group
    //
    public static String getAccountGroupMembers(String _groupId) {

	String requestUrl = pasServerUrl + "/AccountGroups/" + _groupId + "/Members";
	String authHeader = pasSessionToken;

	if(PASJava.DEBUG) {
	    System.out.println("====== PASJava.getAccountGroupMembers() ======");
	    System.out.println("requestUrl: " + requestUrl);
	    System.out.println("authHeader: " + authHeader);
	    System.out.println("==============================================");
	    System.out.println("");
	}

	String memberResponse = JavaREST.httpGet(requestUrl, authHeader);

	if(PASJava.DEBUG) {
	    System.out.println("Raw account group member response:");
	    System.out.println(memberResponse);
	    System.out.println("");
	}

	return memberResponse;

    } // getAccountGroupMembers


/*****************************************************************
 *****************************************************************
 **			PRIVATE MEMBERS				**
 *****************************************************************
 *****************************************************************/
    static private String pasServerUrl;
    static private String pasSessionToken;

} // PASJava
