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
 * Ochiai fault localizer $\frac{\EF}{\sqrt{(\EF+\NF)\cdot(\EF+\EP)}}$
 *
 * @param <T> type used to identify nodes in the system
 */
public class Ochiai<T> extends AbstractFaultLocalizer<T> {

    /**
     * Create fault localizer
     */
    public Ochiai() {
        super();
    }

    @Override
    public double suspiciousness(final INode<T> node, ComputationStrategies strategy) {
        if (node.getEF(strategy) == 0) {
            return 0;
        }
        return node.getEF(strategy)
                / Math.sqrt((node.getEF(strategy) + node.getNF(strategy)) * (node.getEF(strategy) + node.getEP(strategy)));
    }

}
