const CRR = artifacts.require("CRR")
const truffleAssert = require('truffle-assertions');


contract("CRR",async accounts => {
    let cnt;
    let owner;
    let notOwner;
    let worker1,worker2, worker3;

    beforeEach('creating instance before all Test Cases', async function(){
        owner = accounts[0];
        notOwner = accounts[1];
        worker1 = accounts[1];
        worker2 = accounts[2];
        worker2 = accounts[3];
        cnt = await CRR.new({from: accounts[0]});
    });

    it("[test 1] Should create a new instance of the contract with acc[0] owner.", async ()=> {
        let owner_ = await cnt.getOwner({from: worker1});
        assert.equal(owner_, owner , "calling owner from non-owner should be acc[0]");
        owner_ = await cnt.getOwner({from: owner});
        assert.equal(owner, owner_, "calling owner from owner should return acc[0]");
    });

    it("[test 2] Should have 0 initial contract balance", async() => {
        let initialContractBalance = 0;
        let bal = await cnt.getContractBalance();
        assert.equal(bal, initialContractBalance, "Initial contract bal not 0");
    })

    it("[test 3] Should tranfer funds to contract address when initializeContext if called fom owner",
      async () => {
        let cl = await cnt.initializeContext(0, web3.utils.toWei('0.3'), {value: web3.utils.toWei('2')});
        let bal = await cnt.getContractBalance();
        assert.equal(web3.utils.toWei("2"), bal, "funds not transfered");
    });

    it("[test 4] Should allow the owner to call initializeContext and emit a newTask event", async() => {
            let taskId = '0';
            let minDep = '0.3';
            let rew = '2';
            let cl = await cnt.initializeContext(taskId, web3.utils.toWei(minDep) ,
                                                  {from: accounts[0] , value: web3.utils.toWei(rew)})
                                                  .on("receipt", function(receipt){});

           truffleAssert.eventEmitted(cl, 'newTask', (ev) => {
                return (ev.tid.toNumber() == taskId
                        && web3.utils.fromWei(ev._reward) === rew
                        && web3.utils.fromWei(ev._min_deposit) === minDep
                        );
            });
            let res2 = await cnt.getContractBalance();
    });

    it("[test 5] Should not allow a non-owner to call initialize context", async () => {
        let res;
        let cl = await cnt.initializeContext(0, web3.utils.toWei('0.3'),
                                            {from : accounts[1],
                                            value: web3.utils.toWei('2')})
                                            .on("receipt", function(receipt) {res = true;})
                                            .catch(function(error) {res =  false;});
        assert.equal(res, false, "this account should not be able to call initialize context")
    });

    it("[test 6] Should reject a worker registration if not enough funds are transfered from Executor",
       async () => {
        let taskId = '0';
        let minDep = '0.3';
        let rew = '2';
        let res;
        let cl = await cnt.initializeContext(taskId, web3.utils.toWei(minDep) ,
                                              {from: accounts[0] , value: web3.utils.toWei(rew)})
                                              .on("receipt", function(receipt){});
        let registration = await cnt.registerExecutor(taskId,
                           {from: worker1, value: web3.utils.toWei('0.2')})
                           .on("receipt", function(receipt){res = true})
                           .catch(function(error) {res = false});
        assert.equal(res, false, "This worker did not submitted enough funds")
    });

    it("[test 7] Should emit a registeredFor event if one executor registers with enough funds", async() => {
        let taskId = '0';
        let minDep = '0.3';
        let rew = '2';

        let cl = await cnt.initializeContext(taskId, web3.utils.toWei(minDep) ,
                                              {from: accounts[0] , value: web3.utils.toWei(rew)});

        let registration = await cnt.registerExecutor(taskId,
                                                    {from: worker1,
                                                    value: web3.utils.toWei(minDep)})
                                                    .on("receipt", function(receipt){})
                                                    .catch(function(error){console.log("failed registration")})

        truffleAssert.eventEmitted(registration, 'registeredFor', (ev) => {
                return (ev.tid.toNumber() == taskId);
        });
    });

    it("[test 8] Should emit a registerCompleted event upon registration of 2 executors", async () => {
        let taskId = '0';
        let minDep = '0.3';
        let rew = '2';
        let cl = await cnt.initializeContext(taskId, web3.utils.toWei(minDep) ,
                                              {from: accounts[0] , value: web3.utils.toWei(rew)})

        let registration = await cnt.registerExecutor(taskId,
                                                    {from: worker1,
                                                    value: web3.utils.toWei(minDep)})
                                                    .on("receipt", function(receipt){})
                                                    .catch(function(error){console.log("failed registration")})


        truffleAssert.eventEmitted(registration, 'registeredFor', (ev) => {
                return (ev.tid.toNumber() == taskId);
        });

        let registration2 = await cnt.registerExecutor(taskId,
                                                    {from: worker2,
                                                    value: web3.utils.toWei(minDep)})
                                                    .on("receipt", function(receipt){})
                                                    .catch(function(error){console.log(error)});

        truffleAssert.eventEmitted(registration2, 'registerCompleted', (ev) => {
                        return (ev.tid.toNumber() == taskId);
        });

    });

    it("[test 9] Should not allow result submission if only 1 executor in", async() => {
        let taskId = '0';
        let minDep = '0.3';
        let rew = '2';
        let resHash = "123res";
        let submissionAllowed;
        let cl = await cnt.initializeContext(taskId, web3.utils.toWei(minDep) ,
                                          {from: accounts[0] , value: web3.utils.toWei(rew)})

        let registration = await cnt.registerExecutor(taskId,
                                                {from: worker1,
                                                value: web3.utils.toWei(minDep)})
                                                .on("receipt", function(receipt){})
                                                .catch(function(error){console.log("failed registration")})

        let re1 = await cnt.submitResults(taskId, resHash,
                                    {from: worker1})
                                    .on("receipt", function(receipt){})
                                    .catch(function(error){
                                        submissionAllowed=false;
                                    })
        assert.equal(submissionAllowed,false, "can't submit results in this case!");
    });

    it("[test 10] Should emit verifiedResult if 2 executors submit the same result hash", async() =>{
        let taskId = '0';
        let minDep = '0.3';
        let rew = '2';
        let resHash = "1234567890";
        let submissionAllowed;
        let cl = await cnt.initializeContext(taskId, web3.utils.toWei(minDep) ,
                                      {from: accounts[0] , value: web3.utils.toWei(rew)})

        let registration = await cnt.registerExecutor(taskId,
                                        {from: worker1,
                                        value: web3.utils.toWei(minDep)})
                                        .on("receipt", function(receipt){})
                                        .catch(function(error){console.log("failed registration")})

        registration = await cnt.registerExecutor(taskId,
                                        {from: worker2,
                                        value: web3.utils.toWei(minDep)})
                                        .on("receipt", function(receipt){})
                                        .catch(function(error){console.log("failed registration")})

        let res= await cnt.submitResults(taskId, resHash,
                                        {from: worker1})
                                        .on("receipt", function(receipt){})
                                        .catch(function(error){
                                            console.log(error)
                                        })

        res= await cnt.submitResults(taskId, resHash,
                                        {from: worker2})
                                        .on("receipt", function(receipt){})
                                        .catch(function(error){
                                            console.log(error)
                                        })

        truffleAssert.eventEmitted(res, 'verifiedResult', (ev) => {
          return (ev.tid.toNumber() == taskId);
        })
    });

    it("[test 11] Should emit a badResultEvent if result hashes don't match", async() => {
        let taskId = '0';
        let minDep = '0.3';
        let rew = '2';
        let resHash = "1234567890";
        let resHash_2 = "1234567899";
        let badRes;
        let cl = await cnt.initializeContext(taskId, web3.utils.toWei(minDep) ,
                                      {from: accounts[0] , value: web3.utils.toWei(rew)})

        let registration = await cnt.registerExecutor(taskId,
                                        {from: worker1,
                                        value: web3.utils.toWei(minDep)})
                                        .on("receipt", function(receipt){})
                                        .catch(function(error){console.log("failed registration")})

        registration = await cnt.registerExecutor(taskId,
                                        {from: worker2,
                                        value: web3.utils.toWei(minDep)})
                                        .on("receipt", function(receipt){})
                                        .catch(function(error){console.log("failed registration")})

        let res = await cnt.submitResults(taskId, resHash,
                                        {from: worker1})
                                        .on("receipt", function(receipt){})
                                        .catch(function(error){
                                            console.log(error)
                                        })

        res = await cnt.submitResults(taskId, resHash_2,
                                        {from: worker2})
                                        .on("receipt", function(receipt){badRes = true;})
                                        .catch(function(error){
                                            console.log(error)

                                        })

        truffleAssert.eventEmitted(res, 'badResult', (ev) => {
          return (ev.tid.toNumber() == taskId);
        })

        assert.equal(badRes, true, "Bad result, transaction should revert")
    });

});