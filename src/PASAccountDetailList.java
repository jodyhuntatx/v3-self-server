// This class is only useful for Gson parsing of the json output from get account details.
// Count is useless since only one record is returned. However the json for "accounts"
// is in array format, hence it's an array here. PASJava.getAccountDetails() returns
// account[0] - the first (and only) AccountDetail object.

public class PASAccountDetailList{
    Integer Count;
    PASAccountDetail[] accounts;

    public void print() {
	for(Integer i=0; i < this.accounts.length; i++) {
		this.accounts[i].print();
		System.out.println("");
	}
    }
}
