package nl.knaw.dans.repo.axxess;

import com.healthmarketscience.jackcess.Database;
import nl.knaw.dans.repo.axxess.core.AxxessException;
import nl.knaw.dans.repo.axxess.core.AxxessTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AxxessConverterTest {

    @BeforeEach
    private void beforeEach() throws Exception {
        File axxessDir = new File(AxxessConverter.DEFAULT_OUTPUT_DIRECTORY);
        assert !axxessDir.exists() || deleteDirectory(axxessDir);

        File testOutput = new File("target/test-output");
        assert !testOutput.exists() || deleteDirectory(testOutput);
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    @Test
    void convertFile() throws Exception {
        AxxessConverter converter = new AxxessConverter();
        String filename = "src/test/test-set/axxes/axess.accdb";
        List<File> csvFiles = converter.convert(filename);

        File axxessDir = new File(AxxessConverter.DEFAULT_OUTPUT_DIRECTORY);
        assertTrue(axxessDir.exists());
        File metadata = new File(axxessDir, "axess.accdb.metadata.csv");
        assertTrue(metadata.exists());
        File table3 = new File(axxessDir, "axess.accdb.Table3.csv");
        assertTrue(table3.exists());

        assertEquals(4, csvFiles.size());
        assertTrue(csvFiles.contains(metadata.getAbsoluteFile()));
        assertTrue(csvFiles.contains(table3.getAbsoluteFile()));

        try {
            converter.convert(filename);
            fail("Expected AxxessException: File exists");
        } catch (AxxessException e) {
            // excellent
        }
    }

    @Test
    void convertDirectory() throws Exception {
        AxxessConverter converter = new AxxessConverter();
        String filename = "src/test/test-set";
        List<File> csvFiles = converter.convert(filename);

        File axxessDir = new File(AxxessConverter.DEFAULT_OUTPUT_DIRECTORY);
        assertTrue(axxessDir.exists());
        assertEquals(80, csvFiles.size());
    }

    @Test
    void convertDirectoryToTargetDir() throws Exception {
        AxxessConverter converter = new AxxessConverter()
          .withTargetDirectory("target/test-output");
        String filename = "src/test/test-set";
        converter.convert(filename);

        File targetDir = new File("target/test-output");
        assertTrue(targetDir.exists());
        File testSet = new File(targetDir, "test-set");
        assertTrue(testSet.exists());
    }

    @Test
    void convertDirectoryToTargetDirAndArchive() throws Exception {
        AxxessConverter converter = new AxxessConverter()
          .withTargetDirectory("target/test-output")
          .withArchiveResults(true)
          .withCompressArchive(true);
        String filename = "src/test/test-set";
        List<File> zipFiles = converter.convert(filename);

        File targetDir = new File("target/test-output");
        assertTrue(targetDir.exists());
        File testSet = new File(targetDir, "test-set");
        assertTrue(testSet.exists());
        File axesszip = new File(testSet, "axxes/axess.accdb.csv.zip");
        assertTrue(axesszip.exists());
    }
}
