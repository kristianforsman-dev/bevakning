<!DOCTYPE html>
<html lang="sv">
<head>
    <meta charset="UTF-8">
    <title>Bevakning</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body>
    <div class="page">
        <header class="topbar">
            <h1>Flödesbevakning</h1>
            <div class="subtitle">Mockläge lokalt, senare WebSphere + Db2</div>
        </header>

        <main>
            <section class="card">
                <h2>Översikt</h2>
                <div id="summary">Laddar...</div>
            </section>

            <section class="card">
                <h2>Ny bevakning</h2>
                <form id="ruleForm" class="form-grid">
                    <input type="hidden" id="ruleId" name="id">
                    <label>Sender<input id="sender" name="sender" required></label>
                    <label>Receiver<input id="receiver" name="receiver" required></label>
                    <label>MsgType<input id="msgType" name="msgType" required></label>
                    <label>Beskrivning<input id="description" name="description"></label>
                    <label>Min antal<input id="minExpected" name="minExpected" type="number" value="1" required></label>
                    <label>Max antal<input id="maxExpected" name="maxExpected" type="number" value="1" required></label>
                    <label>Deadline<input id="deadline" name="deadline" type="time" value="15:00" required></label>
                    <label>Varning minuter före deadline<input id="warningMinutesBeforeDeadline" name="warningMinutesBeforeDeadline" type="number" value="60" required></label>
                    <label>Aktiv
                        <select id="active" name="active">
                            <option value="true" selected>true</option>
                            <option value="false">false</option>
                        </select>
                    </label>
                    <div class="form-actions">
                        <button type="submit" id="saveBtn">Spara regel</button>
                        <button type="button" id="resetBtn">Rensa</button>
                    </div>
                </form>
            </section>

            <section class="card">
                <h2>Bevakningar</h2>
                <table id="rulesTable">
                    <thead>
                        <tr>
                            <th>Sender</th>
                            <th>Receiver</th>
                            <th>MsgType</th>
                            <th>Status</th>
                            <th>Antal idag</th>
                            <th>Deadline</th>
                            <th>Beskrivning</th>
                            <th>Meddelande</th>
                            <th>Åtgärder</th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </section>

            <section class="card">
                <h2>Historik</h2>
                <table id="historyTable">
                    <thead>
                        <tr>
                            <th>Tid</th>
                            <th>RuleId</th>
                            <th>Status</th>
                            <th>Händelse</th>
                            <th>Kommentar</th>
                            <th>Användare</th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </section>
        </main>
    </div>

    <script src="${pageContext.request.contextPath}/assets/js/dashboard.js"></script>
</body>
</html>
