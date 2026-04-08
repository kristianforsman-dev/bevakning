Detta paket är anpassat för äldre Eclipse / offline-miljö.

Innehåll:
- källkod
- kompatibel pom.xml
- offline-repo/
- tools/bootstrap-old-eclipse.ps1
- build-output/bevakning.war

På Windows:
1. Packa upp zipen
2. Kör tools/bootstrap-old-eclipse.ps1 i PowerShell
3. Starta om Eclipse
4. Importera mappen 'bevakning' som Existing Maven Project

Om Eclipse visar många JavaScript-fel i dashboard.js:
- det är ofta gammal JS-validator i Eclipse
- WAR-filen i build-output kan ändå användas för deploy
