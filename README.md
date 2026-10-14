# Quick start for Windows users

See [`START_HERE_WINDOWS.txt`](START_HERE_WINDOWS.txt). The project includes a Maven bootstrap script and Docker deployment files. The first run requires internet access to download Maven and dependencies.

# Markdown to HTML Converter Pro

A developer tool that turns Markdown into clean, **sanitized** HTML with a live split-screen preview. A Spring Boot REST API does the conversion with CommonMark and the OWASP HTML Sanitizer, and a dependency-free HTML/CSS/JavaScript frontend is served by the same Spring Boot server, so the whole app runs from one command.

> **Verification status:** read [TEST_REPORT.md](TEST_REPORT.md). It lists exactly which tests were executed before packaging and which were not. Run `.\test.ps1` on your own machine and update that file with your own results.

## Screenshots

Add real screenshots to the [`screenshots/`](screenshots/) folder (see [`screenshots/README.md`](screenshots/README.md)), then uncomment these lines:

<!--
![Dark theme](screenshots/editor-dark.png)
![Light theme](screenshots/editor-light.png)
![HTML source tab](screenshots/html-source.png)
-->

## Key features

- Live preview with a 300 ms debounce; stale in-flight requests are cancelled
- Headings H1-H6, paragraphs, line breaks, bold, italic, strikethrough, inline code
- Ordered, unordered and nested lists, task lists, blockquotes, fenced code blocks, tables with alignment, horizontal rules, links and images
- Preview and **HTML source** tabs, one-click **Copy HTML**, **Download .html** (a complete document with a basic stylesheet), **Load example**, **Clear** with a confirmation dialog
- Character and word counts, loading state, empty state, readable error banner with Retry, live server status indicator
- Dark navy theme (default) and light theme, remembered between visits
- Responsive layout (desktop, tablet, phone), keyboard-accessible controls, visible focus rings, skip link, ARIA tabs and live regions
- XSS protection: raw HTML in Markdown is escaped, and the output is filtered through a strict allowlist

## Technology stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5.0 (Spring Web, Bean Validation) |
| Build | Maven |
| Markdown parsing | CommonMark Java 0.24.0 + extensions: tables, strikethrough, task lists |
| HTML sanitizing | OWASP Java HTML Sanitizer 20240325.1 |
| Tests | JUnit 5, Spring Boot Test (MockMvc) |
| Frontend | HTML5, CSS3, vanilla JavaScript, Fetch API |

No database, API keys, paid services or secrets are used.

## Architecture

```
Browser (frontend/)                      Spring Boot server (backend/)
┌──────────────────────┐   POST /api/convert   ┌───────────────────────────────────────────┐
│ index.html           │ ────────────────────► │ RequestSizeLimitFilter  (413 if too big)  │
│ styles.css           │   {"markdown": "..."} │          │                                │
│ app.js (debounce,    │                       │ ConvertController  (@Valid request body)  │
│  fetch, preview,     │                       │          │                                │
│  copy, download)     │ ◄──────────────────── │ MarkdownConversionService                 │
└──────────────────────┘   {"html": "..."}     │   1. CommonMark parse (+ extensions)      │
        ▲                                      │   2. Render HTML, raw HTML escaped        │
        │  static files from classpath:/static │   3. HtmlSanitizer (OWASP allowlist)      │
        └───────────────────────────────────── │ GlobalExceptionHandler (JSON errors)      │
                                               └───────────────────────────────────────────┘
```

At build time Maven copies `frontend/` into the application's `static/` resources (see `<resources>` in `backend/pom.xml`), so there is nothing to start except the Spring Boot app.

The conversion code (`MarkdownConversionService`, `HtmlSanitizer`) is plain Java with no Spring annotations. `ConverterConfig` wires them as beans. This keeps them easy to unit test and easy to explain.

## Project structure

```
markdown-to-html-converter-pro/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/markdownconverterpro/
│       │   ├── MarkdownConverterApplication.java
│       │   ├── config/        ConverterConfig, RequestSizeLimitFilter
│       │   ├── controller/    ConvertController   (POST /api/convert, GET /api/health)
│       │   ├── dto/           ConvertRequest, ConvertResponse, ErrorResponse, HealthResponse, ConverterLimits
│       │   ├── exception/     GlobalExceptionHandler
│       │   ├── sanitizer/     HtmlSanitizer       (OWASP allowlist policy)
│       │   └── service/       MarkdownConversionService
│       ├── main/resources/application.properties
│       └── test/java/com/markdownconverterpro/
│           ├── controller/    ConvertControllerTest, RequestSizeLimitTest
│           ├── sanitizer/     HtmlSanitizerTest
│           └── service/       MarkdownConversionServiceTest
├── frontend/
│   ├── index.html
│   ├── styles.css
│   ├── app.js
│   ├── sample.md              (the "Load example" document)
│   └── assets/logo.svg
├── screenshots/
├── run.ps1                    (start the app)
├── test.ps1                   (run the tests)
├── BUILD_AND_RUN.md
├── TEST_REPORT.md
├── .gitignore
├── LICENSE
└── README.md
```

## Prerequisites

- **JDK 21** (Java 17 should also work, see below; it was not tested)
- **Apache Maven 3.9+** on your `PATH`
- Internet access the first time you build, so Maven can download dependencies
- Git, only if you want to push to GitHub

Node.js and npm are **not** required. The frontend has no build step.

A Maven wrapper (`mvnw`) is not included because it could not be generated in the environment this project was packaged in. You can add one yourself with `mvn wrapper:wrapper` after installing Maven.

## Run the application (Windows PowerShell)

```powershell
cd markdown-to-html-converter-pro
mvn -f backend\pom.xml spring-boot:run
```

Then open <http://localhost:8080>. Stop with `Ctrl+C`.

Shortcut: `.\run.ps1` (if scripts are blocked, use `powershell -ExecutionPolicy Bypass -File .\run.ps1`).

Using Java 17 instead of 21:

```powershell
mvn -f backend\pom.xml spring-boot:run "-Djava.version=17"
```

Build and run a standalone JAR:

```powershell
mvn -f backend\pom.xml clean package
java -jar backend\target\markdown-to-html-converter-pro.jar
```

Use a different port:

```powershell
$env:PORT = "9090"
mvn -f backend\pom.xml spring-boot:run
```

## Run the automated tests

```powershell
mvn -f backend\pom.xml clean test
```

or `.\test.ps1`. The suite contains:

| Test class | What it covers |
|---|---|
| `MarkdownConversionServiceTest` | Headings H1-H6, paragraphs and line breaks, bold, italic, strikethrough, inline code, lists, blockquotes, fenced code, horizontal rules, links, images, tables, task lists, empty/blank/null input, script tags, event-handler attributes, dangerous link and image protocols, attribute injection, Unicode |
| `HtmlSanitizerTest` | Allowlist behaviour on raw HTML: scripts, obfuscated scripts, disallowed elements, style attributes, URL protocols, forced `rel` on links, class and alignment restrictions, checkbox-only inputs |
| `ConvertControllerTest` | Full HTTP behaviour with MockMvc: success responses, empty input, missing/null field, over-long input, malformed JSON, empty body, wrong content type (415), wrong method (405), unknown path (404), health endpoint, sanitization over the API, static frontend |
| `RequestSizeLimitTest` | 413 response when the request body exceeds `converter.max-request-bytes` |

## API

### `POST /api/convert`

Request:

```json
{ "markdown": "# Hello World" }
```

Response `200 OK`:

```json
{ "html": "<h1>Hello World</h1>" }
```

PowerShell example (the server must be running):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/convert `
  -ContentType "application/json" `
  -Body '{"markdown":"# Hello World"}'
```

Example validation error (`400 Bad Request`) for `{}`:

```json
{
  "timestamp": "2026-10-14T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/convert",
  "fieldErrors": { "markdown": "markdown is required" }
}
```

| Situation | Status |
|---|---|
| Converted successfully (an empty `markdown` string returns `{"html":""}`) | 200 |
| `markdown` missing/null, or longer than 200,000 characters | 400 |
| Body missing or not valid JSON | 400 |
| Method other than POST on `/api/convert` | 405 |
| `Content-Type` is not `application/json` | 415 |
| Body larger than 1 MiB (`Content-Length` check) | 413 |
| Unknown path | 404 |
| Unexpected server error (details are logged, not returned) | 500 |

### `GET /api/health`

```json
{ "status": "UP", "application": "markdown-to-html-converter-pro", "timestamp": "2026-10-14T10:15:30.123Z" }
```

### Configuration

`backend/src/main/resources/application.properties`:

| Property | Default | Meaning |
|---|---|---|
| `server.port` | `${PORT:8080}` | HTTP port, override with the `PORT` environment variable |
| `converter.max-request-bytes` | `1048576` | Largest accepted request body |

The 200,000-character Markdown limit is the constant `ConverterLimits.MAX_MARKDOWN_CHARACTERS`; the frontend enforces the same limit.

## Security and HTML sanitization

Defence in depth, in this order:

1. **Raw HTML is escaped, not rendered.** CommonMark runs with `escapeHtml(true)`. `<script>alert(1)</script>` in the Markdown appears as visible text.
2. **OWASP allowlist.** Everything CommonMark generates is passed through `HtmlSanitizer`, which only permits the tags and attributes below. Anything else is removed.
3. **The frontend only displays server-sanitized HTML.** It never inserts the editor text directly into the page.
4. **Size limits** on the request body and the Markdown length.
5. **No secrets** are stored or required. Error responses never include stack traces.

**Allowed elements:** `h1`-`h6`, `p`, `br`, `hr`, `strong`, `em`, `del`, `s`, `code`, `pre`, `blockquote`, `ul`, `ol`, `li`, `a`, `img`, `table`, `thead`, `tbody`, `tr`, `th`, `td`, `input`.

**Allowed attributes:**

| Element | Attributes |
|---|---|
| `a` | `href`, `title` (plus `rel` added automatically) |
| `img` | `src`, `alt`, `title` |
| `code` | `class`, only values like `language-java` |
| `ol` | `start`, digits only |
| `th`, `td` | `align`, only `left`, `right` or `center` |
| `input` | `type="checkbox"` only, plus `checked` and `disabled` (used by task lists) |

No `style` attribute, no `id`, and no event-handler attribute (`onclick`, `onerror`, ...) is ever allowed.

### How links and images are handled

- **Allowed URL protocols:** `http`, `https` and `mailto`. Relative URLs (`/docs`) and fragments (`#top`) are kept.
- **Blocked protocols:** `javascript:`, `vbscript:`, `data:`, `file:` and any other scheme, including obfuscated forms such as `JaVaScRiPt:` or entity-encoded characters. A link with a blocked URL is removed and only its text remains. An image with a blocked URL loses its `src`.
- **Every link** gets `rel="noopener noreferrer nofollow"`, and any `rel` supplied by the author is replaced. The preview additionally opens links in a new tab.
- **Images** are not proxied or downloaded by the server. The browser loads them from the remote host, which means that host can see the viewer's IP address. Keep this in mind before using the tool on untrusted Markdown.

## Third-party libraries and licenses

| Library | Used for | License |
|---|---|---|
| Spring Boot / Spring Framework | Web server, REST, validation, testing | Apache License 2.0 |
| Apache Tomcat (embedded) | HTTP server | Apache License 2.0 |
| Jackson | JSON | Apache License 2.0 |
| Hibernate Validator | Bean Validation | Apache License 2.0 |
| CommonMark Java (`org.commonmark`) | Markdown parsing and HTML rendering | BSD 2-Clause |
| OWASP Java HTML Sanitizer | HTML allowlist sanitizing | Apache License 2.0 |
| JUnit 5 | Tests | Eclipse Public License 2.0 |
| Mockito, AssertJ, Hamcrest, JSONassert (via `spring-boot-starter-test`) | Tests | MIT, Apache 2.0, BSD 3-Clause, Apache 2.0 |

The frontend uses no third-party code, fonts or CDNs; it relies on system font stacks. Check each project's repository for the authoritative license text before redistributing.

## Troubleshooting

| Problem | Fix |
|---|---|
| `'mvn' is not recognized` | Install Maven, add its `bin` folder to `PATH`, then open a **new** PowerShell window. |
| `'java' is not recognized` or `JAVA_HOME` errors | Install JDK 21 and open a new PowerShell window. Check with `java -version`. |
| `release version 21 not supported` / `invalid target release` | Your JDK is older than 21. Install JDK 21, or add `"-Djava.version=17"` to the Maven command (quotes are required in PowerShell). |
| `Port 8080 was already in use` | Run on another port (`$env:PORT = "9090"`), or free it: `netstat -ano \| findstr :8080`, then `taskkill /PID <pid> /F`. |
| The page shows "Server unreachable" | The Spring Boot app is not running, or you opened the file directly. Start the app and browse to `http://localhost:8080`, not `file:///...`. |
| Maven cannot download dependencies | Check your internet, VPN or proxy. Corporate or campus proxies need a Maven `settings.xml` proxy entry. |
| `running scripts is disabled on this system` | Run `powershell -ExecutionPolicy Bypass -File .\run.ps1`, or just use the `mvn` commands above. |
| Copy button says copy failed | Clipboard access needs `http://localhost` or HTTPS. Use the HTML source tab and copy manually as a fallback. |

## Upload to GitHub

1. On github.com choose **New repository**. Name it `markdown-to-html-converter-pro`, leave it **empty** (no README, `.gitignore` or license, because the project already has them), and create it.
2. In PowerShell, from the project folder:

```powershell
git init
git add .
git commit -m "Initial commit: Markdown to HTML Converter Pro"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/markdown-to-html-converter-pro.git
git push -u origin main
```

3. Replace `YOUR-USERNAME` in the GitHub link in `frontend/index.html` and in this README, then commit and push again.

If Git asks who you are: `git config --global user.name "Your Name"` and `git config --global user.email "you@example.com"`.

## Design decisions (useful for interviews)

- **CommonMark instead of regular expressions.** Markdown has nested, context-dependent rules; a real parser handles edge cases correctly.
- **Escape first, then sanitize.** Escaping stops raw HTML at the source; the sanitizer is a second, independent barrier that also covers anything an extension might generate.
- **Allowlist, not blocklist.** Unknown tags and attributes are dropped by default, so new attack tricks do not need new rules.
- **Plain-Java service plus a configuration class.** Core logic has no framework dependency, which makes it fast to unit test.
- **One server.** Maven copies the frontend into `static/`, so there is no CORS setup and no second dev server.
- **Consistent error contract.** `GlobalExceptionHandler` extends Spring's `ResponseEntityExceptionHandler`, so every error, including 404 and 415, returns the same JSON shape.
- **Frontend robustness.** Debounce avoids request spam, an incrementing request id plus `AbortController` prevents an old response from overwriting a newer one.

## Known limitations

- Single newlines render as `<br />` (like GitHub comments) rather than the CommonMark default of a plain space. This is configured with `softbreak("<br />\n")` in `MarkdownConversionService`.
- The sanitizer writes some characters as HTML entities (for example `=` as `&#61;`, `@` as `&#64;`, and emoji as `&#x1f680;`). Browsers display them correctly, but the HTML source tab shows the entities.
- Raw HTML in Markdown is intentionally not supported; it is shown as text.
- Not included: footnotes, heading anchors, syntax highlighting, automatic linking of bare URLs, math, diagrams.
- No persistence: the editor content is lost when the page is closed.
- Requests sent without a `Content-Length` header (chunked) skip the byte-size filter; the 200,000-character validation still applies.
- Java 17 compatibility and a real browser run were not tested during packaging; see TEST_REPORT.md.
- The project has not been deployed anywhere.

## Future enhancements

Maven wrapper, Dockerfile and CI workflow (GitHub Actions), syntax highlighting, autosave in `localStorage`, scroll sync between panes, export to PDF, rate limiting, OpenAPI documentation, frontend automated tests, more CommonMark extensions.

## License

MIT, see [LICENSE](LICENSE).
