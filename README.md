# Bevakning

Applikation för att bevaka integrationsflöden via mockdata lokalt och Db2 i målmiljö.

## Nuvarande lägen

### 1. Dev-läge i Codespaces
Används för snabb lokal utveckling.

Start:

```bash
mvn clean package
mvn exec:java

Öppna port 8080.

2. WebSphere-läge

Projektet är strukturerat för att senare kunna paketeras som WAR och köras i IBM WebSphere 8.5.5.

Viktiga mappar
Backend
src/main/java/se/forsman/bevakning/app
bootstrap/factory för tjänster
src/main/java/se/forsman/bevakning/domain
domänmodeller
src/main/java/se/forsman/bevakning/repository
repository-interface och implementationer
src/main/java/se/forsman/bevakning/service
affärslogik
src/main/java/se/forsman/bevakning/web
servlets för WAR/WebSphere
src/main/java/se/forsman/bevakning/dev
lokal dev-server för Codespaces
src/main/java/se/forsman/bevakning/admin
admin/import/export/config
Frontend för dev-läge
src/main/devui
Data i dev-läge
src/main/resources/data
src/main/resources/mockdb
Dokumentation
docs/
Skript
scripts/
Viktiga endpoints i dev-läge
/api/health
/api/dashboard
/api/rules
/api/history
/api/admin/config
/api/admin/rules/export
/api/admin/rules/import
Backup och restore

Backup:

./scripts/backup.sh

Restore:

./scripts/restore.sh backups/<katalog>
Status just nu

Projektet fungerar i mock-läge och har:

dashboard
regler
kvittering/historik
admin/config
export/import
backup/restore
förberedelse för Db2/WebSphere

Nästa steg i stabilisering är att städa kodstrukturen utan att ändra funktion.
