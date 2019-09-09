/*
 * This file is part of the "STARDUST" project.
 *
 * (c) Fabian Keller <hello@fabian-keller.de>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package se.de.hu_berlin.informatik.faultlocalizer.sbfl.localizers;

import se.de.hu_berlin.informatik.faultlocalizer.sbfl.AbstractFaultLocalizer;
import se.de.hu_berlin.informatik.spectra.core.ComputationStrategies;
import se.de.hu_berlin.informatik.spectra.core.INode;

/**
 * Scott fault localizer $\frac{4\EF\NP - 4\NF\EP - (\NF-\EP)^2}{(2\EF+\NF+\EP)\cdot(2\NP+\NF+\EP)}$
 * 
 * @param <T>
 *            type used to identify nodes in the system
 */
public class Scott<T> extends AbstractFaultLocalizer<T> {

    /**
     * Create fault localizer
     */
    public Scott() {
        super();
    }

    @Override
    public double suspiciousness(final INode<T> node, ComputationStrategies strategy) {
        final double enu1 = 4.0d * node.getEF(strategy) * node.getNP(strategy);
        final double enu2 = 4.0d * node.getNF(strategy) * node.getEP(strategy);
        final double enu3 = node.getNF(strategy) - node.getEP(strategy);
        final double enu = enu1 - enu2 - (enu3 * enu3);

        final double denom1 = 2.0d * node.getEF(strategy) + node.getNF(strategy) + node.getEP(strategy);
        final double denom2 = 2.0d * node.getNP(strategy) + node.getNF(strategy) + node.getEP(strategy);
        final double denom = denom1 * denom2;

        if (enu == 0) {
    		return 0;
    	}
        return enu / denom;
    }
    
}
