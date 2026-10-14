# Test Report

Report date: 2026-10-14. This file records what was **actually executed** before the project was packaged. Nothing here is estimated.

## Summary

| Area | Status | Result |
|---|---|---|
| `MarkdownConversionServiceTest` (33 test cases) | Executed | 33 passed, 0 failed |
| `HtmlSanitizerTest` (21 test cases) | Executed | 21 passed, 0 failed |
| Frontend end-to-end checks (jsdom, 45 checks, real converter behind a throwaway HTTP server) | Executed | 45 passed, 0 failed |
| Maven build (`mvn clean test` / `package`) | **Not executed** | Maven Central was unreachable from the build environment |
| Spring Boot application start-up | **Not executed** | Same reason |
| `ConvertControllerTest` (16 tests) | **Not executed** | Needs Spring Boot, MockMvc |
| `RequestSizeLimitTest` (2 tests) | **Not executed** | Needs Spring Boot, MockMvc |
| Spring classes (controller, exception handler, filter, DTOs, config, application class) | **Not compiled** | Spring and Jakarta jars were not available |
| Real-browser run, Java 17, Windows | **Not executed** | |

**Bottom line:** the conversion and sanitization core is verified. The Spring Boot layer (HTTP routing, validation, error JSON, the size filter, static serving) was written carefully against the Spring 6.2 / Boot 3.5 APIs but has not been compiled or run. Run `mvn -f backend\pom.xml clean test` on your machine; the expected result is 72 passing tests. If anything fails, that output is the thing to fix.

## Environment used

- OpenJDK 21.0.12.1 (Ubuntu), no Maven available
- CommonMark Java **0.24.0**, compiled with `javac` from the official `commonmark-parent-0.24.0` source tag (core, `gfm-tables`, `gfm-strikethrough`, `task-list-items`), not downloaded as a Maven artifact
- OWASP Java HTML Sanitizer **20191001.1** from the Ubuntu package. The `pom.xml` pins **20240325.1**; the builder API used here is the same, but the newer version could behave slightly differently
- JUnit Jupiter 5.10.1 via the JUnit Platform Console Launcher 1.9.1 (Spring Boot 3.5.0 manages a newer JUnit 5.12.x)
- Node.js 22 with jsdom for the frontend checks

## Unit tests that ran

`MarkdownConversionService` and `HtmlSanitizer` were compiled with `javac` and run with the JUnit console launcher: **54 tests found, 54 successful, 0 failed, 0 skipped.**

One failure was found and fixed during this run: my first Unicode test expected the emoji to be returned unchanged, but the sanitizer outputs it as `&#x1f680;`. This is valid HTML that browsers render as the emoji, so I corrected the test to accept either form and listed the behaviour under "Known limitations" in the README.

Covered: H1-H6, paragraphs, hard and soft line breaks, bold, italic, strikethrough, inline code, unordered / ordered / nested lists, blockquotes, fenced code (language class, HTML escaping), horizontal rules, links (including forced `rel`), relative and `mailto` links, images, tables with alignment, task lists, empty / blank / null input, raw `<script>`, `<img onerror>`, `<a onclick>`, `<iframe>`, `<svg onload>`, `<body onload>`, `javascript:` / `JaVaScRiPt:` / entity-encoded / `vbscript:` / `data:` / `file:` URLs in links and images, attribute injection via link titles, obfuscated script tags, style attributes, disallowed elements, class / alignment / input restrictions, Unicode.

## Frontend end-to-end checks that ran

The real `frontend/index.html`, `styles.css` and `app.js` were loaded in jsdom and driven against a small throwaway Java HTTP server (not part of this project) that serves the `frontend/` folder and exposes `/api/convert` and `/api/health` using the real `MarkdownConversionService`. **45 checks, 45 passed.** They covered: initial empty state and disabled buttons; live preview; character and word counts; debounce (5 rapid edits produced exactly 1 request); stale-response protection; XSS payloads not executing and not appearing as elements; links opening in a new tab with safe `rel`; HTML source tab and toggle button; Copy HTML to the clipboard; Download builds a full HTML document with a title and stylesheet and the `markdown-export.html` filename; Load example rendering every syntax; Clear with accept and decline; theme toggle and persistence; backend outage producing a readable error and the "unreachable" status; Retry recovering after the server returned; no uncaught JavaScript errors.

Limits of these checks: jsdom is not a real browser. It does not render CSS, so layout, responsiveness, contrast and the `<dialog>` element (jsdom falls back to `window.confirm`) were **not** visually verified. Please click through the app once in a real browser, using the checklist in BUILD_AND_RUN.md.

## What still needs your verification

1. `mvn -f backend\pom.xml clean test` completes with 72 tests and no failures (compiles the Spring classes for the first time).
2. `mvn -f backend\pom.xml spring-boot:run` starts and `http://localhost:8080` shows the app with **Server online**.
3. The 10-step feature checklist in BUILD_AND_RUN.md, in a real browser, in both themes and at a narrow window width.

If you run into a compile error or failing test, send me the output and I will fix it.
