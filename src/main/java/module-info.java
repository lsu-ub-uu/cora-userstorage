import se.uu.ub.cora.gatekeeper.storage.UserStorageViewInstanceProvider;
import se.uu.ub.cora.userstorage.UserStorageViewInstanceProviderImp;

module se.uu.ub.cora.userstorage {
	requires se.uu.ub.cora.logger;
	requires transitive se.uu.ub.cora.storage;
	requires transitive se.uu.ub.cora.gatekeeper;

	provides UserStorageViewInstanceProvider with UserStorageViewInstanceProviderImp;
}