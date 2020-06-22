package ch.ethz.systems.floodns.ext.metadata;

import ch.ethz.systems.floodns.core.Metadata;
import ch.ethz.systems.floodns.ext.logger.file.CsvPrintableMetadata;

public class SimpleStringMetadata extends Metadata implements CsvPrintableMetadata {

    private final String label;

    /**
     * Constructor.
     *
     * @param label     String with does not contain "," / "\n" or "\r"
     */
    public SimpleStringMetadata(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    @Override
    public String toCsvValidLabel() {
        return this.label;
    }

}
