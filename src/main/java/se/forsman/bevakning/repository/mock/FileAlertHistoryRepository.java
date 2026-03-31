package se.forsman.bevakning.repository.mock;

import se.forsman.bevakning.domain.AlertHistoryEntry;
import se.forsman.bevakning.repository.AlertHistoryRepository;
import se.forsman.bevakning.util.FileJsonStore;
import se.forsman.bevakning.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileAlertHistoryRepository implements AlertHistoryRepository {
    private final String filePath;

    public FileAlertHistoryRepository(String filePath) {
        this.filePath = filePath;
        FileJsonStore.copyResourceToFileIfMissing("data/alert-history.json", filePath);
    }

    @Override
    public synchronized List<AlertHistoryEntry> findAll() {
        List<Map<String, Object>> rows = castList(JsonUtils.parseJson(FileJsonStore.readFile(filePath)));
        List<AlertHistoryEntry> result = new ArrayList<AlertHistoryEntry>();
        for (Map<String, Object> row : rows) {
            result.add(new AlertHistoryEntry(
                    stringValue(row.get("id")),
                    stringValue(row.get("ruleId")),
                    stringValue(row.get("occurrenceKey")),
                    stringValue(row.get("eventType")),
                    stringValue(row.get("status")),
                    stringValue(row.get("message")),
                    stringValue(row.get("createdAt")),
                    stringValue(row.get("createdBy"))
            ));
        }
        return result;
    }

    @Override
    public synchronized void save(AlertHistoryEntry entry) {
        List<AlertHistoryEntry> all = findAll();
        all.add(entry);
        writeAll(all);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value == null) {
            return new ArrayList<Map<String, Object>>();
        }

        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }

        if (value instanceof String) {
            Object reparsed = JsonUtils.parseJson((String) value);
            if (reparsed instanceof List) {
                return (List<Map<String, Object>>) reparsed;
            }
        }

        throw new IllegalStateException("Förväntade JSON-lista men fick: " + value.getClass().getName());
    }

    private void writeAll(List<AlertHistoryEntry> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < rows.size(); i++) {
            AlertHistoryEntry row = rows.get(i);
            if (i > 0) sb.append(",\n");
            sb.append("  {");
            sb.append("\"id\": \"").append(escape(row.getId())).append("\", ");
            sb.append("\"ruleId\": \"").append(escape(row.getRuleId())).append("\", ");
            sb.append("\"occurrenceKey\": \"").append(escape(row.getOccurrenceKey())).append("\", ");
            sb.append("\"eventType\": \"").append(escape(row.getEventType())).append("\", ");
            sb.append("\"status\": \"").append(escape(row.getStatus())).append("\", ");
            sb.append("\"message\": \"").append(escape(row.getMessage())).append("\", ");
            sb.append("\"createdAt\": \"").append(escape(row.getCreatedAt())).append("\", ");
            sb.append("\"createdBy\": \"").append(escape(row.getCreatedBy())).append("\"");
            sb.append("}");
        }
        sb.append("\n]\n");
        FileJsonStore.writeFile(filePath, sb.toString());
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
