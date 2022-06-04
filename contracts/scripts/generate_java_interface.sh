EXE=$1
PACK=$2

echo "------------------------HELP------------------------"
echo "PWD var should be set to point /thesis/contracts dir"
PWD="/home/john/personal/distrib-verifiable/contracts"
if [ $# -ne 2 ];  then 
	echo "Not enough arguments passed to script"
	echo "Usage: ./generate_java_interface.sh <contract_name> <package_name>"
	exit -1
fi

web3j generate solidity  -b $PWD/solcOutputs/$EXE.bin -a $PWD/solcOutputs/$EXE.abi -o $PWD/javaWrapper/ -p $PACK
