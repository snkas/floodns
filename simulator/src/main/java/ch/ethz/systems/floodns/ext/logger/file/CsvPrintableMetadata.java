package ch.ethz.systems.floodns.ext.logger.file;

public interface CsvPrintableMetadata {

    /**
     * Convert whatever metadata contained in this class to a CSV valid string representation.
     * This can be useful to distinguish semantically different things in the info log.
     *
     * @return  CSV valid label, as such without comma, "\n", and "\r".
     */
    String toCsvValidLabel();

}
