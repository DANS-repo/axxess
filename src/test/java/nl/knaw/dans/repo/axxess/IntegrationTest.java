package nl.knaw.dans.repo.axxess;

import com.healthmarketscience.jackcess.Database;
import nl.knaw.dans.repo.axxess.acc2csv.AxxessToCsvConverter;
import nl.knaw.dans.repo.axxess.csv2acc.Csv2AxxessConverter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrationTest {

    private static File baseDirectory = new File("src/test/resources/integration").getAbsoluteFile();

    private static String[][] databases = {
      // File format: V1997 [VERSION_3]   AccessVersion: 07.53
      {"avereest", "avereest.mdb",
        "https://easy.dans.knaw.nl/ui/rest/datasets/61704/files/4917456/content"},

      // File format: V2000 [VERSION_4]   AccessVersion: 08.50
      {"walcheren", "Boedelbestand Walcheren 1755-1855.MDB",
        "https://easy.dans.knaw.nl/ui/rest/datasets/48968/files/2964358/content"},

      // File format: V2007 [VERSION_12]  AccessVersion: 09.50
      {"kohier", "KOHIER1748.accdb",
        "https://easy.dans.knaw.nl/ui/rest/datasets/48078/files/2804052/content"},

      // File format: V2000 [VERSION_4]  AccessVersion: 08.50
      {"types", "all_datatypes.mdb", ""},

      {"types2", "decimal_types.accdb", ""},

      {"cliwoc", "CLIWOC21_97.mdb",
        "https://easy.dans.knaw.nl/ui/rest/datasets/40826/files/2462445/content"},

      {"webfaq", "AccWebFAQ.mdb", "http://access.mvps.org/access/downloads/accwebfaq-10-10-00-A8.zip"},

      {"kb", "KB.mdb", "http://www.theaccessweb.com/downloads/kb.zip"},

      {"polyglot", "Polyglot.mdb", "http://www.theaccessweb.com/downloads/Polygloth_pt.zip"},

      {"medicare", "DFCompare.mdb", "https://data.medicare.gov/data/dialysis-facility-compare"},

      {"article17", "Art17_MS_EU27_2015.mdb",
        "https://www.eea.europa.eu/data-and-maps/data/article-17-database-habitats-directive-92-43-eec-1/"}

    };

    @BeforeAll
    static void beforeAll() throws Exception {
        for (String[] name : databases) {
            assert deleteDirectory(getAcc2csvDir(name[0]));
            assert deleteDirectory(getCsv2accDir(name[0]));
            assert deleteDirectory(getAcc2csv2Dir(name[0]));
            File dbFile = FileUtils.getFile(baseDirectory, name[0], "db", name[1]);
            if (!dbFile.exists()) {
                loadFromUrl(name[2], dbFile);
            }
        }
    }

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        if (directoryToBeDeleted.exists()) {
            File[] allContents = directoryToBeDeleted.listFiles();
            if (allContents != null) {
                for (File file : allContents) {
                    deleteDirectory(file);
                }
            }
            return directoryToBeDeleted.delete();
        }
        return true;
    }

    private static File getDbFile(String name) throws IOException {
        File dbDir = FileUtils.getFile(baseDirectory, name, "db");
        File[] files = dbDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                for (Database.FileFormat fm : Database.FileFormat.values()) {
                    if (name.toLowerCase().endsWith(fm.getFileExtension())) {
                        return true;
                    }
                }
                return false;
            }
        });
        if (files == null || files.length < 1) {
            throw new IOException("dbFile does not exist: " + name);
        }
        return files[0];
    }

    private static File getZipFile(String name) throws IOException {
        String zipFilename = getDbName(name) + ".csv.zip";
        return new File(getAcc2csvZipDir(name), zipFilename);
    }

    private static File getMetadataFile(String name) throws IOException {
        String metadataFilename = getDbName(name) + "._metadata.csv";
        return new File(getAcc2csvFilesDir(name), metadataFilename);
    }

    private static File getAcc2csvDir(String name) {
        return FileUtils.getFile(baseDirectory, name, "acc2csv");
    }

    private static File getAcc2csv2Dir(String name) {
        return FileUtils.getFile(baseDirectory, name, "acc2csv2");
    }

    private static File getAcc2csvZipDir(String name) {
        return new File(getAcc2csvDir(name), "zipped");
    }

    private static File getAcc2csvFilesDir(String name) {
        return new File(getAcc2csvDir(name), "files");
    }

    private static File getCsv2accDir(String name) {
        return FileUtils.getFile(baseDirectory, name, "csv2acc");
    }

    private static File getCsv2accDb(String name) throws IOException {
        return FileUtils.getFile(getCsv2accDir(name), "files", getDbName(name) + ".accdb");
    }

    private static String getDbName(String name) throws IOException {
        return getDbFile(name).getName();
    }

    private static void loadFromUrl(String urlString, File file) throws IOException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        File dir = file.getParentFile();
        assert dir.exists() || dir.mkdirs();
        assert file.exists() || file.createNewFile();

        BufferedInputStream bif = null;
        BufferedOutputStream bof = null;
        System.out.print("Downloading " + file.getName());

        try {
            bif = new BufferedInputStream(conn.getInputStream());
            bof = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[1024];
            int read = 0;
            int total = 0;
            while ((read = bif.read(buffer, 0, buffer.length)) != -1) {
                bof.write(buffer, 0, read);
                total += read;
                if (total % 100000 == 0) {
                    System.out.print(" |");
                }
            }
            System.out.println("\nDownloaded: " + total + " bytes");
        } finally {
            if (bof != null) {
                bof.close();
            }
            if (bif != null) {
                bif.close();
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////

    @Disabled
    @Test
    void acc2csv2acc() throws Exception {
        for (String[] name : databases) {
            File dbFile = FileUtils.getFile(baseDirectory, name[0], "db", name[1]);
            if (!dbFile.exists()) {
                System.out.println("dbFile does not exist: " + dbFile);
            } else {
                //acc2csvZipped(name[0]);
                List<File> list1 = acc2csvFiled(name[0]);
                csv2acc(name[0]);
                acc2csvAgain(name[0]);

                compareConvertedFiles(list1);
            }
        }
    }

    private void acc2csvZipped(String name) throws Exception {
        AxxessToCsvConverter converter = new AxxessToCsvConverter()
          .withTargetDirectory(getAcc2csvZipDir(name))
          .setArchiveResults(true)
          .setCompressArchive(true)
          .setIncludeManifest(true);
        List<File> fileList = converter.convert(getDbFile(name));
        assertEquals(1, fileList.size());
        assertEquals(getZipFile(name), fileList.get(0));
        assertEquals(1, converter.getConvertedDatabaseCount());
        assertEquals(0, converter.getErrorCount());
    }

    private List<File> acc2csvFiled(String name) throws Exception {
        AxxessToCsvConverter converter = new AxxessToCsvConverter()
          .withTargetDirectory(getAcc2csvFilesDir(name))
          .setIncludeManifest(true);
        List<File> fileList = converter.convert(getDbFile(name));
        assertTrue(fileList.contains(getMetadataFile(name)));
        assertTrue(getMetadataFile(name).exists());
        assertEquals(1, converter.getConvertedDatabaseCount());
        assertEquals(0, converter.getErrorCount());

        return fileList;
    }

    private void csv2acc(String name) throws Exception {
        Csv2AxxessConverter converter = new Csv2AxxessConverter()
          .withTargetDirectory(getCsv2accDir(name))
          .withTargetDatabaseFileFormat(Database.FileFormat.V2010)
          .setIncludeIndexes(false)
          .setAutoNumberColumns(false)
          .setIncludeManifest(true);
        List<File> fileList = converter.convert(getAcc2csvFilesDir(name));
        assertEquals(2, fileList.size());
        assertEquals(1, converter.getConvertedDatabaseCount());
        assertEquals(0, converter.getErrorCount());
    }

    private void acc2csvAgain(String name) throws Exception {
        AxxessToCsvConverter converter = new AxxessToCsvConverter()
          .withTargetDirectory(getAcc2csv2Dir(name))
          .setIncludeManifest(true);
        List<File> fileList = converter.convert(getCsv2accDb(name));
        assertEquals(1, converter.getConvertedDatabaseCount());
        assertEquals(0, converter.getErrorCount());

    }

    private void compareConvertedFiles(List<File> list1) throws IOException {
        for (File file : list1) {
            if (isTableFile(file)) {
                File other = findOther(file);
                List<String> diffs = listDiffs(file, other);
                for (String diff : diffs) {
                    System.err.println(diff);
                }
                assertEquals(0, diffs.size());
            }
        }
    }

    private File findOther(File file) {
        File dir1 = file.getParentFile().getParentFile();
        File dir2 = new File(dir1.getAbsoluteFile() + "2");
        String name2 = file.getName().replaceAll("\\.mdb\\.", ".mdb.accdb.");
        File file2 = new File(dir2, name2);
        if (file2.exists()) {
            return file2;
        }
        name2 = file.getName().replaceAll("\\.MDB\\.", ".MDB.accdb.");
        file2 = new File(dir2, name2);
        if (file2.exists()) {
            return file2;
        }
        name2 = file.getName().replaceAll("\\.accdb\\.", ".accdb.accdb.");
        file2 = new File(dir2, name2);
        if (file2.exists()) {
            return file2;
        } else {
            throw new IllegalStateException("Could not find other file: " + file);
        }
    }

    private boolean isTableFile(File file) {
        return !file.getName().endsWith(".txt") && !file.getName().endsWith("_metadata.csv");
    }

    private List<String> listDiffs(File f1, File f2) throws IOException {
        // many thanks: http://www.java2s.com/Tutorial/Java/0180__File/Comparetextfilelinebyline.htm
        List<String> diffs = new ArrayList<>();
        InputStreamReader insr1 = new InputStreamReader(new FileInputStream(f1));
        InputStreamReader insr2 = new InputStreamReader(new FileInputStream(f2));
        String name1 = f1.getName();
        String name2 = f2.getName();

        LineNumberReader reader1 = new LineNumberReader(insr1);
        LineNumberReader reader2 = new LineNumberReader(insr2);
        String line1 = reader1.readLine();
        String line2 = reader2.readLine();
        while (line1 != null && line2 != null) {
            if (!line1.equals(line2)) {
                diffs.add("File \"" + name1 + "\" and file \"" +
                  name2 + "\" differ at line " + reader1.getLineNumber() +
                  ":" + "\n" + line1 + "\n" + line2);
                break;
            }
            line1 = reader1.readLine();
            line2 = reader2.readLine();
        }
        if (line1 == null && line2 != null) {
            diffs.add("File \"" + name2 + "\" has extra lines at line " +
              reader2.getLineNumber() + ":\n" + line2);
        }
        if (line1 != null && line2 == null) {
            diffs.add("File \"" + name1 + "\" has extra lines at line " +
              reader1.getLineNumber() + ":\n" + line1);
        }
        reader1.close();
        reader2.close();
        return diffs;
    }


}
