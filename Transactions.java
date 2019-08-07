package indicium_blockChain;

import java.security.*;
import java.util.*;
import java.time.*;

import org.bouncycastle.asn1.x509.Time;

public class Transactions {

	public String transactionID; //hash of the transaction
	public PublicKey sender; //the senders public key
	public PublicKey receiver; //the recipients public key
	public float value; //the value being transferred
	public LocalTime timeOfCreation; //time the block is created
	public byte[] signature; //ensures data in transaction hasn't been altered
	
	public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>(); //ensures sender has sufficient fund; the 'debit' in terms of a double-ledger
	public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>(); //how much the recipient received in the transaction; the 'credit' in terms of a double-ledger.
	
	private static int transactions_count; 	
	
	public Transactions(PublicKey from, PublicKey to, float value_transferred, ArrayList<TransactionInput> input) {
		sender = from;
		receiver = to;
		value = value_transferred;
		inputs = input;
	}
	
	/**Calculates a transaction's hash by using the <i>applySha256</i> method.
	 * @return The String of the resulting hash. 
	 * */
	public String calcHash() {
		transactions_count++; //ensures two identical transactions don't have same hash
		return StringUtil.applySHA256(StringUtil.getStringFromKey(sender) + 
				StringUtil.getStringFromKey(receiver) + 
				Float.toString(value) + 
				transactions_count);
	}
	
	/**Generates a signature for that transaction, hence verifying the <i>authenticity</i> of the transaction.
	 * This method generates a digital signature, which is used to verify that it was initiated by the owner of the coins.
	 * <br>Signatures will be verified by miners as they are added to new blocks.  
	 * @param privKey - The PrivateKey of the Sender, which will be used in signing the data.
	 * @return A signature for the specified transaction, generated via the ECDSA algorithm.
	 * */
	public void generateSignature(PrivateKey privKey) {
		timeOfCreation = java.time.LocalTime.now();
		String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(receiver) + Float.toString(value) + timeOfCreation.toString();
		signature = StringUtil.applyESDSA(privKey, data);
	}
	
	/**Verifies that the data has not been tampered with and that it was the owner of the coins who authorized the transaction.
	 * @return True if the signature is valid (i.e. the PublicKey corresponds to the PrivateKey used for signing).
	 * */
	public boolean verifySignature() {
		String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(receiver) + Float.toString(value) + timeOfCreation.toString();
		return StringUtil.verifyECDSASig(sender, data, signature);
	}
	
	/** Processes the transaction and ensures that the digital signature is valid and the Sender has sufficient outputs to complete the transaction.
	 * <p>The transaction is then carried out and processed, sending the transferred coins to the Recipient and any change back to the Sender.</p>
	 * 
	 * */
	public boolean processTransaction() {
		if(verifySignature() == false) {
			System.out.println("#Transaction signature failed to verify. ");
			return false;
		}
		
		//gathers transaction's inputs (ensures they are unspent)
		for(TransactionInput i : inputs) {
			i.UTXO = Indicium.UTXOs.get(i.transactionOutID); //returns mapped value from previous TransactionOutputs
		}
		
		//checks if funds are sufficient to complete transaction
		if(getInputsValue() < Indicium.minTransaction) {
			System.out.println("#Transaction Inputs too small: " + getInputsValue());
			return false;
		}
		
		//generates the transaction's outputs
		float leftOver = getInputsValue() - value; //the 'change' in the transaction
		transactionID = calcHash();
		outputs.add(new TransactionOutput(receiver, value, transactionID)); //sends the value to the recipient
		outputs.add(new TransactionOutput(sender, leftOver, transactionID)); //sends any change back to the sender
		
		//adds outputs to Unspent list (UTXOs)
		for(TransactionOutput i : outputs){
			Indicium.UTXOs.put(i.id, i);
		}
		
		//remove the transaction inputs from UTXO list as they're spent
		for(TransactionInput i : inputs) {
			if(i.UTXO == null) {
				continue;
			}
			Indicium.UTXOs.remove(i.UTXO.id);
		}
		return true;
	}
	
	/**Calculates the total value of inputs.
	 * @return The total value.
	 * */
	public float getInputsValue() {
		float total = 0;
		for(TransactionInput i : inputs) {
			if(i.UTXO.equals(null)) {
				continue;
			}
			total += i.UTXO.value;
		}
		return total;
	}
	
	/**Calculates the total value of outputs
	 * 
	 * */
	public float getOutputsValue() {
		float total = 0;
		for(TransactionOutput i : outputs) {
			total += i.value;
		}
		return total;
	}
}
