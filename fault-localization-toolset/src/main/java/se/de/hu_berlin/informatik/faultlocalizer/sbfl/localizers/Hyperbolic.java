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
 * Hyperbolic function fault localizer $\frac{1}{K_1 + \frac{\NF}{\EF + \NF}} + \frac{K_3}{K_2 + \frac{\EP}{\EF + \EP}}$
 * <p>
 * K_1 and K_2 should range from 0 to 100.
 * K_3 should range from 0 to 2.
 *
 * @param <T> type used to identify nodes in the system
 */
public class Hyperbolic<T> extends AbstractFaultLocalizer<T> {

    private static final double SMALL_DELTA = 1.0E-6;
    private final double k1;
    private final double k2;
    private final double k3;

    public Hyperbolic() {
        // mean values of received data...
        //this(3.149, 4.296, 0.318);
        // first training set coefficients from received data
        this(5.72, 4.038, 0.113);
    }

    public Hyperbolic(double k1, double k2, double k3) {
        super();
        this.k1 = k1;
        this.k2 = k2;
        this.k3 = k3;
    }

    @Override
    public double suspiciousness(final INode<T> node, ComputationStrategies strategy) {
        return 1.0 / (k1 + SMALL_DELTA + (node.getNF(strategy) / (node.getEF(strategy) + node.getNF(strategy) + SMALL_DELTA))) +
                k3 / (k2 + SMALL_DELTA + (node.getEP(strategy) / (node.getEF(strategy) + node.getEP(strategy) + SMALL_DELTA)));
    }

}
