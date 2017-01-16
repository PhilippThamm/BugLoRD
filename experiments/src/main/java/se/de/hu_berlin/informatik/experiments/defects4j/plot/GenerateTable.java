/**
 * 
 */
package se.de.hu_berlin.informatik.experiments.defects4j.plot;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.cli.Option;

import se.de.hu_berlin.informatik.benchmark.api.defects4j.Defects4J;
import se.de.hu_berlin.informatik.benchmark.api.defects4j.Defects4J.Defects4JProperties;
import se.de.hu_berlin.informatik.experiments.defects4j.BugLoRD;
import se.de.hu_berlin.informatik.experiments.defects4j.BugLoRD.BugLoRDProperties;
import se.de.hu_berlin.informatik.rankingplotter.plotter.datatables.AveragePlotStatisticsCollection.StatisticsCategories;
import se.de.hu_berlin.informatik.utils.optionparser.OptionWrapperInterface;
import se.de.hu_berlin.informatik.utils.tm.pipeframework.AbstractPipe;
import se.de.hu_berlin.informatik.utils.tm.pipeframework.PipeLinker;
import se.de.hu_berlin.informatik.utils.tm.pipes.ListSequencerPipe;
import se.de.hu_berlin.informatik.utils.fileoperations.FileUtils;
import se.de.hu_berlin.informatik.utils.fileoperations.ListToFileWriterModule;
import se.de.hu_berlin.informatik.utils.fileoperations.csv.CSVUtils;
import se.de.hu_berlin.informatik.utils.miscellaneous.LaTexUtils;
import se.de.hu_berlin.informatik.utils.miscellaneous.Log;
import se.de.hu_berlin.informatik.utils.miscellaneous.MathUtils;
import se.de.hu_berlin.informatik.utils.miscellaneous.Misc;
import se.de.hu_berlin.informatik.utils.optionparser.OptionParser;
import se.de.hu_berlin.informatik.utils.optionparser.OptionWrapper;

/**
 * Generates plots of the experiments' results.
 * 
 * @author SimHigh
 */
public class GenerateTable {
	
	public static enum CmdOptions implements OptionWrapperInterface {
		/* add options here according to your needs */
		PROJECTS(Option.builder("p").longOpt("projects").hasArgs()
        		.desc("A list of projects to consider of the Defects4J benchmark. "
        		+ "Should be either 'Lang', 'Chart', 'Time', 'Closure', 'Mockito', 'Math' or "
        		+ "'super' for the super directory (only for the average plots). Set this to 'all' to "
        		+ "iterate over all projects (and the super directory).").build()),
        PERCENTAGES(Option.builder("pc").longOpt("percentages").hasArgs().required()
        		.desc("A list of percentage values to include in the table.").build());

		/* the following code blocks should not need to be changed */
		final private OptionWrapper option;

		//adds an option that is not part of any group
		CmdOptions(final String opt, final String longOpt, 
				final boolean hasArg, final String description, final boolean required) {
			this.option = new OptionWrapper(
					Option.builder(opt).longOpt(longOpt).required(required).
					hasArg(hasArg).desc(description).build(), NO_GROUP);
		}
		
		//adds an option that is part of the group with the specified index (positive integer)
		//a negative index means that this option is part of no group
		//this option will not be required, however, the group itself will be
		CmdOptions(final String opt, final String longOpt, 
				final boolean hasArg, final String description, int groupId) {
			this.option = new OptionWrapper(
					Option.builder(opt).longOpt(longOpt).required(false).
					hasArg(hasArg).desc(description).build(), groupId);
		}
		
		//adds the given option that will be part of the group with the given id
		CmdOptions(Option option, int groupId) {
			this.option = new OptionWrapper(option, groupId);
		}
		
		//adds the given option that will be part of no group
		CmdOptions(Option option) {
			this(option, NO_GROUP);
		}

		@Override public String toString() { return option.getOption().getOpt(); }
		@Override public OptionWrapper getOptionWrapper() { return option; }
	}
	
	private final static String SEP = File.separator;
	
	/**
	 * @param args
	 * -p project -b bugID
	 */
	public static void main(String[] args) {
		
		OptionParser options = OptionParser.getOptions("GenerateTable", false, CmdOptions.class, args);
		
		String[] projects = options.getOptionValues(CmdOptions.PROJECTS);
		boolean allProjects = false;
		if (projects != null) {
			allProjects = projects[0].equals("all");
		} else {
			projects = new String[0];
		}
		
		if (allProjects) {
			projects = Defects4J.getAllProjects();
		}
		
		String[] percentagesStrings = options.getOptionValues(CmdOptions.PERCENTAGES);
		Double[] percentages = new Double[percentagesStrings.length];
		for (int i = 0; i < percentages.length; ++i) {
			percentages[i] = Double.valueOf(percentagesStrings[i]);
		}
		Arrays.sort(percentages, (Double x, Double y) -> Double.compare(y, x));
		
		
		String[] localizers = BugLoRD.getValueOf(BugLoRDProperties.LOCALIZERS).split(" ");
		
		String outputDir = Defects4J.getValueOf(Defects4JProperties.PLOT_DIR);
		
		for (String project : projects) {
			
			boolean isProject = Defects4J.validateProject(project, false);
			
			if (!isProject && !project.equals("super")) {
				Log.abort(GenerateTable.class, "Project doesn't exist: '" + project + "'.");
			}

			Path plotDir = Paths.get(generatePlotOutputDir(isProject, project, outputDir));

			new PipeLinker().append(
					new ListSequencerPipe<String>(),
					new AbstractPipe<String, String[]>(true) {

						@Override
						public String[] processItem(String localizer) {
							localizer = localizer.toLowerCase(Locale.getDefault());
							File localizerDir = plotDir.resolve(localizer).toFile();
							if (!localizerDir.exists()) {
								Log.err(GenerateTable.class, "localizer directory doesn't exist: '" + localizerDir + "'.");
								return null;
							}
							
							File meanRankFile = FileUtils.searchFileContainingPattern(localizerDir, "_" + StatisticsCategories.MEAN_RANK + ".csv");
							if (meanRankFile == null) {
								Log.err(GenerateTable.class, "mean rank csv file doesn't exist for localizer '" + localizer + "'.");
								return null;
							}
							
							File meanFirstRankFile = FileUtils.searchFileContainingPattern(localizerDir, "_" + StatisticsCategories.MEAN_FIRST_RANK + ".csv");
							if (meanFirstRankFile == null) {
								Log.err(GenerateTable.class, "mean first rank csv file doesn't exist for localizer '" + localizer + "'.");
								return null;
							}
							
							String[] result = new String[percentages.length*2 + 3];
							int counter = 0;
							result[counter++] = getEscapedLocalizerName(localizer);
							
							{
								List<Double[]> meanRankList = CSVUtils.readCSVFileToListOfDoubleArrays(meanRankFile.toPath());
								Map<Double, Double> meanRankMap = new HashMap<>();
								for (Double[] array : meanRankList) {
									meanRankMap.put(array[0], array[1]);
								}
								Double fullValue = meanRankMap.get(100.0);
								if (fullValue == null) {
									Log.err(GenerateTable.class, "percentage value '100.0%' not existing in mean rank csv file.");
									return null;
								}
								Double bestValue = fullValue;
								for (double percentage : percentages) {
									Double value = meanRankMap.get(percentage);
									if (value == null) {
										Log.err(GenerateTable.class, "percentage value '" + percentage + "%' not existing in mean rank csv file.");
										return null;
									}
									if (value.compareTo(bestValue) < 0) {
										bestValue = value;
									}
								}
								for (double percentage : percentages) {
									Double value = meanRankMap.get(percentage);
									if (value.equals(bestValue)) {
										result[counter++] = "\\textbf{" + String.valueOf(MathUtils.roundToXDecimalPlaces(value, 1)) + "}";
									} else { 
										result[counter++] = String.valueOf(MathUtils.roundToXDecimalPlaces(value, 1));
									}
								}
								result[counter++] = String.valueOf(MathUtils.roundToXDecimalPlaces(-(bestValue.doubleValue() / fullValue.doubleValue() * 100.0 - 100.0), 1)) + "\\%";
							}

							{
								List<Double[]> meanFirstRankList = CSVUtils.readCSVFileToListOfDoubleArrays(meanFirstRankFile.toPath());
								Map<Double, Double> meanFirstRankMap = new HashMap<>();
								for (Double[] array : meanFirstRankList) {
									meanFirstRankMap.put(array[0], array[1]);
								}
								Double fullValue = meanFirstRankMap.get(100.0);
								if (fullValue == null) {
									Log.err(GenerateTable.class, "percentage value '100.0%' not existing in mean first rank csv file.");
									return null;
								}
								Double bestValue = fullValue;
								for (double percentage : percentages) {
									Double value = meanFirstRankMap.get(percentage);
									if (value == null) {
										Log.err(GenerateTable.class, "percentage value '" + percentage + "%' not existing in mean first rank csv file.");
										return null;
									}
									if (value.compareTo(bestValue) < 0) {
										bestValue = value;
									}
								}
								for (double percentage : percentages) {
									Double value = meanFirstRankMap.get(percentage);
									if (value.equals(bestValue)) {
										result[counter++] = "\\textbf{" + String.valueOf(MathUtils.roundToXDecimalPlaces(value, 1)) + "}";
									} else { 
										result[counter++] = String.valueOf(MathUtils.roundToXDecimalPlaces(value, 1));
									}
								}
								result[counter++] = String.valueOf(MathUtils.roundToXDecimalPlaces(-(bestValue.doubleValue() / fullValue.doubleValue() * 100.0 - 100.0), 1)) + "\\%";
							}

							return result;
						}
					},
					new AbstractPipe<String[], List<String>>(true) {
						Map<String, String[]> map = new HashMap<>();
						@Override
						public List<String> processItem(String[] item) {
							map.put(item[0], item);
							return null;
						}
						@Override
						public List<String> getResultFromCollectedItems() {
							String[] titleArray1 = new String[percentages.length*2 + 3];
							int counter = 0;
							titleArray1[counter++] = "\\hfill SBFL ranking metric \\hfill";
							for (int i = 0; i < percentages.length; ++i) {
								titleArray1[counter++] = "";
							}
							titleArray1[counter++] = "max";
							for (int i = 0; i < percentages.length; ++i) {
								titleArray1[counter++] = "";
							}
							titleArray1[counter++] = "max";
							map.put("1", titleArray1);
							
							String[] titleArray2 = new String[percentages.length*2 + 3];
							counter = 0;
							titleArray2[counter++] = "";
							for (double percentage : percentages) {
								titleArray2[counter++] = "$\\lambda=" + MathUtils.roundToXDecimalPlaces(percentage/100.0, 2) +"$";
							}
							titleArray2[counter++] = "improv.";
							for (double percentage : percentages) {
								titleArray2[counter++] = "$\\lambda=" + MathUtils.roundToXDecimalPlaces(percentage/100.0, 2) +"$";
							}
							titleArray2[counter++] = "improv.";
							map.put("2", titleArray2);
							
							return LaTexUtils.generateSimpleLaTexTable(Misc.sortByKeyToValueList(map));
						}
					},
					new ListToFileWriterModule<List<String>>(plotDir.resolve(project + "_bigTable.tex"), true)
					).submitAndShutdown(Arrays.asList(localizers));
		}

	}
	
	public static String generatePlotOutputDir(Boolean isProject, String project, String outputDir) {
		String plotOutputDir;
		if (isProject) {
			
			/* #====================================================================================
			 * # plot averaged rankings for given project
			 * #==================================================================================== */
			plotOutputDir = outputDir + SEP + "average" + SEP + project;

			
		} else { //given project name was "super"
			
			/* #====================================================================================
			 * # plot averaged rankings for super directory
			 * #==================================================================================== */
			plotOutputDir = outputDir + SEP + "average" + SEP + "super";

		}
		return plotOutputDir;
	}
	
	private static String getEscapedLocalizerName(String localizer) {
		return "\\" + localizer.replace("1","ONE").replace("2","TWO").replace("3","THREE");
	}
	
}