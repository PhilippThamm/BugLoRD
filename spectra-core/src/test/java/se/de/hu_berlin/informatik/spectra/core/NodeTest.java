/*
 * This file is part of the "STARDUST" project.
 *
 * (c) Fabian Keller <hello@fabian-keller.de>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package se.de.hu_berlin.informatik.spectra.core;

import org.junit.Assert;
import org.junit.Test;
import se.de.hu_berlin.informatik.spectra.core.hit.HitSpectra;
import se.de.hu_berlin.informatik.spectra.test.data.SimpleSpectraProvider;

public class NodeTest {

    private final double smallDelta = 0.00001;

    /**
     * Test data taken from Table 1 from:
     * <p>
     * Lee Naish, Hua Jie Lee, and Kotagiri Ramamohanarao. 2011. A model for spectra-based software diagnosis. ACM
     * Trans. Softw. Eng. Methodol. 20, 3, Article 11 (August 2011), 32 pages. DOI=10.1145/2000791.2000795
     * http://doi.acm.org/10.1145/2000791.2000795
     */
    @Test
    public void computeINFSMetricsForSimpleSpectra() {
        final HitSpectra<String> s = new SimpleSpectraProvider().loadHitSpectra();

        Assert.assertTrue(s.hasNode("S1"));
        Assert.assertTrue(s.hasNode("S2"));
        Assert.assertTrue(s.hasNode("S3"));

        Assert.assertEquals(s.getOrCreateNode("S1").getNP(ComputationStrategies.STANDARD_SBFL), 0, smallDelta);
        Assert.assertEquals(s.getOrCreateNode("S1").getNF(ComputationStrategies.STANDARD_SBFL), 1, smallDelta);
        Assert.assertEquals(s.getOrCreateNode("S1").getEP(ComputationStrategies.STANDARD_SBFL), 3, smallDelta);
        Assert.assertEquals(s.getOrCreateNode("S1").getEF(ComputationStrategies.STANDARD_SBFL), 1, smallDelta);

        Assert.assertEquals(s.getOrCreateNode("S2").getNP(ComputationStrategies.STANDARD_SBFL), 2, smallDelta);
        Assert.assertEquals(s.getOrCreateNode("S2").getNF(ComputationStrategies.STANDARD_SBFL), 0, smallDelta);
        Assert.assertEquals(s.getOrCreateNode("S2").getEP(ComputationStrategies.STANDARD_SBFL), 1, smallDelta);
        Assert.assertEquals(s.getOrCreateNode("S2").getEF(ComputationStrategies.STANDARD_SBFL), 2, smallDelta);

        Assert.assertEquals(s.getOrCreateNode("S3").getNP(ComputationStrategies.STANDARD_SBFL), 1, smallDelta);
        Assert.assertEquals(s.getOrCreateNode("S3").getNF(ComputationStrategies.STANDARD_SBFL), 1, smallDelta);
        Assert.assertEquals(s.getOrCreateNode("S3").getEP(ComputationStrategies.STANDARD_SBFL), 2, smallDelta);
        Assert.assertEquals(s.getOrCreateNode("S3").getEF(ComputationStrategies.STANDARD_SBFL), 1, smallDelta);
    }

    @Test
    public void computeForSpectraWithoutTraces() {
        final HitSpectra<String> s = new HitSpectra<>(null);
        final INode<String> n = s.getOrCreateNode("sampleNode");
        Assert.assertEquals(n.getNP(ComputationStrategies.STANDARD_SBFL), 0, smallDelta);
        Assert.assertEquals(n.getNF(ComputationStrategies.STANDARD_SBFL), 0, smallDelta);
        Assert.assertEquals(n.getEP(ComputationStrategies.STANDARD_SBFL), 0, smallDelta);
        Assert.assertEquals(n.getEF(ComputationStrategies.STANDARD_SBFL), 0, smallDelta);
    }
}
