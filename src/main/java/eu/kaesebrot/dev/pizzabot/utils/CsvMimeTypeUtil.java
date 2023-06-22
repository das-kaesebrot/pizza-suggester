package eu.kaesebrot.dev.pizzabot.utils;

import java.util.List;

public final class CsvMimeTypeUtil {
    private CsvMimeTypeUtil() {}

    public static boolean MimeTypeCouldBeCsv(String mimetypeString) {
        return List.of(
                "text/plain",
                "text/x-csv",
                "application/vnd.ms-excel",
                "application/csv",
                "application/x-csv",
                "text/csv",
                "text/comma-separated-values",
                "text/x-comma-separated-values"
        )
        .contains(mimetypeString);
    }
}
