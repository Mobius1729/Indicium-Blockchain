package indicium_blockChain;

import java.io.*;
import java.security.*;
import java.util.*;
import com.google.gson.*;

public class Indicium_beta {

	public static ArrayList<Block> blockchain = new ArrayList<Block>();
	public static HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();

	public static final int difficulty = 5; //mining difficulty
	public static float minTransaction = 0.1f; //transaction between two parties has to be at least 0.1 Indiciums
	
	public static Wallet firstWallet; 
	public static Wallet walletB;
	public static Transactions genesisTransaction;
	
	public static int walletCounter;
	public static Wallet currentWallet;


	public static Scanner sc = new Scanner(System.in);
	/** Determines if a block is valid
	 * @return True if the block is valid, False if it is not. 
	 * */
	public static boolean isValid() {
		Block currentBlock;
		Block previousBlock;

		for(int i = 1; i < blockchain.size(); i++) {
			currentBlock = blockchain.get(i);
			previousBlock = blockchain.get(i - 1);

			if(!(currentBlock.hash.equals(currentBlock.calculateHash()))) {
				System.out.println("Current hashes are not equal.");
				return false;
			}
			if(!(currentBlock.prevHash.equals(previousBlock.hash))) {
				System.out.println("Previous hashes are not equal.");
				return false;
			}
		}
		return true;
	}
	/** Determines if the blockchain is valid and its integrity has not been compromised.
	 * @return True if the chain is valid, False if it is not.
	 * */
	public static boolean isChainValid() {
		Block currentBlock;
		Block prevBlock;

		String hashTarget = new String(new char[difficulty]).replace('\0', '0');
		HashMap<String, TransactionOutput> tempUTXOs = new HashMap<String, TransactionOutput>();
		tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

		//iterate through the blockchain to check hashes
		for(int i = 1; i < blockchain.size(); i++) {
			currentBlock = blockchain.get(i);
			prevBlock = blockchain.get(i - 1);
			
			//if the current hashes are not equal
			if(!(currentBlock.hash.equals(currentBlock.calculateHash()))) {
				System.out.println("#Current hashes are not equal. ");
				return false;
			}
			//if the previous hashes are not equal
			if(!(prevBlock.hash.equals(currentBlock.prevHash))) {
				System.out.println("#Previous hashes are not equal. ");
				return false;
			}
			//if the block has not been mined
			if(!(currentBlock.hash.substring(0, difficulty).equals(hashTarget))) {
				System.out.println("#This block has not been mined. ");
				return false;
			}

			TransactionOutput tempOutput;
			for(int t = 0; i < currentBlock.transactions.size(); t++) {
				Transactions currentTransaction = currentBlock.transactions.get(t);

				if(!(currentTransaction.verifySignature())) {
					System.out.println("#Transaction (" + t + ") + signature is invalid. ");
					return false;
				}
				if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
					System.out.print("#Inputs are not equal to outputs in Transaction(" + t + ")");
					return false;
				}

				for(TransactionInput input: currentTransaction.inputs) {
					tempOutput = tempUTXOs.get(input.transactionOutID);

					if(tempOutput == null) {
						System.out.println("#Referenced input in Transaction(" + t + ") is missing. ");
						return false;
					}

					if(input.UTXO.value != tempOutput.value) {
						System.out.println("#Referenced input in Transaction(" + t + ") value is invalid. ");
						return false;
					}

					tempUTXOs.remove(input.transactionOutID);
				}

				for(TransactionOutput output: currentTransaction.outputs) {
					tempUTXOs.put(output.id, output);
				}

				if(currentTransaction.outputs.get(0).recipient != currentTransaction.sender) {
					System.out.println("#Transaction(" + t + ") output recipient is not who it should be. ");
					return false;
				}
				if(currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
					System.out.println("#Transaction(" + t + ") output 'change' is not transfered back to Sender. ");
					return false;
				}
			}

		}

		System.out.println("#Blockchain is valid");
		return true;	
	}
	
	/** Adds a block to the blockchain after it has been successfully mined. 
	 * @param newBlock - the block which is being added to the blockchain.
	 * @return Adds the block to the blockchain once mined. 
	 * */
	public static void addBlock(Block newBlock) {
		newBlock.mineBlock(difficulty);
		blockchain.add(newBlock);
	}

	public static void main(String[] args){
		//Setup Bouncy Castle as the security provider
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		//Initialize new wallets
		firstWallet = new Wallet();
		walletB = new Wallet();
		Wallet coinbase = new Wallet();
		
		//A new wallet can be created by a user initializing a new Wallet: Wallet wallet_name = new Wallet();


		System.out.println("Welcome to the Indicium blockchain!");
		System.out.println("This is a demo. This will showcase how the blockchain functions and how value is transferred between 2 wallets.\n");

		genesisTransaction = new Transactions(coinbase.pubKey, firstWallet.pubKey, 1729f, null);
		genesisTransaction.generateSignature(coinbase.privKey);
		genesisTransaction.transactionID = "0";
		genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.receiver, genesisTransaction.value, genesisTransaction.transactionID));
		UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));


		System.out.println("Creating and mining the Genesis block... ");
		Block genesis = new Block("0");
		genesis.addTransaction(genesisTransaction);
		addBlock(genesis);

		Block block1 = new Block(genesis.hash);
		System.out.println("\nWallet A's balance is: " + firstWallet.getWalletBalance());
		System.out.println("Wallet A is attempting to transfer funds (40) to WalletB...  ");
		block1.addTransaction(firstWallet.sendFunds(walletB.pubKey, 40f));
		addBlock(block1);
		System.out.println("\nWallet A's balance is: " + firstWallet.getWalletBalance());
		System.out.println("\nWallet B's balance is: " + walletB.getWalletBalance());

		Block block2 = new Block(block1.hash);
		System.out.println("Wallet A is attempting to transfer more funds (1000) than it currently has... ");
		block2.addTransaction(firstWallet.sendFunds(walletB.pubKey, 1000f));
		addBlock(block2);
		System.out.println("\nWallet A's balance is: " + firstWallet.getWalletBalance());
		System.out.println("\nWallet B's balance is: " + walletB.getWalletBalance());

		Block block3 = new Block(block2.hash);
		System.out.println("Wallet B is attempting to send funds (20) to Wallet A... ");
		block3.addTransaction(walletB.sendFunds(firstWallet.pubKey, 20f));
		addBlock(block3);
		System.out.println("\nWallet A's balance is: " + firstWallet.getWalletBalance());
		System.out.println("\nWallet B's balance is: " + walletB.getWalletBalance());

		isChainValid();





		/*System.out.println("Private and Public keys: ");
		System.out.println(StringUtil.getStringFromKey(firstWallet.privKey));
		System.out.println(StringUtil.getStringFromKey(firstWallet.pubKey));

		Transactions transaction = new Transactions(firstWallet.pubKey, walletB.pubKey, 5, null);
		transaction.generateSignature(firstWallet.privKey);
		System.out.println("Is signature verified: ");
		System.out.println(transaction.verifySignature());

		blockchain.add(new Block("0"));
		System.out.println("Trying to Mine block 1... ");
		blockchain.get(0).mineBlock(difficulty);

		blockchain.add(new Block(blockchain.get(0).hash));
		System.out.println("Trying to Mine block 2... ");
		blockchain.get(1).mineBlock(difficulty);

		blockchain.add(new Block(blockchain.get(1).hash));
		System.out.println("Trying to Mine block 3... ");
		blockchain.get(2).mineBlock(difficulty);*/	

		String blockchainJson = new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
		System.out.println("\nThe block chain: ");
		System.out.println(blockchainJson);


	}

}
