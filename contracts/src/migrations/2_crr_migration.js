const Migrations = artifacts.require("CRR.sol");

module.exports = function (deployer) {
  deployer.deploy(Migrations);
};
