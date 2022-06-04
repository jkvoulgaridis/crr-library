EXE=$1

echo "-----HELP------"
echo "PWD var should be set to point /thesis/contracts dir"
PWD="/home/john/personal/distrib-verifiable/contracts"
echo "currently $PWD in directory" 
solc --overwrite $PWD/src/contracts/$EXE --bin --abi --optimize -o $PWD/solcOutputs
