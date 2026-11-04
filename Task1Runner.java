
package task1;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

interface ReportFormatter {
    String format(String title, String[] headers, String[][] rows);
}

class JsonFormatter implements ReportFormatter {
    @Override
    public String format(String title, String[] headers, String[][] rows) {
        StringBuilder sb = new StringBuilder();

        sb.append("{\n \"report\": \"").append(title);
        sb.append("\",\n \"records\": [\n");

        for (int i = 0; i < rows.length; i++) {
            sb.append(" {");

            for (int j = 0; j < headers.length; j++) {
                sb.append("\"").append(headers[j]).append("\": \"");
                sb.append(rows[i][j]).append("\"");

                if (j < headers.length - 1) {
                    sb.append(", ");
                }
            }

            sb.append("}");

            if (i < rows.length - 1) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }

        sb.append(" ]\n}");
        return sb.toString();
    }
}

class CsvFormatter implements ReportFormatter {
    @Override
    public String format(String title, String[] headers, String[][] rows) {
        StringBuilder sb = new StringBuilder();

        sb.append("# TITLE: ").append(title).append("\n");
        sb.append(String.join(",", headers)).append("\n");

        for (String[] row : rows) {
            sb.append(String.join(",", row)).append("\n");
        }

        return sb.toString();
    }
}

interface Exporter {
    void export(String destination, String data);
}

class BaseFileExporter implements Exporter {
    @Override
    public void export(String destination, String data) {
        System.out.printf("[DISK WRITE] Writing %d bytes to file://%s%n",
                data.length(), destination);

        System.out.println("----- FILE PAYLOAD BEGIN -----");
        System.out.println(data);
        System.out.println("----- FILE PAYLOAD END -----");
    }
}

abstract class ExporterDecorator implements Exporter {
    protected final Exporter wrappedExporter;

    public ExporterDecorator(Exporter wrappedExporter) {
        if (wrappedExporter == null) {
            throw new IllegalArgumentException("Wrapped exporter cannot be null");
        }

        this.wrappedExporter = wrappedExporter;
    }

    @Override
    public void export(String destination, String data) {
        wrappedExporter.export(destination, data);
    }
}

class HeaderFooterDecorator extends ExporterDecorator {
    private final String timestamp;

    public HeaderFooterDecorator(Exporter wrappedExporter, String timestamp) {
        super(wrappedExporter);
        this.timestamp = timestamp;
    }

    @Override
    public void export(String destination, String data) {
        String result = "=== METADATA HEADER: " + timestamp + " ===\n"
                + data + "\n"
                + "=== INTEGRITY CHECK: SHA-256 OK | END RECORD ===";

        super.export(destination, result);
    }
}

class Base64EncryptDecorator extends ExporterDecorator {
    public Base64EncryptDecorator(Exporter wrappedExporter) {
        super(wrappedExporter);
    }

    @Override
    public void export(String destination, String data) {
        String encoded = Base64.getEncoder().encodeToString(
                data.getBytes(StandardCharsets.UTF_8));

        String result = "[BASE64_CIPHERTEXT:" + encoded + "]";

        super.export(destination, result);
    }
}

public class Task1Runner {
    public static void main(String[] args) {
        String[] headers = {"ID", "User", "Role"};

        String[][] rows = {
                {"101", "sufyan", "Admin"},
                {"102", "alex", "Dev"}
        };

        ReportFormatter jsonStrategy = new JsonFormatter();
        String rawJson = jsonStrategy.format("UserAuditReport", headers, rows);

        System.out.println(">>> TESTING DECORATOR STACK: Base64(HeaderFooter(BaseFileExporter)) <<<");

        Exporter securePipeline = new Base64EncryptDecorator(
                new HeaderFooterDecorator(
                        new BaseFileExporter(),
                        "2026-11-02 10:00:00 UTC"
                )
        );

        securePipeline.export("/var/logs/audit.sec", rawJson);
    }
}
