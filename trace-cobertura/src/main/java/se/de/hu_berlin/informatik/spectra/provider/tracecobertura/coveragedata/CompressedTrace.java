package se.de.hu_berlin.informatik.spectra.provider.tracecobertura.coveragedata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An execution trace consists structurally of a list of executed nodes
 * and a list of tuples that mark repeated sequences in the trace.
 *
 */
public class CompressedTrace implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5903218865249529299L;
	
	private int originalSize;
	private int[][] compressedTrace;
	private int[] repetitionMarkers;
	
	private CompressedTrace child;
	
	private CompressedTrace(List<int[]> trace, CompressedTrace parent) {
		this.originalSize = trace.size();
		List<int[]> traceWithoutRepetitions = extractRepetitions(trace);
		trace = null;
		// did something change?
		if (originalSize == traceWithoutRepetitions.size()) {
			System.out.println("=> " + originalSize);
			// no... then just store the compressed trace
			this.compressedTrace = new int[traceWithoutRepetitions.size()][];
			for (int i = 0; i < traceWithoutRepetitions.size(); ++i) {
				compressedTrace[i] = traceWithoutRepetitions.get(i);
			}
		} else {
			System.out.println(originalSize + " -> " + traceWithoutRepetitions.size());
			// yes... then try again recursively
			this.child = new CompressedTrace(traceWithoutRepetitions, this);
		}
	}
	
	public CompressedTrace(List<int[]> trace) {
		this(trace, null);
	}
	
//	public ExecutionTrace(int[] trace, int[] repetitionMarkers) {
//		this.trace = trace;
//		this.repetitionMarkers = repetitionMarkers;
//		this.originalSize = computeFullTraceLength();
//	}
//
//	private int computeFullTraceLength() {
//		int length = trace.length;
//		for (int j = 0; j < repetitionMarkers.length; j += 3) {
//			// rangeStart, length, repetitionCount
//			length += (repetitionMarkers[j+1] * repetitionMarkers[j+2]) - 1;
//		}
//		return length;
//	}

	private List<int[]> extractRepetitions(List<int[]> trace2) {
		ArrayList<int[]> traceWithoutRepetitions = new ArrayList<>();
		List<Integer> traceRepetitions = new ArrayList<>();
		int currentIndex = 0;
		
		// mapping from elements to their most recent positions
		Map<List<Integer>,Integer> elementToPositionMap = new HashMap<>();
		int startingPosition = 0;
		for (int i = 0; i < trace2.size(); i++) {
			int[] element = trace2.get(i);
			List<Integer> repr = toList(element);

			// check for repetition of the current element
			Integer position = elementToPositionMap.get(repr);
			if (position == null) {
				// no repetition: remember position of element
				elementToPositionMap.put(repr, i);
			} else {
				// element was repeated
				// check if the sequence of elements between the last position of the element
				// and this position is the same as the following sequence
				int length = i - position;
				// only check if there would actually be enough space for a repeated sequence
				if (i + length - 1 < trace2.size()) {
					int repetitionCounter = 0;
					for (int pos = 0; i + pos < trace2.size(); ++pos) {
						int[] first = trace2.get(position + pos);
						int[] second = trace2.get(i + pos);
						if (first.length != second.length ||
								first[1] != second[1] || 
								first[0] != second[0]
										// || (first.length > 2 && first[2] != second[2])
										) {
							break;
						}
						// check for end of sequence
						if ((pos + 1) % length == 0) {
							++repetitionCounter;
						}
					}
					// are there any repetitions?
					if (repetitionCounter > 0) {
						traceWithoutRepetitions.ensureCapacity(
								traceWithoutRepetitions.size() + (position + length - startingPosition));
						// add the previous sequence
						for (int pos = startingPosition; pos < position; ++pos) {
							traceWithoutRepetitions.add(trace2.get(pos));
							trace2.set(pos, null);
						}
						currentIndex += position - startingPosition;
						
						// add a triplet to the list
						traceRepetitions.add(currentIndex);
						traceRepetitions.add(length);
						traceRepetitions.add(repetitionCounter + 1);
//						System.out.println("index: " + currentIndex + ", length: " + length + ", repetitions: " + (repetitionCounter+1));
						// add one repeated sequence to the trace
						for (int pos = position; pos < position + length; ++pos) {
							traceWithoutRepetitions.add(trace2.get(pos));
							trace2.set(pos, null);
						}
						currentIndex += length;
						// continue after the repeated sequences;
						// right now, i is at the beginning of the second repetition,
						// so move i by the count of repetitions decreased by one
						i += ((repetitionCounter) * length) - 1;
						// reset repetition recognition and the index to continue
						elementToPositionMap.clear();
						// reset the starting position (all previous sequences have been processed)
						startingPosition = i + 1;
					} else {
						// no repetitions found: update position of element;
						// this only stores the most recent position of each element
						elementToPositionMap.put(repr, i);
					}
				} else {
					// no repetitions possible: update position of element;
					// this only stores the most recent position of each element
					elementToPositionMap.put(repr, i);
				}
			}
		}
		
		// process remaining elements
		if (startingPosition < trace2.size()) {
			// there exists an unprocessed sequence 
			// before this element's position
			
			traceWithoutRepetitions.ensureCapacity(
					traceWithoutRepetitions.size() + (trace2.size() - startingPosition));
			// add the previous sequence
			for (int pos = startingPosition; pos < trace2.size(); ++pos) {
				traceWithoutRepetitions.add(trace2.get(pos));
				trace2.set(pos, null);
			}
			
			// forget all previously remembered positions of elements
			elementToPositionMap.clear();
		}

		if (!traceRepetitions.isEmpty()) {
			this.repetitionMarkers = new int[traceRepetitions.size()];
			for (int i = 0; i < traceRepetitions.size(); ++i) {
				repetitionMarkers[i] = traceRepetitions.get(i);
			}
		}
		return traceWithoutRepetitions;
	}

	private List<Integer> toList(int[] element) {
		ArrayList<Integer> result = new ArrayList<>(2);
		result.add(element[0]);
		result.add(element[1]);
		return result;
	}

	public int[][] getCompressedTrace() {
		if (child != null) {
			return child.getCompressedTrace();
		} else {
			return compressedTrace;
		}
	}

	public int[] getRepetitionMarkers() {
		return repetitionMarkers;
	}
	
	public int[][] reconstructFullTrace() {
		return reconstructTrace();
	}

	private int[][] reconstructTrace() {
		if (child == null) {
			return this.compressedTrace;
		}
		
		// got a child object? then reconstruct the trace...
		int[][] compressedTrace = child.reconstructTrace();

		int[][] result = new int[originalSize][];
		int startPos = 0;
		int currentIndex = 0;
		for (int j = 0; j < repetitionMarkers.length; j += 3) {
			// rangeStart, length, repetitionCount
			int unprocessedLength = repetitionMarkers[j] - startPos;
			if (unprocessedLength > 0) {
				// the previous sequence has not been repeated
				System.arraycopy(compressedTrace, startPos, result, currentIndex, unprocessedLength);
				for (int i = startPos; i < startPos + unprocessedLength; i++) {
					compressedTrace[i] = null;
				}
				// move the index for the result array
				currentIndex += unprocessedLength;
			}

			// add the repeated sequences
			for (int i = 0; i < repetitionMarkers[j+2]; ++i) {
				System.arraycopy(compressedTrace, repetitionMarkers[j], result, currentIndex, repetitionMarkers[j+1]);
				currentIndex += repetitionMarkers[j+1];
			}

			// move the start position in the source trace array
			startPos = repetitionMarkers[j] + repetitionMarkers[j+1];
			
			for (int i = repetitionMarkers[j]; i < startPos; i++) {
				compressedTrace[i] = null;
			}
		}

		if (startPos < compressedTrace.length) {
			// the remaining sequence has not been repeated
			System.arraycopy(compressedTrace, startPos, result, currentIndex, compressedTrace.length - startPos);
		}
		compressedTrace = null;

		return result;
	}
	
}