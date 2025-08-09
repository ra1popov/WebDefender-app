
/*
 * class to save results of data scan (see Scanner class)
 *
 * last modified: 2015.04.30
 */

package app.scanner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import app.internal.Settings;

public class ScanResult {
    // to save all scan verdicts and trigged records ids
    protected Set types = new HashSet(); // LibScan.RECORD_TYPE_* values
    protected int[] ids = null;

    // has any common type, test or white ?
    protected boolean has_dangerous = false;
    protected boolean has_ads = false;
    protected boolean has_social = false;

    protected boolean has_test = false;
    protected boolean has_white = false;
    protected boolean has_other = false; // has any type != WHITE, TEST and CLEAN ?

    // most important type
    protected int major = LibScan.RECORD_TYPE_UNKNOWN; // == LibScan.RECORD_TYPE_*

    /* ----------------------------------------------------------- */

    public ScanResult() {
    }

    ;

    public ScanResult(int type) {
        this.addType(type);
    }

    ;

    /*
     * add scan type (verdict) (== LibScan.RECORD_TYPE_*)
     *
     * UNKNOWN type can not be added
     * on WHITE type CLEAN also will be added
     */
    public void addType(int type) {
        if (type == LibScan.RECORD_TYPE_UNKNOWN)
            return;

        if (!this.types.add(type))
            return; // not modified, returning

        //

        if (this.major == LibScan.RECORD_TYPE_UNKNOWN)
            this.major = type;
        else
            this.major = LibScan.recordTypeGetMajor(this.major, type);

        if (!this.has_dangerous)
            this.has_dangerous = LibScan.recordTypeIsDangerous(type);

        if (!this.has_ads)
            this.has_ads = LibScan.recordTypeIsAds(type);

        if (!this.has_social)
            this.has_social = LibScan.recordTypeIsSocial(type);

        //

        switch (type) {
            case LibScan.RECORD_TYPE_WHITE:
                this.has_white = true;
                this.types.add(LibScan.RECORD_TYPE_CLEAN);
                break;

            case LibScan.RECORD_TYPE_TEST:
                this.has_test = true;
                break;

            case LibScan.RECORD_TYPE_CLEAN:
                break;

            default:
                this.has_other = true;
                break;
        }
    }

    // types == LibScan.RECORD_TYPE_* values
    public void addTypes(int[] types) {
        for (int i = 0; i < types.length; i++) {
            //int type = (LibScan.recordTypeIsValid(types[i])) ? types[i] : LibScan.RECORD_TYPE_UNKNOWN;
            int type = types[i];
            this.addType(type);
        }
    }

    /*
     * has a type ? (== LibScan.RECORD_TYPE_*)
     *
     * if have WHITE return true only on: WHITE, CLEAN, TEST
     * if have any type != WHITE or TEST (and no WHITE) return false on CLEAN
     */
    public boolean hasType(int type) {
        if (this.has_white) {
            switch (type) {
                case LibScan.RECORD_TYPE_WHITE:
                case LibScan.RECORD_TYPE_CLEAN:
                case LibScan.RECORD_TYPE_TEST:
                    break;

                default:
                    return false;
            }
        } else {
            if (this.has_other && type == LibScan.RECORD_TYPE_CLEAN)
                return false;
        }

        return this.types.contains(type);
    }

    // have any dangerous, ads or social type ? see hasType
    public boolean hasDangerous() {
        return ((this.has_white) ? false : this.has_dangerous);
    }

    public boolean hasAds() {
        return ((this.has_white) ? false : this.has_ads);
    }

    public boolean hasSocial() {
        return ((this.has_white) ? false : this.has_social);
    }

    /* return most important type from all added (LibScan.RECORD_TYPE_* values)
     * see recordTypeGetMajor
     */
    public int getMajorType() {
        return this.major;
    }

    // can return null
    public int[] getIds() {
        //if (this.ids == null) return (new int[0]);
        //else return this.ids;
        return this.ids;
    }

    public void addIds(int[] ids) {
        if (this.ids == null) {
            this.ids = ids;
            return;
        }

        int[] result = Arrays.copyOf(this.ids, this.ids.length + ids.length);
        System.arraycopy(ids, 0, result, this.ids.length, ids.length);
        this.ids = result;
    }

    /*
     * add full scan result (see int[] dbScanData in LibScan)
     * return false on error
     */
    public boolean addScanResult(int[] result) {
        if (result == null || result.length % 2 != 0) // see dbScanData comments
            return false;

        this.addTypes(Arrays.copyOf(result, result.length / 2));
        this.addIds(Arrays.copyOfRange(result, result.length / 2, result.length));

        return true;
    }

    //
    @Override
    public String toString() {
        if (Settings.DEBUG || Settings.DEBUG_POLICY) {
            String ids_s = "";
            if (this.ids != null) {
                int l = this.ids.length;
                for (int i = 0; i < l; ) {
                    ids_s += this.ids[i];
                    if (++i != l) ids_s += ", ";
                }
            }

            Set types = this.types;
            if (this.has_other && !this.has_white)
                types.remove(LibScan.RECORD_TYPE_CLEAN); // see hasType

            return "ScanResult: " + LibScan.recordTypesToString(types) +
                    ", m:" + this.major +
                    " w:" + this.has_white + " t:" + this.has_test +
                    " d:" + this.has_dangerous + " a:" + this.has_ads + " s:" + this.has_social +
                    " o:" + this.has_other +
                    " ids: [" + ids_s + "]";
        }

        return "";
    }
}
