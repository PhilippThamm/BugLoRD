/**
 * 
 */
package se.de.hu_berlin.informatik.c2r.modules;

import java.nio.file.Path;

import se.de.hu_berlin.informatik.stardust.spectra.ISpectra;
import se.de.hu_berlin.informatik.stardust.spectra.manipulation.FilterSpectraModule;
import se.de.hu_berlin.informatik.stardust.util.Indexable;
import se.de.hu_berlin.informatik.stardust.util.SpectraUtils;
import se.de.hu_berlin.informatik.utils.tm.moduleframework.AbstractModule;

/**
 * Saves a Spectra object and forwards it to the output.
 * 
 * @author Simon Heiden
 */
public class SaveFilteredSpectraModule<T extends Indexable<T>> extends AbstractModule<ISpectra<T>, ISpectra<T>> {
	
	final private Path output;
	final private T dummy;

	public SaveFilteredSpectraModule(T dummy, final Path output) {
		super(true);
		this.output = output;
		this.dummy = dummy;
	}

	/* (non-Javadoc)
	 * @see se.de.hu_berlin.informatik.utils.tm.ITransmitter#processItem(java.lang.Object)
	 */
	@Override
	public ISpectra<T> processItem(final ISpectra<T> input) {
		ISpectra<T> filteredSpectra = new FilterSpectraModule<T>().submit(input).getResult();
		SpectraUtils.saveSpectraToZipFile(dummy, filteredSpectra, output, true, true, true);
		return input;
	}

}
