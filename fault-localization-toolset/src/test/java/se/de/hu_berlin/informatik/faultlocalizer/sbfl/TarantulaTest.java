/*
 * This file is part of the "STARDUST" project.
 *
 * (c) Fabian Keller <hello@fabian-keller.de>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package se.de.hu_berlin.informatik.faultlocalizer.sbfl;

import org.junit.Assert;
import org.junit.Test;
import se.de.hu_berlin.informatik.faultlocalizer.sbfl.localizers.Tarantula;
import se.de.hu_berlin.informatik.faultlocalizer.test.data.SimpleSpectraProvider;
import se.de.hu_berlin.informatik.spectra.core.INode;
import se.de.hu_berlin.informatik.spectra.core.hit.HitSpectra;
import se.de.hu_berlin.informatik.utils.experiments.ranking.Ranking;

import java.math.BigDecimal;

public class TarantulaTest {

    @Test
    public void check() {
        final HitSpectra<String> s = new SimpleSpectraProvider().loadHitSpectra();
        final Tarantula<String> fl = new Tarantula<>();
        final Ranking<INode<String>> r = fl.localize(s);
        double smallDelta = 0.00001;
        Assert.assertEquals(round(r.getRankingValue(s.getOrCreateNode("S1"))), 0.333, smallDelta);
        Assert.assertEquals(round(r.getRankingValue(s.getOrCreateNode("S2"))), 0.750, smallDelta);
        Assert.assertEquals(round(r.getRankingValue(s.getOrCreateNode("S3"))), 0.429, smallDelta);

        Assert.assertEquals(r.wastedEffort(s.getOrCreateNode("S1")), 2);
        Assert.assertEquals(r.wastedEffort(s.getOrCreateNode("S2")), 0);
        Assert.assertEquals(r.wastedEffort(s.getOrCreateNode("S3")), 1);
    }

    public static double round(final double d) {
        return round(d, 3);
    }

    /**
     * Round double to n decimal places
     *
     * @param d            something
     * @param decimalPlace something else
     * @return the result
     * @see {@linktourl http://stackoverflow.com/a/24780468/1262901}
     */
    public static double round(final double d, final int decimalPlace) {
        // see the Javadoc about why we use a String in the constructor
        // http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }
}
