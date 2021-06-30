public class PASAccountDetail {
    String AccountID;
    KeyValue[] InternalProperties;
    KeyValue[] Properties;

    // searches both internal arrays for Key, if found returns Value
    public String getValue(String _key) {
	String foundValue=null;


	if(PASJava.DEBUG)	
	    System.out.print("PASAccount.getValue():\n  Key=" + _key + "\n  ");

	KeyValue[] kvArray = this.InternalProperties;
	for(Integer i=0; foundValue == null && i < kvArray.length; i++) {
	    if(PASJava.DEBUG)	
		System.out.print(i + ":" + kvArray[i].Key + ", ");
	    if(kvArray[i].Key.equals(_key))
		foundValue = kvArray[i].Value;
	}

	kvArray = this.Properties;
	for(Integer i=0; foundValue == null && i < kvArray.length; i++) {
	    if(PASJava.DEBUG)	
		System.out.print(i + ":" + kvArray[i].Key + ", ");
	    if(kvArray[i].Key.equals(_key)) 
		foundValue = kvArray[i].Value;
	}

	
	if(PASJava.DEBUG) {
	    System.out.println("");
	    System.out.println("   foundValue:" + foundValue);
	    System.out.println("");
	}
	return foundValue;
    } // getValue()

    public void print() {
	System.out.println( "  AccountId: " + this.AccountID);
	System.out.println( "  Internal Properties:");
	for(Integer i=0; i < this.InternalProperties.length; i++) {
	    this.InternalProperties[i].print();
	}
	System.out.println( "  Properties:");
	for(Integer i=0; i < this.Properties.length; i++) {
	    this.Properties[i].print();
	}
    };
}
