# Build and Run (Windows PowerShell)

Copy and paste these commands into **Windows PowerShell**.

## 1. Install prerequisites (once)

Check what you already have:

```powershell
java -version
mvn -v
git --version
```

You need **JDK 21**. This project includes `backend\mvnw.cmd`, which downloads a project-local Maven copy automatically on first run, so global Maven installation is not required.

```powershell
winget search temurin
winget install -e --id EclipseAdoptium.Temurin.21.JDK
winget search maven
```

`winget search` shows the exact package ids; if the Temurin id above is not listed, use the id it shows. Maven can also be installed with `choco install maven -y` (administrator PowerShell, Chocolatey required) or by downloading the binary zip from <https://maven.apache.org/download.cgi> and adding its `bin` folder to `PATH`.

## 2. Extract the project

```powershell
cd $HOME\Downloads
Expand-Archive -Path .\markdown-to-html-converter-pro.zip -DestinationPath $HOME\projects -Force
cd $HOME\projects\markdown-to-html-converter-pro
```

## 3. Run the automated tests

```powershell
.\backend\mvnw.cmd -f backend\pom.xml clean test
```

The first run downloads dependencies (needs internet). Expect `BUILD SUCCESS` and 72 tests (54 unit tests + 18 Spring MockMvc tests). Tell me or note down any failure output; see TEST_REPORT.md for what was already verified.

## 4. Start the application

```powershell
.\backend\mvnw.cmd -f backend\pom.xml spring-boot:run
```

Open <http://localhost:8080> in your browser. Press `Ctrl+C` in PowerShell to stop.

Shortcuts: `.\run.ps1` and `.\test.ps1` (if blocked: `powershell -ExecutionPolicy Bypass -File .\run.ps1`).

## 5. Optional: build a JAR and run it

```powershell
.\backend\mvnw.cmd -f backend\pom.xml clean package
java -jar backend\target\markdown-to-html-converter-pro.jar
```

## 6. Optional: different port or Java 17

```powershell
$env:PORT = "9090"
.\backend\mvnw.cmd -f backend\pom.xml spring-boot:run

.\backend\mvnw.cmd -f backend\pom.xml spring-boot:run "-Djava.version=17"
```

## 7. Quick API check (second PowerShell window, app running)

```powershell
Invoke-RestMethod http://localhost:8080/api/health

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/convert `
  -ContentType "application/json" `
  -Body '{"markdown":"# Hello World"}'
```

## 8. Verify the features in the browser

1. The header shows **Server online**.
2. Click **Load example**: all syntax appears in the preview (headings, lists, task list, table, code, image, quote, rule).
3. Edit the Markdown: the preview updates after a short pause.
4. Click **HTML source** (or **View HTML source**) to see the generated HTML.
5. Click **Copy HTML**, then paste into Notepad.
6. Click **Download .html** and open the saved file.
7. Click the moon/sun button to switch themes; reload to confirm it is remembered.
8. Type `<script>alert(1)</script>` and `[x](javascript:alert(1))`: nothing executes; the script shows as text and the link is removed.
9. Click **Clear**: a confirmation dialog appears.
10. Stop the server and type something: a readable error with **Retry** appears. Start the server again and press **Retry**.

## 9. Push to your own GitHub repository

Create an **empty** repository on github.com first (no README, license or `.gitignore`), then:

```powershell
git init
git add .
git commit -m "Initial commit: Markdown to HTML Converter Pro"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/markdown-to-html-converter-pro.git
git push -u origin main
```

Then replace `YOUR-USERNAME` in `frontend/index.html` and `README.md`, and commit and push again:

```powershell
git add .
git commit -m "Set GitHub repository links"
git push
```
