package indicium_blockChain;

import java.io.Serializable;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.*;

public class Wallet_beta {
	public PrivateKey privKey;
	public PublicKey pubKey;


	public HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>(); //only UTXOs owned by this wallet



	public Wallet_beta() {
		generateKeyPair();
	}
	/** Generates a Public-Private key pair from the Elliptic Curve Digital Signature Algorithm. 
	 * <br>It uses the SHA-1 as the foundation of the pseudo-random number generator.
	 * <br>The paramaters are based off of prime192v1 generation. 
	 * @return A Private-Public key pair which is specific to that wallet. 
	 * */
	public void generateKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC"); //generates pair of public and private keys using the digital signature and elliptic curve (EC) algorithms.
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG"); //a cryptographically strong random number generator - uses pseudorandom number generator (PRNG) generation and hashed with SHA-1
			ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1"); //specifies the method of parameter generation for the EC

			keyGen.initialize(ecSpec, random); //initializes the key-pair generator with the given parameter set and the source of randomness
			KeyPair keyPair = keyGen.generateKeyPair(); //generates the actual key pair

			privKey = keyPair.getPrivate(); //private key in key pair
			pubKey = keyPair.getPublic(); //public key in key pair

		} catch(Exception ex){
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	/** Gets the current balance of the wallet by iterating through all the unspent outputs (i.e. UTXOs) on the blockchain.
	 * If the value belongs to the user, that output is then added to an arbitrary counter <code>total</code>.
	 * @return The current Wallet balance i.e. all the unspent outputs of that wallet.
	 * */
	public float getWalletBalance() {
		float total = 0;

		//for all the entries in the Blockchain's hashmap containing all UTXOs.
		for(Map.Entry<String, TransactionOutput> item : Indicium.UTXOs.entrySet()) {
			TransactionOutput UTXO = item.getValue();

			//if the output (the coins) in TransactionOutput belong to the sender
			if(UTXO.isMine(pubKey)) {
				UTXOs.put(UTXO.id, UTXO); //add it to list of unspent outputs
				total += UTXO.value;
			}
		}
		return total;	
	}

	/**Sends funds to the desired address from an individual's wallet.
	 * Gathers all the necessary outputs and 'transfers' them to a new owner.
	 * All transactions are signed by the Sender.
	 * @param _recipient - The PublicKey (i.e. address) of the recipient of the coins.
	 * @param value - The amount being transferred.
	 * @return A new signed transaction.  
	 * */
	public Transactions sendFunds(PublicKey _recipient, float value) {
		//if wallet's balance is less than value being transferred
		if(getWalletBalance() < value) {
			System.out.println("#Not enough funds to complete transactions. Transaction discarded. ");
			return null;
		}

		ArrayList<TransactionInput>inputs = new ArrayList<TransactionInput>(); //create an ArrayList of inputs 

		float total = 0;
		
		//iterates through every unspent output of the wallet.
		for(Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {
			TransactionOutput UTXO = item.getValue();
			total += UTXO.value;
			inputs.add(new TransactionInput(UTXO.id));

			if(total > value) {
				break;
			}
		}

		Transactions newTransaction = new Transactions(pubKey, _recipient, value, inputs); //constructs a transaction from the wallet's input
		newTransaction.generateSignature(privKey); //generates a signature for the transaction

		//remove the inputs from UTXOs
		for(TransactionInput input : inputs) {
			UTXOs.remove(input.transactionOutID);
		}
		
		return newTransaction;
	}
}
