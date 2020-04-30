package se.de.hu_berlin.informatik.gen.ranking;

import org.junit.*;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.rules.ExpectedException;
import se.de.hu_berlin.informatik.gen.ranking.Coverage2Ranking.CmdOptions;
import se.de.hu_berlin.informatik.utils.miscellaneous.Abort;
import se.de.hu_berlin.informatik.utils.miscellaneous.TestSettings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * @author Simon
 */
public class Coverage2RankingTest extends TestSettings {

    /**
     *
     */
    @BeforeClass
    public static void setUpBeforeClass() {
    }

    /**
     *
     */
    @AfterClass
    public static void tearDownAfterClass() {
        deleteTestOutputs();
    }

    /**
     *
     */
    @Before
    public void setUp() {
    }

    /**
     *
     */
    @After
    public void tearDown() {
        deleteTestOutputs();
    }

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    /**
     * Test method for {@link se.de.hu_berlin.informatik.gen.ranking.Coverage2Ranking#main(java.lang.String[])}.
     */
    @Test
    public void testMainRankingGeneration() {
        String[] args = {
                CmdOptions.INPUT.asArg(), getStdResourcesDir() + File.separator + "coverage-xml",
                CmdOptions.LOCALIZERS.asArg(), "tarantula", "jaccard",
                CmdOptions.OUTPUT.asArg(), getStdTestDir() + File.separator + "rankings"};
        Coverage2Ranking.main(args);
        assertTrue(Files.exists(Paths.get(getStdTestDir(), "rankings", "tarantula", "ranking.rnk")));
        assertTrue(Files.exists(Paths.get(getStdTestDir(), "rankings", "jaccard", "ranking.rnk")));
    }

    /**
     * Test method for {@link se.de.hu_berlin.informatik.gen.ranking.Coverage2Ranking#main(java.lang.String[])}.
     */
    @Test
    public void testMainRankingGenerationNoLocalizers() {
        String[] args = {
                CmdOptions.INPUT.asArg(), getStdResourcesDir() + File.separator + "coverage-xml",
                CmdOptions.OUTPUT.asArg(), getStdTestDir() + File.separator + "rankings"};
        Coverage2Ranking.main(args);
        assertTrue(Files.exists(Paths.get(getStdTestDir(), "rankings", "spectraCompressed.zip")));
    }

    /**
     * Test method for {@link se.de.hu_berlin.informatik.gen.ranking.Coverage2Ranking#main(java.lang.String[])}.
     */
    @Test
    public void testMainRankingGenerationWrongLocalizer() {
        String[] args = {
                CmdOptions.INPUT.asArg(), getStdResourcesDir() + File.separator + "coverage-xml",
                CmdOptions.LOCALIZERS.asArg(), "tarantulululula",
                CmdOptions.OUTPUT.asArg(), getStdTestDir() + File.separator + "rankings"};
        exception.expect(Abort.class);
        Coverage2Ranking.main(args);
    }

}
