# Bevakning

Applikation för att bevaka integrationsflöden via mockdata lokalt och Db2 i målmiljö.

## Lägen

1. Dev-läge i Codespaces
- används för snabb lokal utveckling
- starta med:
  mvn clean package
  mvn exec:java
- öppna port 8080

2. WebSphere-läge
- projektet är strukturerat för att senare kunna paketeras som WAR
- tänkt målmiljö är IBM WebSphere 8.5.5

## Viktiga mappar

Backend
- src/main/java/se/forsman/bevakning/app
- src/main/java/se/forsman/bevakning/domain
- src/main/java/se/forsman/bevakning/repository
- src/main/java/se/forsman/bevakning/service
- src/main/java/se/forsman/bevakning/web
- src/main/java/se/forsman/bevakning/dev
- src/main/java/se/forsman/bevakning/admin

Frontend för dev-läge
- src/main/devui

Data i dev-läge
- src/main/resources/data
- src/main/resources/mockdb

Dokumentation
- docs

Skript
- scripts

## Viktiga endpoints i dev-läge

- /api/health
- /api/dashboard
- /api/rules
- /api/history
- /api/admin/config
- /api/admin/rules/export
- /api/admin/rules/import

## Backup och restore

Backup
- ./scripts/backup.sh

Restore
- ./scripts/restore.sh backups/<katalog>

## Status

Projektet fungerar i mock-läge och har:
- dashboard
- regler
- kvittering och historik
- admin/config
- export/import
- backup/restore
- förberedelse för Db2 och WebSphere
