/*
 * This file is part of the "STARDUST" project. (c) Fabian Keller
 * <hello@fabian-keller.de> For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */

package se.de.hu_berlin.informatik.spectra.core.hit;

import se.de.hu_berlin.informatik.spectra.core.AbstractHierarchicalSpectra;
import se.de.hu_berlin.informatik.spectra.core.ISpectra;
import se.de.hu_berlin.informatik.spectra.core.ITrace;

import java.nio.file.Path;

/**
 * @param <P> parent node identifier type
 * @param <C> child node identifier type
 */
public class HierarchicalHitSpectra<P, C> extends AbstractHierarchicalSpectra<P, C, HierarchicalHitTrace<P, C>> {

//	/**
//	 * Creates a new parent spectra object.
//	 *
//	 * @param childSpectra
//	 * the child spectra to fetch involvement information from
//	 */
//	public HierarchicalHitSpectra(final ISpectra<C, ?> childSpectra) {
//		super(childSpectra);
//	}

    public HierarchicalHitSpectra(final ISpectra<C, ?> childSpectra, Path spectraZipFilePath) {
        super(childSpectra, spectraZipFilePath);
    }

    @Override
    protected HierarchicalHitTrace<P, C> createNewHierarchicalTrace(
            AbstractHierarchicalSpectra<P, C, HierarchicalHitTrace<P, C>> abstractHierarchicalSpectra,
            ITrace<C> childTrace) {
        return new HierarchicalHitTrace<>(abstractHierarchicalSpectra, childTrace);
    }

}
