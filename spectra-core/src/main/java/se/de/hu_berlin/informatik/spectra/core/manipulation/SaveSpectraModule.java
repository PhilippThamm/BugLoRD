package se.de.hu_berlin.informatik.spectra.core.manipulation;

import se.de.hu_berlin.informatik.spectra.core.ISpectra;
import se.de.hu_berlin.informatik.spectra.util.Indexable;
import se.de.hu_berlin.informatik.spectra.util.SpectraFileUtils;
import se.de.hu_berlin.informatik.utils.files.FileUtils;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.processors.AbstractProcessor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Saves a Spectra object and forwards it to the output.
 *
 * @param <T> the type of nodes in the spectra
 * @author Simon Heiden
 */
public class SaveSpectraModule<T extends Indexable<T>> extends AbstractProcessor<ISpectra<T, ?>, ISpectra<T, ?>> {

    final private Path output;

    public SaveSpectraModule(final Path output) {
        super();
        this.output = output;
    }

    /* (non-Javadoc)
     * @see se.de.hu_berlin.informatik.utils.tm.ITransmitter#processItem(java.lang.Object)
     */
    @Override
    public ISpectra<T, ?> processItem(final ISpectra<T, ?> input) {
//		if (input.isEmpty()) {
//			Log.out(this, "Spectra is empty and will not be saved.");
//			return null;
//		} else {
        Log.out(this, "Saving spectra...");

        if (output.toFile().exists()) {
            // we might be overwriting a spectra file that we are still loading stuff from...
            Path tempOut = output.getParent().resolve("tmp_spectra.zip");
            FileUtils.delete(tempOut);
            SpectraFileUtils.saveSpectraToZipFile(input, tempOut, true, true, true);
            try {
                output.toFile().delete();
                FileUtils.copyFileOrDir(tempOut.toFile(), output.toFile(), StandardCopyOption.REPLACE_EXISTING);
                tempOut.toFile().delete();
            } catch (IOException e) {
                Log.err(this, e, "Could not copy temporary spectra output '%s' to '%s'.", tempOut, output);
            }
        } else {
            SpectraFileUtils.saveSpectraToZipFile(input, output, true, true, true);
        }
//		}
        return input;
    }

}
