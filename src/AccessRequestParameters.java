// structure for gson
public class AccessRequestParameters {
    String projectName;
    String requestor;
    Integer approved;
    String environment;
    String pasVaultName;
    String pasSafeName;
    String pasCpmName;
    String pasLobName;
    String appIdName;
    String appAuthnMethod;

    public void print() {
        System.out.println( "projectName: " + this.projectName);
        System.out.println( "requestor: " + this.requestor);
        System.out.println( "approved: " + this.approved.toString());
        System.out.println( "environment: " + this.environment);
        System.out.println( "pasVaultName: " + this.pasVaultName);
        System.out.println( "pasSafeName: " + this.pasSafeName);
        System.out.println( "pasCpmName: " + this.pasCpmName);
        System.out.println( "pasLobName: " + this.pasLobName);
        System.out.println( "appIdName: " + this.appIdName);
        System.out.println( "appAuthnMethod: " + this.appAuthnMethod);
    };
}
