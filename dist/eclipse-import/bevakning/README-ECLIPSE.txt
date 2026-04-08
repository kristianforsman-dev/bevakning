Importera i Eclipse via:
File -> Import -> General -> Existing Projects into Workspace

Efter import:
1. Högerklicka projektet -> Properties
2. Project Facets: kontrollera Java 1.8 + Dynamic Web Module 3.1
3. Targeted Runtimes: välj din WebSphere-runtime
4. Om gamla Eclipse klagar på JavaScript i dashboard.js:
   - det är ofta bara JS-validatorn
   - serverkörning/deploy kan ändå fungera
