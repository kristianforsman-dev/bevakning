package se.forsman.bevakning.repository.db2;

import se.forsman.bevakning.domain.BackendStatus;
import se.forsman.bevakning.domain.DailyFlowCount;
import se.forsman.bevakning.domain.FlowEvent;
import se.forsman.bevakning.repository.FlowRepository;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Db2FlowRepository implements FlowRepository {
    public static final String MODE_PROCESS_ATTRIBUTE = "PROCESS_ATTRIBUTE";
    public static final String MODE_INSTANCE_ATTRIBUTE_T = "INSTANCE_ATTRIBUTE_T";

    private final String jndiName;
    private final int lookbackDays;
    private final String schema;
    private final String queryMode;

    private final String processTable;
    private final String attributeTable;

    private final String processBTable;
    private final String processInstanceAttributeTTable;

    private final String senderKey;
    private final String receiverKey;
    private final String msgTypeKey;

    public Db2FlowRepository(String jndiName,
                             int lookbackDays,
                             String schema,
                             String queryMode,
                             String processTable,
                             String attributeTable,
                             String processBTable,
                             String processInstanceAttributeTTable,
                             String senderKey,
                             String receiverKey,
                             String msgTypeKey) {
        this.jndiName = jndiName;
        this.lookbackDays = lookbackDays;
        this.schema = safeTrim(schema);
        this.queryMode = safeTrim(queryMode).isEmpty() ? MODE_PROCESS_ATTRIBUTE : safeTrim(queryMode);

        this.processTable = empty(processTable) ? "PROCESS_INSTANCE" : safeTrim(processTable);
        this.attributeTable = empty(attributeTable) ? "PROCESS_ATTRIBUTE" : safeTrim(attributeTable);

        this.processBTable = empty(processBTable) ? "PROCESS_INSTANCE_B_" : safeTrim(processBTable);
        this.processInstanceAttributeTTable = empty(processInstanceAttributeTTable) ? "PROCESS_INSTANCE_ATTRIBUTE_T" : safeTrim(processInstanceAttributeTTable);

        this.senderKey = empty(senderKey) ? "sender" : safeTrim(senderKey);
        this.receiverKey = empty(receiverKey) ? "receiver" : safeTrim(receiverKey);
        this.msgTypeKey = empty(msgTypeKey) ? "msgType" : safeTrim(msgTypeKey);
    }

    @Override
    public List<FlowEvent> findAllFlowEvents() {
        return Collections.emptyList();
    }

    @Override
    public List<DailyFlowCount> findTodayFlowCounts() {
        return queryDailyCounts(LocalDate.now(), LocalDate.now());
    }

    @Override
    public List<DailyFlowCount> findHistoricalFlowCounts(int lookbackDays) {
        LocalDate fromDate = LocalDate.now().minusDays(lookbackDays);
        LocalDate toDate = LocalDate.now().minusDays(1);
        return queryDailyCounts(fromDate, toDate);
    }

    @Override
    public BackendStatus getBackendStatus() {
        Connection connection = null;
        try {
            DataSource dataSource = (DataSource) new InitialContext().lookup(jndiName);
            connection = dataSource.getConnection();
            return new BackendStatus(true, "Db2 OK via " + jndiName + " (" + queryMode + ")");
        } catch (Exception e) {
            return new BackendStatus(false, "Db2/JNDI fel: " + e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    public String describeSource() {
        return "Db2FlowRepository{jndiName=" + jndiName + ", queryMode=" + queryMode + ", schema=" + schema + ", splitQueries=true}";
    }

    private List<DailyFlowCount> queryDailyCounts(LocalDate fromDate, LocalDate toDate) {
        if (toDate.isBefore(fromDate)) {
            return new ArrayList<DailyFlowCount>();
        }

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            DataSource dataSource = (DataSource) new InitialContext().lookup(jndiName);
            connection = dataSource.getConnection();

            String sql = buildAggregatedSql();
            ps = connection.prepareStatement(sql);

            bindAggregatedParameters(ps, fromDate, toDate);

            rs = ps.executeQuery();

            List<DailyFlowCount> result = new ArrayList<DailyFlowCount>();
            while (rs.next()) {
                result.add(new DailyFlowCount(
                        toLocalDate(rs.getDate("FLOW_DATE")),
                        trimToNull(rs.getString("SENDER")),
                        trimToNull(rs.getString("RECEIVER")),
                        trimToNull(rs.getString("MSGTYPE")),
                        rs.getInt("FLOW_COUNT")
                ));
            }

            return result;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Kunde inte hämta aggregerad data via Db2/JNDI. jndi=" + jndiName + ", mode=" + queryMode, e
            );
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(connection);
        }
    }

    private String buildAggregatedSql() {
        if (MODE_INSTANCE_ATTRIBUTE_T.equalsIgnoreCase(queryMode)) {
            return buildAggregatedInstanceAttributeTSql();
        }
        return buildAggregatedProcessAttributeSql();
    }

    private void bindAggregatedParameters(PreparedStatement ps, LocalDate fromDate, LocalDate toDate) throws Exception {
        ps.setString(1, senderKey);
        ps.setString(2, receiverKey);
        ps.setString(3, msgTypeKey);
        ps.setDate(4, Date.valueOf(fromDate));
        ps.setDate(5, Date.valueOf(toDate));
    }

    private String buildAggregatedProcessAttributeSql() {
        String pi = qualifiedName(processTable);
        String pa = qualifiedName(attributeTable);

        return "SELECT " +
                " DATE(PI.STARTED) AS FLOW_DATE, " +
                " CAST(PA_S.VALUE AS VARCHAR(256)) AS SENDER, " +
                " CAST(PA_R.VALUE AS VARCHAR(256)) AS RECEIVER, " +
                " CAST(PA_M.VALUE AS VARCHAR(256)) AS MSGTYPE, " +
                " COUNT(*) AS FLOW_COUNT " +
                "FROM " + pi + " PI " +
                "LEFT JOIN " + pa + " PA_S ON PI.PIID = PA_S.PIID AND PA_S.NAME = ? " +
                "LEFT JOIN " + pa + " PA_R ON PI.PIID = PA_R.PIID AND PA_R.NAME = ? " +
                "LEFT JOIN " + pa + " PA_M ON PI.PIID = PA_M.PIID AND PA_M.NAME = ? " +
                "WHERE DATE(PI.STARTED) BETWEEN ? AND ? " +
                "GROUP BY DATE(PI.STARTED), CAST(PA_S.VALUE AS VARCHAR(256)), CAST(PA_R.VALUE AS VARCHAR(256)), CAST(PA_M.VALUE AS VARCHAR(256)) " +
                "WITH UR";
    }

    private String buildAggregatedInstanceAttributeTSql() {
        String pib = qualifiedName(processBTable);
        String pia = qualifiedName(processInstanceAttributeTTable);

        return "SELECT " +
                " DATE(PI.STARTED) AS FLOW_DATE, " +
                " CAST(PA_S.VALUE AS VARCHAR(256)) AS SENDER, " +
                " CAST(PA_R.VALUE AS VARCHAR(256)) AS RECEIVER, " +
                " CAST(PA_M.VALUE AS VARCHAR(256)) AS MSGTYPE, " +
                " COUNT(*) AS FLOW_COUNT " +
                "FROM " + pib + " PI " +
                "LEFT JOIN " + pia + " PA_S ON PI.PIID = PA_S.PIID AND PA_S.ATTR_KEY = ? " +
                "LEFT JOIN " + pia + " PA_R ON PI.PIID = PA_R.PIID AND PA_R.ATTR_KEY = ? " +
                "LEFT JOIN " + pia + " PA_M ON PI.PIID = PA_M.PIID AND PA_M.ATTR_KEY = ? " +
                "WHERE DATE(PI.STARTED) BETWEEN ? AND ? " +
                "GROUP BY DATE(PI.STARTED), CAST(PA_S.VALUE AS VARCHAR(256)), CAST(PA_R.VALUE AS VARCHAR(256)), CAST(PA_M.VALUE AS VARCHAR(256)) " +
                "WITH UR";
    }

    private String qualifiedName(String table) {
        if (schema == null || schema.isEmpty()) {
            return table;
        }
        return schema + "." + table;
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }
}
