package se.forsman.bevakning.repository.mock;

import se.forsman.bevakning.domain.Acknowledgement;
import se.forsman.bevakning.repository.AcknowledgementRepository;
import se.forsman.bevakning.util.FileJsonStore;
import se.forsman.bevakning.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileAcknowledgementRepository implements AcknowledgementRepository {
    private final String filePath;

    public FileAcknowledgementRepository(String filePath) {
        this.filePath = filePath;
        FileJsonStore.copyResourceToFileIfMissing("data/acknowledgements.json", filePath);
    }

    @Override
    public synchronized List<Acknowledgement> findAll() {
        List<Map<String, Object>> rows = castList(JsonUtils.parseJson(FileJsonStore.readFile(filePath)));
        List<Acknowledgement> result = new ArrayList<Acknowledgement>();
        for (Map<String, Object> row : rows) {
            result.add(new Acknowledgement(
                    stringValue(row.get("id")),
                    stringValue(row.get("ruleId")),
                    stringValue(row.get("status")),
                    stringValue(row.get("comment")),
                    stringValue(row.get("acknowledgedBy")),
                    stringValue(row.get("acknowledgedAt"))
            ));
        }
        return result;
    }

    @Override
    public synchronized void save(Acknowledgement acknowledgement) {
        List<Acknowledgement> all = findAll();
        all.add(acknowledgement);
        writeAll(all);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private void writeAll(List<Acknowledgement> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < rows.size(); i++) {
            Acknowledgement row = rows.get(i);
            if (i > 0) sb.append(",\n");
            sb.append("  {");
            sb.append("\"id\": \"").append(escape(row.getId())).append("\", ");
            sb.append("\"ruleId\": \"").append(escape(row.getRuleId())).append("\", ");
            sb.append("\"status\": \"").append(escape(row.getStatus())).append("\", ");
            sb.append("\"comment\": \"").append(escape(row.getComment())).append("\", ");
            sb.append("\"acknowledgedBy\": \"").append(escape(row.getAcknowledgedBy())).append("\", ");
            sb.append("\"acknowledgedAt\": \"").append(escape(row.getAcknowledgedAt())).append("\"");
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
