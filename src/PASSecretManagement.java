public class PASSecretManagement {
    String automaticManagementEnabled;
    String manualManagementReason;
    String status;
    Integer lastModifiedTime;
    Integer lastReconciledTime;
    Integer lastVerifiedTime;

    public void print() {
	System.out.println( "  automaticManagementEnabled: " + this.automaticManagementEnabled);
	System.out.println( "  manualManagementReason: " + this.manualManagementReason);
	System.out.println( "  status: " + this.status);
	System.out.println( "  lastModifiedTime: " + this.lastModifiedTime);
	System.out.println( "  lastReconciledTime: " + this.lastReconciledTime);
	System.out.println( "  lastVerifiedTime: " + this.lastVerifiedTime);
    };
}
