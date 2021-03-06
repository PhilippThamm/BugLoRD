/**
 *
 */
package se.de.hu_berlin.informatik.spectra.provider.tracecobertura.coveragedata.tests;

import org.junit.*;
import se.de.hu_berlin.informatik.spectra.provider.tracecobertura.infrastructure.BufferedMap;
import se.de.hu_berlin.informatik.spectra.provider.tracecobertura.infrastructure.comptrace.RepetitionMarkerBufferedMap;

import java.io.File;


/**
 * @author SimHigh
 *
 */
public class BufferedMapTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    private File outputDir = new File("target" + File.separator + "bufferedMapTest");

    /**
     * Test method for {@link se.de.hu_berlin.informatik.spectra.provider.tracecobertura.infrastructure.BufferedMap#BufferedMap(java.io.File, java.lang.String)}.
     */
    @Test
    public void testBufferedMapFileString() throws Exception {
        BufferedMap<String> queue = new BufferedMap<>(outputDir, "test1", 5);

        for (int i = 0; i < 50; ++i) {
            queue.put(i * 10, String.valueOf(i * 10));
        }
        queue.sleep();

//		Assert.assertEquals(2, queue.get(2).intValue());
        Assert.assertEquals("30", queue.get(30));

    }

    @Test
    public void testBufferedMapRepMarkers() throws Exception {
        BufferedMap<int[]> queue = new RepetitionMarkerBufferedMap(outputDir, "test2", 5);

        for (int i = 0; i < 50; ++i) {
            queue.put(i * 10, new int[]{i, i + 1});
        }
        queue.sleep();

//		Assert.assertEquals(2, queue.get(2).intValue());
        Assert.assertArrayEquals(new int[]{3, 4}, queue.get(30));

    }

}
