package indicium_blockChain;

import java.security.*;

public class TransactionOutput {
	// The credit in a double-ledger transaction
	// Transaction outputs show final amount of money sent to each party. 

	public String id;
	public PublicKey recipient; //new owner of the coins
	public float value; //amount of coins they own
	public String parentTransactionID; //ID of transaction output was created in. 
	
	public TransactionOutput(PublicKey rec, float val, String parTransactionID) {
		recipient = rec;
		value = val;
		parTransactionID = parentTransactionID;
		id = StringUtil.applySHA256(StringUtil.getStringFromKey(recipient) + Float.toString(value) + parentTransactionID);
	}
	
	public boolean isMine(PublicKey pub) {
		return(pub == recipient);
	}
}
