/**
 * 
 */
package se.de.hu_berlin.informatik.rankingplotter.modules;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

import se.de.hu_berlin.informatik.rankingplotter.plotter.datatables.SinglePlotStatisticsCollection;
import se.de.hu_berlin.informatik.rankingplotter.plotter.datatables.SinglePlotStatisticsCollection.StatisticsCategories;
import se.de.hu_berlin.informatik.utils.fileoperations.FileUtils;
import se.de.hu_berlin.informatik.utils.fileoperations.csv.CSVUtils;
import se.de.hu_berlin.informatik.utils.tm.moduleframework.AbstractModule;

/**
 * @author Simon Heiden
 */
public class CsvToSingleStatisticsCollectionModule extends AbstractModule<File, SinglePlotStatisticsCollection> {

	private final String localizer;
	
	/**
	 * Creates a new {@link CsvToSingleStatisticsCollectionModule} object with the given parameters.
	 * @param localizer
	 * the current localizer
	 */
	public CsvToSingleStatisticsCollectionModule(String localizer) {
		super(true);
		this.localizer = localizer;
	}
	
	/* (non-Javadoc)
	 * @see se.de.hu_berlin.informatik.utils.tm.ITransmitter#processItem(java.lang.Object)
	 */
	public SinglePlotStatisticsCollection processItem(File csvFileLocation) {
		
		SinglePlotStatisticsCollection tables = new SinglePlotStatisticsCollection(localizer);
		
		//generate enum set with everything but unknown stuff
		EnumSet<StatisticsCategories> set = EnumSet.complementOf(EnumSet.of(StatisticsCategories.UNKNOWN));
		
		for (StatisticsCategories category : set) {
			File csvFile = FileUtils.searchFileContainingPattern(csvFileLocation, "_" + category + ".csv");
			if (csvFile == null) {
				//Log.err(this, "Couldn't find file '%s' in '%s'.", "_" + category + ".csv", csvFileLocation);
				continue;
			}
			
			List<Double[]> data = CSVUtils.readCSVFileToListOfDoubleArrays(csvFile.toPath());
			
			tables.setValueList(category, data);
		}
		
		return tables;
	}
	
}
