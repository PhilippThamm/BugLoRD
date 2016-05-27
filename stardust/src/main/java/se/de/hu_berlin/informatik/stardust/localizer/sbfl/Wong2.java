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
 * Wong2 fault localizer
 * 
 * @param <T>
 *            type used to identify nodes in the system
 */
public class Wong2<T> extends AbstractSpectrumBasedFaultLocalizer<T> {

    /**
     * Create fault localizer
     */
    public Wong2() {
        super();
    }

    @Override
    public double suspiciousness(final INode<T> node) {
        return node.getIF() - node.getIS();
    }

    @Override
    public String getName() {
        return "Wong2";
    }

}
