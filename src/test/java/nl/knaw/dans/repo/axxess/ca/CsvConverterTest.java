package nl.knaw.dans.repo.axxess.ca;

import org.junit.jupiter.api.Test;

public class CsvConverterTest {


    @Test
    void createDb() throws Exception {
        CsvConverter converter = new CsvConverter();
        converter.builtDatabase();
    }
}
