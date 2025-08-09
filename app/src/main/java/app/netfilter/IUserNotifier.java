package app.netfilter;

import app.security.PolicyRules;

public interface IUserNotifier {

    void notify(PolicyRules rules, byte[] serverIp);

    void notify(PolicyRules rules, String domain);

    void notify(PolicyRules rules, String domain, String refDomain);

}
