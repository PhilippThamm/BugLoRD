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
 * Sokal fault localizer
 * 
 * @param <T>
 *            type used to identify nodes in the system
 */
public class Sokal<T> extends AbstractSpectrumBasedFaultLocalizer<T> {

    /**
     * Create fault localizer
     */
    public Sokal() {
        super();
    }

    @Override
    public double suspiciousness(final INode<T> node) {
        return new Double(2.0d * (node.getIF() + node.getNS()))
                / new Double(2.0d * (node.getIF() + node.getNS()) + node.getNF() + node.getIS());
    }

    @Override
    public String getName() {
        return "Sokal";
    }

}
