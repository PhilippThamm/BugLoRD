/*
 * This file is part of the "STARDUST" project.
 *
 * (c) Fabian Keller <hello@fabian-keller.de>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package se.de.hu_berlin.informatik.stardust.localizer.sbfl.localizers;

import se.de.hu_berlin.informatik.stardust.localizer.sbfl.AbstractSpectrumBasedFaultLocalizer;
import se.de.hu_berlin.informatik.stardust.spectra.INode;

/**
 * Tarantula fault localizer $\frac{\frac{\EF}{\EF+\NF}}{\frac{\EF}{\EF+\NF}+\frac{\EP}{\EP+\NP}}$
 * 
 * @param <T>
 *            type used to identify nodes in the system
 */
public class Tarantula<T> extends AbstractSpectrumBasedFaultLocalizer<T> {

    /**
     * Create fault localizer
     */
    public Tarantula() {
        super();
    }

    @Override
    public double suspiciousness(final INode<T> node) {
        double part = (double)node.getEF() / (double)(node.getEF() + node.getNF());
        if (node.getEF() == 0) {
        	return 0;
        }
        double part2 = (double)node.getEP() / (double)(node.getEP() + node.getNP());
        if (node.getEP() == 0) {
        	part2 = 0;
        }
        return part / (double)(part + part2);
    }

    @Override
    public String getName() {
        return "tarantula";
    }

}