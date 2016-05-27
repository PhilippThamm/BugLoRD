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
 * Rogot1 fault localizer
 * 
 * @param <T>
 *            type used to identify nodes in the system
 */
public class Rogot1<T> extends AbstractSpectrumBasedFaultLocalizer<T> {

    /**
     * Create fault localizer
     */
    public Rogot1() {
        super();
    }

    @Override
    public double suspiciousness(final INode<T> node) {
        final double left = new Double(node.getIF()) / new Double(2.0d * node.getIF() + node.getNF() + node.getIS());
        final double right = new Double(node.getNS()) / new Double(2.0d * node.getNS() + node.getNF() + node.getIS());
        return 0.5d * (left + right);
    }

    @Override
    public String getName() {
        return "Rogot1";
    }

}
