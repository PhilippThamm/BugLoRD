/*
 * This file is part of the "STARDUST" project. (c) Fabian Keller
 * <hello@fabian-keller.de> For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */

package se.de.hu_berlin.informatik.stardust.localizer;

import se.de.hu_berlin.informatik.stardust.localizer.sbfl.AbstractSpectrumBasedFaultLocalizer.ComputationStrategies;
import se.de.hu_berlin.informatik.stardust.spectra.INode;
import se.de.hu_berlin.informatik.stardust.spectra.ISpectra;
import se.de.hu_berlin.informatik.utils.experiments.ranking.Ranking;

/**
 * Interface used to implement fault localization algorithms.
 * 
 * @param <T>
 * type used to identify nodes in the system
 */
public interface IFaultLocalizer<T> {

	/**
	 * Returns a human-understandable name of this fault localizer.
	 * 
	 * @return name
	 */
	String getName();

	/**
	 * Creates a fault location ranking for all nodes in the given spectra.
	 * 
	 * @param spectra
	 * the spectra to perform the fault localization on
	 * @return nodes ranked by suspiciousness of actually causing the failure
	 */
	default Ranking<INode<T>> localize(ISpectra<T,?> spectra) {
		return localize(spectra, ComputationStrategies.STANDARD_SBFL);
	}

	/**
	 * Creates a fault location ranking for all nodes in the given spectra.
	 * 
	 * @param spectra
	 * the spectra to perform the fault localization on
	 * @param strategy
	 * the strategy to use for computation
	 * @return nodes ranked by suspiciousness of actually causing the failure
	 */
	Ranking<INode<T>> localize(ISpectra<T,?> spectra, ComputationStrategies strategy);
}
