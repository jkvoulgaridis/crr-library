pragma solidity ^0.5.0;

contract CRR {
  address  _owner;
  uint _total_pot;
  uint _state;
  uint _rew;
  uint _min_dep;
  uint _total_particpants;
  uint  total_tasks;
  uint completed_tasks;
  bool _pay_executor_called;

  uint[] taskIds;
  mapping(uint => address[]) participants;
  mapping(uint => int[]) results;

  event newTask(uint tid, uint _reward, uint _min_deposit);
  event registeredFor(uint tid);
  event registerCompleted(uint tid);
  event taskResult(uint tid, int results);
  event badResult(uint tid);
  event verifiedResult(uint tid);

  constructor() public {
  	_owner = msg.sender;
  	_total_pot = 0;
    _total_particpants=0;
    _state = 0;
    _pay_executor_called=false;
  }

  function startJob() public returns (bool) {
    _total_pot = 0;
    _total_particpants = 0;
    _state = 0;
    _pay_executor_called=false;
    for(uint i=0; i<taskIds.length; i++){
      delete participants[taskIds[i]];
      delete results[taskIds[i]];
    }
    delete taskIds;
    return true;
  }

  function getOwner() public view returns (address) {
	return _owner;
  }

  function getTotalParticipantsCount() public view returns (uint) {
    return _total_particpants;
  }

  function getContractBalance() public view returns (uint) {
    return address(this).balance;
  }

  function initializeContext(uint _taskId, uint _min_deposit) public payable{
      _rew = msg.value;
      _min_dep = _min_deposit;
      taskIds.push(_taskId);
      require(msg.sender.balance > msg.value);
      emit newTask(_taskId , _rew ,_min_deposit);
  }

  function registerExecutor(uint taskId) public payable{
    require(msg.value >= _min_dep);
    require(participants[taskId].length < 2, "registration full");
    if(participants[taskId].length == 0){
      participants[taskId].push(msg.sender);
      emit registeredFor(taskId);
    }
    else if(participants[taskId].length == 1){
      participants[taskId].push(msg.sender);
      emit registerCompleted(taskId);
    }
    else{
      require(participants[taskId].length < 2, "registration full");
    }
  }

  function submitResults(uint taskId, int resultHash) public payable{
    if (results[taskId].length == 0){
      results[taskId].push(resultHash);
    }
    else if(results[taskId].length == 1){
      if(results[taskId][0] == resultHash){
        emit verifiedResult(taskId);
      }
      else{
        emit badResult(taskId);
      }
    }
  }

  function payExecutors() public payable{
    require(total_tasks > 0, "job not yet started");
    require(total_tasks == completed_tasks, "job not yet done");
    require(_pay_executor_called = false);
    _pay_executor_called=true;
  }
}

