public class PASAccount {
    String id;
    String name;
    String address;
    String userName;
    String platformId;
    String safeName;
    String secretType;
    PASPlatformProperties platformAccountProperties;
    PASSecretManagement secretManagement;
    PASRemoteMachinesAccess remoteMachinesAccess;
    String createdTime;

    PASAccountDetail details;

    public String getValue(String _key) {
	return this.details.getValue(_key);
    }

    public void print() {
	System.out.println( "id: " + this.id);
	System.out.println( "name: " + this.name);
	System.out.println( "address: " + this.address);
	System.out.println( "userName: " + this.userName);
	System.out.println( "platformId: " + this.platformId);
	System.out.println( "safeName: " + this.safeName);
	System.out.println( "secretType: " + this.secretType);
	if(this.platformAccountProperties != null) {
	    System.out.println( "platformAccountProperties:");
	    this.platformAccountProperties.print();
	}
	System.out.println( "secretManagement:");
	this.secretManagement.print();
	if(this.remoteMachinesAccess != null) {
	    System.out.println( "remoteMachinesAccess:");
	    this.remoteMachinesAccess.print();
	}
	System.out.println( "createdTime: " + this.createdTime);
	if(this.details != null) {
	    System.out.println( "details:");
	    this.details.print();
	}
    };
}
