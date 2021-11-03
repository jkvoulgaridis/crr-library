package org.crr.lib;

import org.web3j
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.abi.FunctionEncoder
import org.web3j.tx.Transfer
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.tx.TransactionManager
import org.web3j.tx.RawTransactionManager
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.Web3j
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import org.web3j.utils.Convert.Unit
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.Log
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import java.math.BigInteger
import java.security.InvalidKeyException
import java.util.concurrent.CountDownLatch;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import scala.util.Random

class localGanaceDeploy(accountNumber: String, contractAddress: String,
                              privateKey: String) {

  var contract: CRR = _;
  var client: Web3j = _;
  var nonceSync: String = _;
  var credentials: Credentials = _;
  var gasProvider: ContractGasProvider = _;
  var rnd = new Random()

  def loadDeployedContract(): Any = {
    val w3 = Web3j.build(new HttpService("http://127.0.0.1:7545"));
    this.client = w3;
    println("[deploy] Connected to http provider service");
    this.gasProvider = new StaticGasProvider(new BigInteger("20000000000"),new BigInteger("6721975"));
    this.credentials = Credentials.create(this.privateKey)
    val address = credentials.getAddress()
    println(s"[deploy] computed address: ${address} for private key ${this.privateKey}")
    if(this.accountNumber == address){
      println(s"[deploy] extected address occured from provided private key :) ")
    }
    val contract = CRR.load(this.contractAddress , w3, credentials, gasProvider);
    this.contract = contract;
    this.nonceSync = "100";
    val contractAddressCheck = this.contract.getContractAddress();
    println(s"[deploy] contract address matching: ${contractAddressCheck == this.contractAddress}")
  }


  def initializeContext(taskId: Int,  minDeposit: Int, weiValue: Int):java.util.concurrent.CompletableFuture[EthSendTransaction] = {
    val taskIdBig = new BigInteger(taskId.toString);
    val minDepositBig = new BigInteger(minDeposit.toString);
    val weiValueBig = new BigInteger(weiValue.toString);
    val data = this.contract.initializeContext(taskIdBig, minDepositBig, weiValueBig);
    println(s"[initialize contract] transactio obj: $data")
    println(s"[deploy] nonce = ${this.nonceSync}")
    println(s"[deploy] gasPrice = ${this.gasProvider.getGasPrice()}")
    println(s"[deploy] gasLimit = ${this.gasProvider.getGasLimit()}")
    println(s"[deploy] contract address = ${this.contract.getContractAddress()}")

    val transaction = RawTransaction.createTransaction(
      new BigInteger(this.nonceSync),
      this.gasProvider.getGasPrice(),
      this.gasProvider.getGasLimit(),
      this.contract.getContractAddress(),
      weiValueBig,
      data);
    println("[deploy] created transaction and now trying to create signed")
    val signed = TransactionEncoder.signMessage(transaction, this.credentials)
    this.nonceSync = (this.nonceSync.toInt + 1).toString
    println("[deploy] created transaction signed")
    this.client.ethSendRawTransaction(signed.toString).sendAsync();
  }

  def getBalance(): BigInteger = {
    val res = this.contract.getContractBalance().send();
    return res
  }

  def registerExecutor(taskId: Int, weiValue: Int): Any = {
    val taskIdBig = new BigInteger(taskId.toString);
    val weiValueBig = new BigInteger(weiValue.toString);
    val transaction = this.contract.registerExecutor(taskIdBig, weiValueBig);
    transaction.send();
  }

  def submitResults(taskId: Int, resultHash: String, weiValue: Int, delay: Boolean = false): Any = {
    val taskIdBig = new BigInteger(taskId.toString);
    val resHashBig = new BigInteger(resultHash.toString).abs();
    val weiValueBig = new BigInteger(weiValue.toString);
    val transaction = this.contract.submitResults(taskIdBig, resHashBig ,weiValueBig);
    transaction.send();
  }

  def pollForNewTaskEvents(): Any = {
    val filter: EthFilter = new EthFilter(DefaultBlockParameterName.PENDING,
        DefaultBlockParameterName.LATEST, this.contractAddress.substring(2));
    val a = this.contract.newTaskEventFlowable(DefaultBlockParameterName.PENDING,
      DefaultBlockParameterName.LATEST)
      .subscribe(ev => println(s"Got a newTask event for task-${ev}"));
  }

  def pollForBadResultEvent(): Any = {
    val filter: EthFilter = new EthFilter(DefaultBlockParameterName.EARLIEST,
      DefaultBlockParameterName.LATEST, this.contractAddress.substring(2));
    val a = this.contract.badResultEventFlowable(filter)
      .subscribe(ev => println(s"Got a BadResult event for task-${ev.tid}"));
  }

  def startJob(): java.util.concurrent.CompletableFuture[TransactionReceipt] = {
    println("calling start Job")
    this.contract.startJob().sendAsync();
  }
}

class rinkebyDeployment(accountNumber: String, contractAddress: String,
                        privateKey: String)  extends localGanaceDeploy(accountNumber,contractAddress,privateKey){

  override def loadDeployedContract(): Any ={
    val w3 = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/4a0b4bd849a440518cab44f4fa7a83f3"));
    this.client = w3;
    println("[deploy] Connected to https provider service (infura)");
    this.gasProvider = new DefaultGasProvider();
    this.credentials = Credentials.create(this.privateKey)
    val address = credentials.getAddress()
    println(s"[deploy] computed address: ${address} for private key ${this.privateKey}")
    if(this.accountNumber == address){
      println(s"[deploy] extected address occured from provided private key :) ")
    }
    val attempts = 10;
    val sleepDur = 4;
    val TrxMan = new RawTransactionManager(this.client, credentials, attempts, sleepDur);
    val contract = CRR.load(this.contractAddress , w3, credentials,gasProvider);
    this.contract = contract;
    this.nonceSync = "100";
//    val ethGetTransactionCount1 = ethGetTransactionCount(credentials.getAddress()).send();
//    this.nonceSync = ethGetTransactionCount1.getTransactionCount();
  }
}

//class testClass(accountNumber: String, contractAddress: String,
//                privateKey: String) extends localGanaceDeploy(accountNumber,contractAddress, privateKey){
//  override def loadDeployedContract(): Any = {
//    val w3 = Web3j.build(new HttpService("http://127.0.0.1:7545"));
//    this.client = w3;
//    println("[deploy] Connected to http provider service");
//    val gasProvider = new StaticGasProvider(new BigInteger("20000000000"),new BigInteger("6721975"));
//    val credentials = Credentials.create(this.privateKey)
//    val address = credentials.getAddress()
//
//    println(s"[deploy] computed address: ${address} for private key ${this.privateKey}")
//    if(this.accountNumber == address){
//      println(s"[deploy] extected address occured from provided private key :) ")
//    }
//    val attempts = 10;
//    val sleepDur = 4;
//    val TrxMan = new RawTransactionManager(this.client, credentials, attempts, sleepDur);
//    //val contract = CRR.load(this.contractAddress , w3, TrxMan, gasProvider);
//    val contract = CRR.load(this.contractAddress , w3, credentials, gasProvider);
//    this.contract = contract;
//    val contractAddress: String = contract.getContractAddress();
//    println(s"[deploy] contract address matching: ${contractAddress == this.contractAddress}")
//  }
//}
