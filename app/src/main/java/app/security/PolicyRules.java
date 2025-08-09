
/*
 * last modified: 2015.04.30
 */

package app.security;

import app.internal.Settings;
import app.scanner.LibScan;

public class PolicyRules {
    public static final int NORMAL = 0;
    public static final int DROP = 1;
    public static final int SCAN = 2;
    public static final int NOTIFY_USER = 4;
    public static final int NOTIFY_SERVER = 8;
    public static final int HASH = 16;
    public static final int MODIFY = 32;
    public static final int WAIT = 64;
    public static final int COMPRESSED = 128;

    protected int policy;
    protected int verdict; // == LibScan.RECORD_TYPE_*
    protected int[] records = null;

    // sometimes we need to allow open site in new browser tab, but we can't
    // detect it in 100% by referrer, so we use JS redirect (see send404, getPolicy(String url,...))
    protected String redirect = null;

    /* ----------------------------------------------------------- */

    public PolicyRules() {
        this.policy = PolicyRules.NORMAL;
        this.verdict = LibScan.RECORD_TYPE_UNKNOWN;
    }

    // policy == bitmask
    public PolicyRules(int policy) {
        this.policy = policy;
        this.verdict = LibScan.RECORD_TYPE_UNKNOWN;
    }

    // policy == bitmask, verdict == LibScan.RECORD_TYPE_*
    public PolicyRules(int policy, int verdict) {
        this.policy = (policy == -1) ? PolicyRules.NORMAL : policy;
        //this.verdict = (LibScan.recordTypeIsValid(verdict)) ? verdict : LibScan.RECORD_TYPE_UNKNOWN;
        this.verdict = verdict;
    }

    public PolicyRules(int policy, int verdict, int[] records, String redirect) {
        this.policy = policy;
        this.verdict = verdict;
        this.records = records;
        this.redirect = redirect;
    }

    /* ----------------------------------------------------------- */

    public int getPolicy() {
        return this.policy;
    }

    public boolean hasPolicy(int policy) {
        return ((this.policy & policy) > 0);
    }

    public void addPolicy(int policy) {
        this.policy |= policy;
    }

    public int getVerdict() {
        return this.verdict;
    }

    // can return null
    public int[] getRecords() {
        //if (this.records == null) return (new int[0]);
        //else return this.records;
        return this.records;
    }

    public boolean isNormal() {
        return (this.policy == NORMAL);
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public String getRedirect() {
        return this.redirect;
    }

    @Override
    public String toString() {
        if (Settings.DEBUG || Settings.DEBUG_POLICY)
            return ("PolicyRules: " + this.policy + " " + LibScan.recordTypeToString(this.verdict) +
                    " '" + this.redirect + "'");

        return "";
    }
}
