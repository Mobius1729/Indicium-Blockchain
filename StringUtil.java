package indicium_blockChain;

import java.security.*;
import java.util.ArrayList;
import java.util.Base64;

public class StringUtil {

	/** Applies the SHA-256 cryptographic hash function to a given input. 
	 * @param input - The String which needs to be hashed
	 * @return The hashed String in hexadecimal representation. 
	 * */
	public static String applySHA256(String input){
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256"); //creates instance of MessageDigest class, which is using SHA-256 for the hash function. Message digests are deterministic hash functions
			byte[] hash = digest.digest(input.getBytes("UTF-8")); //input is converted to bytes array by the UTF-8 standard. This is then encrypted via the digest object. 

			StringBuffer hexString = new StringBuffer(); //StringBuffer is used to make modifiable Strings. 
			/*
			 * StringBuffer may have characters and substrings inserted in the middle or appended to the end. 
			 * It will automatically grow to make room for such additions and often has more characters preallocated than are actually needed, to allow room for growth.
			 * */

			for(int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]); //converts every bit in hash to an 8-bit binary number in String representation
				//0xff & is used so only last 8-bits of hash[i] are used since 0xff is (2^8)-1 in decimal and 00000000 00000000 00000000 11111111 in binary

				//if the length of hex = 1
				if(hex.length() == 1) {
					hexString.append('0');
				}

				hexString.append(hex); //add hex to end of hexString
			}
			return hexString.toString();

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	/**Converts the Sender's private key and input to a signed array of bytes, known as the digital signature. 
	 * <br>This digital signature ensures that only the owner can spend their coins and that an unverified transaction (i.e. before it is mined) cannot be tampered with.
	 * @param privKey - The Sender's private key which is generated from their wallet. 
	 * @param input - The input (i.e. transaction) which needs to be signed.
	 * @return The digital signature, represented in a Byte array. 
	 * */
	public static byte[] applyESDSA(PrivateKey privKey, String input) {
		byte[] output = new byte[0];
		try {
			Signature dsa = Signature.getInstance("ECDSA", "BC"); //returns Signature object that implements ECDSA algorithm
			dsa.initSign(privKey); //initialize private key for signing
			dsa.update(input.getBytes()); //update the data to be verified
			byte[] signature = dsa.sign(); //returns signature of the updated data in a byte array.
			output = signature; 
		} catch(Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		return output;
	}

	/**Takes in the signature, public key of the Sender, and the data, and verifies if the signature is valid.
	 * @param pubKey - The public key of the Sender, generated as a key pair from the Sender's private key
	 * @param data - The data which is being verified
	 * @param signature - The signature which was created from the private key.
	 * @return True if it was the Sender's private key which signed the message.
	 * */
	public static boolean verifyECDSASig(PublicKey pubKey, String data, byte[] signature) {
		try {
			Signature verify = Signature.getInstance("ECDSA", "BC"); //returns Signature object that implements ECDSA
			verify.initVerify(pubKey); //initializes public key for verification
			verify.update(data.getBytes()); //updates the data to be verified
			return verify.verify(signature); //verifies the signature
		} catch(Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	/** Shows a standard representation of the key, that is needed outside of the JVM, as when transmitting the key to some other party
	 * @param key - The key which is being encoded
	 * @return The Base64-encoded String representation of the key.
	 * */
	public static String getStringFromKey(Key key) {
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

	/** Gets the Merkle root of all the transactions by going through the list of transactions.
	 * <br>This allows there to be 1000s of transactions in every block. The Merkle root is simply hashing all of the transaction hashes until only one hash remain. That is, the Merkle root is the hash of all the hashes of all the transactions stored in a block.</br>
	 * <p>This Merkle root still maintains the integrity of the blockchain. A change to a single transaction will result in a change of the root, which thus changes the hash of the entire block.</p>
	 * @param transactions - The list of transactions of a block, which form the base of the Merkle tree. 
	 * */
	public static String getMerkleRoot(ArrayList<Transactions> transactions) {
		int count = transactions.size(); //the size of the transactions
		ArrayList<String> prevTreeLayer = new ArrayList<String>(); //previous layer in the Merkle Tree

		//adds all Transactions to the previous layer (i.e. the base).
		for(Transactions transaction: transactions) {
			prevTreeLayer.add(transaction.transactionID);
		}

		ArrayList<String> treeLayer = prevTreeLayer;
		while(count > 1) {
			treeLayer = new ArrayList<String>();

			for(int i = 1; i < prevTreeLayer.size(); i++) {
				treeLayer.add(applySHA256(prevTreeLayer.get(i - 1) + prevTreeLayer.get(i)));
			}

			count = treeLayer.size();
			prevTreeLayer = treeLayer;
		}
		String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0): "";
		return merkleRoot;	
	}
}
