public class PASPlatformProperties {
	String Database;
	String AWSAccessKeyID;
  	String ConjurAccount;
        String HostName;
        String ApplianceURL;

    public void print() {
	System.out.println( "  Database: " + this.Database);
	System.out.println( "  ConjurAccount: " + this.ConjurAccount);
	System.out.println( "  HostName: " + this.HostName);
	System.out.println( "  ApplianceURL: " + this.ApplianceURL);
	System.out.println( "  AWSAccessKeyID: " + this.AWSAccessKeyID);
    };
}
