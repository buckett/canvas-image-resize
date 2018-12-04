package uk.ac.ox.it;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * This writes a summary of the resizing out to a CSV.
 */
public class SummaryCsv implements Closeable {

    // If we are writing a summary it won't be null, otherwise will be null
    private CSVPrinter csvPrinter;

    public SummaryCsv(File output) throws IOException {
        if (output != null) {
            Appendable appendable;
            if ("-".equals(output.getName())) {
                appendable = new OutputStreamWriter(System.out);
            } else {
                appendable = new FileWriter(output);
            }
            csvPrinter = new CSVPrinter(appendable, CSVFormat.EXCEL);
            csvPrinter.printRecord("Course Name", "ID", "Original Size", "New Size");
        }
    }

    public void record(Object[] results) throws IOException {
        if (csvPrinter != null && results != null) {
            csvPrinter.printRecord(results);
        }
    }

    @Override
    public void close() throws IOException {
        if (csvPrinter != null) {
            csvPrinter.close();
        }
    }
}
