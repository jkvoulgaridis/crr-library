let Contract = require('web3-eth-contract');
let binFile = "/home/user/Documents/thesis/contracts/CRR.bin" 
let provider = "http://127.0.0.1:7545"


function deployContract(provider, Contract, address, fromAccount) {
	Contract.setProvider(provider);
	fs = require('fs')
	let accountFrom = "0x0f4287A9D7d388415b78Cc1Ede2c1Ce48a8c5946"
	let accountKey = "07ef15255f3beca509b183b47718c0cf124dd2926f48f8897044e5ce126894ae"
	let contractBin = fs.readFileSync(binFile , 'utf8')
	Contract.deploy({data: contractBin, from : accountFrom})
}


function useContract() {


}


function mainTest() {
	deployContract()
}