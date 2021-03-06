/**
 *
 */
package se.de.hu_berlin.informatik.spectra.util;

import org.junit.*;
import se.de.hu_berlin.informatik.utils.files.FileUtils;
import se.de.hu_berlin.informatik.utils.miscellaneous.TestSettings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Simon
 *
 */
public class CachedMapTest extends TestSettings {

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

    @Test
    public void cachedMapTest() {
        Path output1 = Paths.get(getStdTestDir(), "cacheMap", "map.zip");
        Path output2 = Paths.get(getStdTestDir(), "cacheMap", "mapTarget.zip");
        FileUtils.delete(output1);
        FileUtils.delete(output2);

        CachedMap<int[]> map = new CachedIntArrayMap(output1, 50, 500, "test", false);
        Map<Integer, int[]> checkMap = new HashMap<>();

        Random rand = new Random(12315415);

        map.put(17, new int[]{});

        map.get(17);

        for (int i = 0; i < 1000; ++i) {
            int length = rand.nextInt(100)+1;
            int[] array = new int[length];
            for (int j = 0; j < length; ++j) {
                array[j] = rand.nextInt();
            }
            int key = i;//rand.nextInt();
            map.put(key, array);
            checkMap.put(key, array);
        }
        
        checkIfEqual(map, checkMap);

        map.put(17, new int[]{});
        checkMap.put(17, new int[]{});
        
        checkIfEqual(map, checkMap);
        
        for (int i = 1001; i < 2000; ++i) {
            int length = rand.nextInt(100)+1;
            int[] array = new int[length];
            for (int j = 0; j < length; ++j) {
                array[j] = rand.nextInt();
            }
            int key = i;//rand.nextInt();
            map.put(key, array);
            checkMap.put(key, array);
        }
        
        map.remove(17);
        checkMap.remove(17);

        checkIfEqual(map, checkMap);
        
        for (int i = 2001; i < 3000; ++i) {
            int length = rand.nextInt(100)+1;
            int[] array = new int[length];
            for (int j = 0; j < length; ++j) {
                array[j] = rand.nextInt();
            }
            int key = i;//rand.nextInt();
            map.put(key, array);
            checkMap.put(key, array);
        }
        
        checkIfEqual(map, checkMap);
        
        Map<Integer, int[]> entriesToReplace = new HashMap<>();
        for (int i = 2000; i < 4000; i += 10) {
            int length = rand.nextInt(100)+1;
            int[] array = new int[length];
            for (int j = 0; j < length; ++j) {
                array[j] = rand.nextInt();
            }
            int key = rand.nextInt(10000);
            entriesToReplace.put(key, array);
            checkMap.put(key, array);
        }
		map.putAll(entriesToReplace);

		checkIfEqual(map, checkMap);
		
        map.moveMapContentsTo(output2);

        assertTrue(output2.toFile().exists());

        CachedMap<int[]> map2 = new CachedIntArrayMap(output2, 100, 500, "test", false);

        checkIfEqual(map2, checkMap);
    }

    private void checkIfEqual(CachedMap<int[]> map, Map<Integer, int[]> checkMap) {
        assertEquals(checkMap.size(), map.size());

        for (Entry<Integer, int[]> entry : checkMap.entrySet()) {
            assertTrue(map.containsKey(entry.getKey()));

            assertArrayEquals("key: " + entry.getKey() + ", length: " + entry.getValue().length, entry.getValue(), map.get(entry.getKey()));
        }
    }

}
