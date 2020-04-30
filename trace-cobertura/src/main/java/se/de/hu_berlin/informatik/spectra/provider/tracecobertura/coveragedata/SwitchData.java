package se.de.hu_berlin.informatik.spectra.provider.tracecobertura.coveragedata;

import se.de.hu_berlin.informatik.spectra.provider.tracecobertura.data.BranchCoverageData;
import se.de.hu_berlin.informatik.spectra.provider.tracecobertura.data.CoverageIgnore;

import java.io.Serializable;
import java.util.Arrays;

@CoverageIgnore
public class SwitchData
        implements
        BranchCoverageData,
        Comparable<Object>,
        Serializable {
    private static final long serialVersionUID = 9;

    private final int switchNumber;

    private long defaultHits;

    private long[] hits;

    private int[] keys;

    private int maxBranches;

    public SwitchData(int switchNumber, int[] keys, int maxBranches) {
        this.switchNumber = switchNumber;
        defaultHits = 0;
        hits = new long[keys.length];
        Arrays.fill(hits, 0);
        this.keys = new int[keys.length];
        System.arraycopy(keys, 0, this.keys, 0, keys.length);
        this.maxBranches = maxBranches;
    }

    public SwitchData(int switchNumber, int min, int max, int maxBranches) {
        this.switchNumber = switchNumber;
        defaultHits = 0;
        hits = new long[max - min + 1];
        Arrays.fill(hits, 0);
        this.keys = new int[max - min + 1];
        for (int i = 0; min <= max; keys[i++] = min++) ;
        this.maxBranches = maxBranches;
    }

    public SwitchData(int switchNumber, int maxBranches) {
        this(switchNumber, new int[0], maxBranches);
    }

    public int compareTo(Object o) {
        if (!o.getClass().equals(SwitchData.class))
            return Integer.MAX_VALUE;
        return this.switchNumber - ((SwitchData) o).switchNumber;
    }

    void touchBranch(int branch, int new_hits) {
        if (branch == -1) {
            defaultHits += new_hits;
        } else {
            if (hits.length <= branch) {
                long[] old = hits;
                hits = new long[branch + 1];
                System.arraycopy(old, 0, hits, 0, old.length);
                Arrays.fill(hits, old.length, hits.length - 1, 0);
            }
            hits[branch] += new_hits;
        }
    }

    public int getSwitchNumber() {
        return this.switchNumber;
    }

    public long getHits(int branch) {
        return (hits.length > branch) ? hits[branch] : -1;
    }

    public long getDefaultHits() {
        return defaultHits;
    }

    public double getBranchCoverageRate() {
        int branches = getNumberOfValidBranches();
        int hit = (defaultHits > 0) ? 1 : 0;
        for (int i = hits.length - 1; i >= 0; hit += ((hits[i--] > 0) ? 1 : 0)) ;
        return ((double) hit) / branches;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if ((obj == null) || !(obj.getClass().equals(this.getClass())))
            return false;

        SwitchData switchData = (SwitchData) obj;
        return (this.defaultHits == switchData.defaultHits)
                && (Arrays.equals(this.hits, switchData.hits))
                && (this.switchNumber == switchData.switchNumber);
    }

    public int hashCode() {
        return this.switchNumber;
    }

    public int getNumberOfCoveredBranches() {
        int ret = (defaultHits > 0) ? 1 : 0;
        for (int i = hits.length - 1; i >= 0; i--) {
            if (hits[i] > 0)
                ret++;
        }
        return ret;
    }

    public int getNumberOfValidBranches() {
        return Math.min(hits.length + 1, maxBranches);
    }

    public void merge(BranchCoverageData coverageData) {
        SwitchData switchData = (SwitchData) coverageData;
        defaultHits += switchData.defaultHits;
        for (int i = Math.min(hits.length, switchData.hits.length) - 1; i >= 0; i--)
            hits[i] += switchData.hits[i];
        if (switchData.hits.length > hits.length) {
            long[] old = hits;
            hits = new long[switchData.hits.length];
            System.arraycopy(old, 0, hits, 0, old.length);
            System.arraycopy(switchData.hits, old.length, hits, old.length,
                    hits.length - old.length);
        }
        if ((this.keys.length == 0) && (switchData.keys.length > 0))
            this.keys = switchData.keys;
        maxBranches = Math.min(maxBranches, switchData.getMaxBranches());
    }

    public int getMaxBranches() {
        return maxBranches;
    }

    public void setMaxBranches(int maxBranches) {
        this.maxBranches = maxBranches;
    }
}

