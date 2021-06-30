// This class is mostly useful for Gson parsing of the json output from get accounts.

public class PASAccountList {
    PASAccount[] value;
    Integer count;

    public void print() {
	for(Integer i=0; i<this.count; i++) {
		this.value[i].print();
		System.out.println("");
	}
    }
}
