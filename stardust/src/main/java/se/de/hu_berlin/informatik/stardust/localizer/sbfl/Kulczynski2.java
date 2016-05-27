/*
 * This file is part of the "STARDUST" project.
 *
 * (c) Fabian Keller <hello@fabian-keller.de>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package se.de.hu_berlin.informatik.stardust.localizer.sbfl;

import se.de.hu_berlin.informatik.stardust.traces.INode;

/**
 * Kulczynski2 fault localizer
 * 
 * @param <T>
 *            type used to identify nodes in the system
 */
public class Kulczynski2<T> extends AbstractSpectrumBasedFaultLocalizer<T> {

    /**
     * Create fault localizer
     */
    public Kulczynski2() {
        super();
    }

    @Override
    public double suspiciousness(final INode<T> node) {
        final Double left = new Double(node.getIF()) / new Double(node.getIF() + node.getNF());
        final Double right = new Double(node.getIF()) / new Double(node.getIF() + node.getIS());
        return 0.5d * (left + right);
    }

    @Override
    public String getName() {
        return "Kulczynski2";
    }

}
