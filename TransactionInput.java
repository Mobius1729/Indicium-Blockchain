package indicium_blockChain;

public class TransactionInput {
	// The debit in a double-ledger transaction.
	// This Class will reference unspent TransactionOutputs, allowing miners to check ownership. 

	public String transactionOutID; //reference to TransactionOutput
	public TransactionOutput UTXO; //unspent transaction outputs
	
	public TransactionInput(String output) {
		transactionOutID = output;
	}
}
