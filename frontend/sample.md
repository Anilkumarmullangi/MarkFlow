# Markdown to HTML Converter Pro

Welcome! This example shows **every syntax** the converter supports. Edit anything on the left and the preview updates live.

## Text formatting

This is **bold**, this is *italic*, this is ***both***, and this is ~~strikethrough~~.
Inline code looks like `System.out.println("Hi")`.

A single line break in the editor
becomes a line break in the output.

### Heading level 3
#### Heading level 4
##### Heading level 5
###### Heading level 6

## Lists

Unordered list with nesting:

- Backend
  - Java 21
  - Spring Boot 3
- Frontend
  - HTML, CSS, JavaScript

Ordered list:

1. Write Markdown
2. Watch the live preview
3. Copy or download the HTML

## Task list

- [x] Parse Markdown with CommonMark
- [x] Sanitize the generated HTML
- [ ] Deploy it somewhere

## Blockquote

> Good tools get out of your way.
>
> — A very wise developer

## Code block

```java
public static void main(String[] args) {
    System.out.println("Hello, Markdown!");
}
```

## Table

| Feature        | Status | Notes                    |
|:---------------|:------:|-------------------------:|
| Headings       |   ✅   | H1 to H6                 |
| Tables         |   ✅   | With column alignment    |
| Task lists     |   ✅   | Read-only checkboxes     |

## Links and images

Visit the [CommonMark specification](https://spec.commonmark.org/ "CommonMark spec") for the full syntax.

![Placeholder image](https://placehold.co/320x120/4f5bd5/ffffff?text=Markdown+Pro "Placeholder")

## Horizontal rule

---

## Safety check

Raw HTML is never rendered. The line below stays harmless text:

<script>alert("This will not run")</script>

This link is removed because its protocol is unsafe: [click me](javascript:alert(1))
