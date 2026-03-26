package se.forsman.bevakning.repository.mock;

import se.forsman.bevakning.domain.FlowEvent;
import se.forsman.bevakning.repository.FlowRepository;
import se.forsman.bevakning.util.JsonUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockFlowRepository implements FlowRepository {
    @Override
    public List<FlowEvent> findAllFlowEvents() {
        List<Map<String, Object>> instances = castList(JsonUtils.parseJsonResource("mockdb/process_instance.json"));
        List<Map<String, Object>> attributes = castList(JsonUtils.parseJsonResource("mockdb/process_attribute.json"));

        Map<Long, Map<String, String>> attrsByPiid = new HashMap<Long, Map<String, String>>();
        for (Map<String, Object> row : attributes) {
            Long piid = toLong(row.get("piid"));
            String name = stringValue(row.get("name"));
            String value = stringValue(row.get("value"));
            Map<String, String> map = attrsByPiid.get(piid);
            if (map == null) {
                map = new HashMap<String, String>();
                attrsByPiid.put(piid, map);
            }
            map.put(name, value);
        }

        List<FlowEvent> result = new ArrayList<FlowEvent>();
        for (Map<String, Object> row : instances) {
            long piid = toLong(row.get("piid"));
            long ptid = toLong(row.get("ptid"));
            int state = toInt(row.get("state"));
            LocalDateTime started = parseDateTime(row.get("started"));
            LocalDateTime completed = parseDateTime(row.get("completed"));

            Map<String, String> attrs = attrsByPiid.get(piid);
            String sender = attrs != null ? attrs.get("sender") : null;
            String receiver = attrs != null ? attrs.get("receiver") : null;
            String msgType = attrs != null ? attrs.get("msgType") : null;

            result.add(new FlowEvent(
                    piid,
                    ptid,
                    state,
                    started,
                    completed,
                    sender,
                    receiver,
                    msgType
            ));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.valueOf(String.valueOf(value));
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        return LocalDateTime.parse(s);
    }
}
