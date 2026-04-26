#!/usr/bin/env python3
"""Crawl public IT support documents into raw Markdown knowledge files.

The script intentionally does not summarize or rewrite the source content.
It extracts the main readable article body, preserves source metadata, and
writes one Markdown file per source URL under samples/knowledge/raw/<category>.
"""

from __future__ import annotations

import argparse
import html
import json
import re
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from typing import Iterable


DEFAULT_DOCS = [
    {
        "slug": "microsoft_always_on_vpn_troubleshooting",
        "category": "network",
        "title": "Troubleshoot Always On VPN",
        "url": "https://learn.microsoft.com/en-us/troubleshoot/windows-server/networking/troubleshoot-always-on-vpn",
    },
    {
        "slug": "microsoft_outlook_connection_issues",
        "category": "collaboration",
        "title": "Fix Outlook connection problems for Microsoft 365 account",
        "url": "https://learn.microsoft.com/en-us/exchange/troubleshoot/outlook-issues/outlook-connection-issues",
    },
    {
        "slug": "microsoft_teams_signin_errors",
        "category": "collaboration",
        "title": "Resolve sign-in errors in Teams",
        "url": "https://learn.microsoft.com/en-us/microsoftteams/troubleshoot/teams-sign-in/resolve-sign-in-errors",
    },
    {
        "slug": "microsoft_intune_policy_profile_troubleshooting",
        "category": "endpoint",
        "title": "Troubleshooting policies and profiles in Microsoft Intune",
        "url": "https://learn.microsoft.com/en-us/troubleshoot/mem/intune/device-configuration/troubleshoot-policies-in-microsoft-intune",
    },
    {
        "slug": "google_workspace_password_reset",
        "category": "identity",
        "title": "Reset a user's password",
        "url": "https://support.google.com/a/answer/33319",
    },
    {
        "slug": "okta_mfa_reset_users",
        "category": "identity",
        "title": "Reset multifactor authentication for users",
        "url": "https://help.okta.com/oie/en-us/content/topics/security/mfa/mfa-reset-users.htm",
    },
    {
        "slug": "github_enterprise_saml_authentication_troubleshooting",
        "category": "identity",
        "title": "Troubleshooting SAML authentication",
        "url": "https://docs.github.com/en/enterprise-cloud@latest/admin/managing-iam/using-saml-for-enterprise-iam/troubleshooting-saml-authentication",
    },
    {
        "slug": "jira_service_management_approvals",
        "category": "itsm",
        "title": "What are approvals?",
        "url": "https://support.atlassian.com/jira-service-management-cloud/docs/what-are-approvals/",
    },
    {
        "slug": "azure_vpn_point_to_site_connection_problems",
        "category": "network",
        "title": "Troubleshooting Azure point-to-site connection problems",
        "url": "https://learn.microsoft.com/en-us/troubleshoot/azure/vpn-gateway/vpn-gateway-troubleshoot-vpn-point-to-site-connection-problems",
    },
    {
        "slug": "azure_vpn_client_troubleshooting",
        "category": "network",
        "title": "Troubleshoot Azure VPN Client",
        "url": "https://learn.microsoft.com/en-us/azure/vpn-gateway/troubleshoot-azure-vpn-client",
    },
    {
        "slug": "google_workspace_deploy_2_step_verification",
        "category": "identity",
        "title": "Deploy 2-Step Verification",
        "url": "https://support.google.com/a/answer/9176657",
    },
    {
        "slug": "google_workspace_user_security_settings",
        "category": "identity",
        "title": "Manage a user's security settings",
        "url": "https://support.google.com/a/answer/2537800",
    },
    {
        "slug": "slack_saml_authorization_errors",
        "category": "identity",
        "title": "Troubleshoot SAML authorization errors",
        "url": "https://slack.com/help/articles/360037402653-Troubleshoot-SAML-authorization-errors?locale=en-US",
    },
    {
        "slug": "microsoft_onedrive_work_school_sync_issues",
        "category": "collaboration",
        "title": "Troubleshooting OneDrive for work or school sync issues",
        "url": "https://learn.microsoft.com/en-us/troubleshoot/sharepoint/sync/troubleshoot-sync-issues",
    },
    {
        "slug": "microsoft_teams_client_connectivity_issues",
        "category": "collaboration",
        "title": "Troubleshoot connectivity issues with the Microsoft Teams client",
        "url": "https://learn.microsoft.com/en-us/microsoftteams/connectivity-issues",
    },
    {
        "slug": "microsoft_defender_endpoint_onboarding_issues",
        "category": "endpoint",
        "title": "Troubleshoot Microsoft Defender for Endpoint onboarding issues",
        "url": "https://learn.microsoft.com/en-us/defender-endpoint/troubleshoot-onboarding",
    },
    {
        "slug": "jira_service_management_set_up_approvals",
        "category": "itsm",
        "title": "Set up approval steps",
        "url": "https://support.atlassian.com/jira-service-management-cloud/docs/set-up-approvals/",
    },
    {
        "slug": "jira_service_management_request_types",
        "category": "itsm",
        "title": "What are request types?",
        "url": "https://support.atlassian.com/jira-service-management-cloud/docs/what-are-request-types-in-a-service-project/",
    },
]

BLOCK_TAGS = {
    "address",
    "article",
    "aside",
    "blockquote",
    "br",
    "dd",
    "div",
    "dl",
    "dt",
    "figcaption",
    "figure",
    "footer",
    "form",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "header",
    "hr",
    "li",
    "main",
    "nav",
    "ol",
    "p",
    "pre",
    "section",
    "table",
    "tbody",
    "td",
    "tfoot",
    "th",
    "thead",
    "tr",
    "ul",
}

DROP_TAGS = {"script", "style", "noscript", "svg", "canvas", "iframe"}
MAIN_TAGS = ("article", "main")


@dataclass(frozen=True)
class SourceDoc:
    slug: str
    category: str
    title: str
    url: str


class TitleParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self._in_title = False
        self.parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag.lower() == "title":
            self._in_title = True

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() == "title":
            self._in_title = False

    def handle_data(self, data: str) -> None:
        if self._in_title:
            self.parts.append(data)

    @property
    def title(self) -> str:
        return normalize_spaces(" ".join(self.parts))


class ArticleTextParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.depth = 0
        self.drop_depth = 0
        self.parts: list[str] = []
        self._last_tag = ""
        self._main_stack: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        tag = tag.lower()
        attr_map = {name.lower(): value or "" for name, value in attrs}
        if tag in DROP_TAGS:
            self.drop_depth += 1
            return
        if tag in MAIN_TAGS or attr_map.get("role") == "main":
            self.depth += 1
            self._main_stack.append(tag)
            self._newline()
        elif self.depth and tag in BLOCK_TAGS:
            self._newline()
        if self.depth and tag == "li":
            self.parts.append("- ")
        if self.depth and tag in {"h1", "h2", "h3", "h4", "h5", "h6"}:
            level = min(int(tag[1]), 4)
            self.parts.append("#" * level + " ")
        self._last_tag = tag

    def handle_endtag(self, tag: str) -> None:
        tag = tag.lower()
        if self.drop_depth:
            if tag in DROP_TAGS:
                self.drop_depth -= 1
            return
        if self.depth and tag in BLOCK_TAGS:
            self._newline()
        if self._main_stack and tag == self._main_stack[-1]:
            self._main_stack.pop()
            self.depth -= 1
            self._newline()
        self._last_tag = tag

    def handle_data(self, data: str) -> None:
        if not self.depth or self.drop_depth:
            return
        text = normalize_spaces(data)
        if not text:
            return
        if self.parts and self.parts[-1] and not self.parts[-1].endswith((" ", "\n", "- ")):
            self.parts.append(" ")
        self.parts.append(text)

    def _newline(self) -> None:
        if not self.parts:
            return
        if self.parts[-1].endswith("- "):
            return
        if self.parts[-1].endswith("\n\n"):
            return
        if self.parts[-1].endswith("\n"):
            self.parts.append("\n")
        else:
            self.parts.append("\n\n")

    @property
    def text(self) -> str:
        return clean_markdown_text("".join(self.parts))


def normalize_spaces(value: str) -> str:
    return re.sub(r"[ \t\f\v]+", " ", html.unescape(value)).strip()


def clean_markdown_text(value: str) -> str:
    value = value.replace("\r\n", "\n").replace("\r", "\n")
    value = re.sub(r"[ \t]+\n", "\n", value)
    value = re.sub(r"\n{3,}", "\n\n", value)
    lines = [line.rstrip() for line in value.splitlines()]
    return "\n".join(remove_page_noise(lines)).strip()


def remove_page_noise(lines: list[str]) -> list[str]:
    noise_exact = {
        "Feedback",
        "Summarize this article for me",
        "Copy as Markdown",
        "Was this helpful?",
        "Yes",
        "No",
    }
    stop_phrases = (
        "Want to try using Ask Learn",
        "Ask Learn Ask Learn",
        "Additional resources",
    )
    cleaned: list[str] = []
    for line in lines:
        stripped = line.strip()
        if any(stripped.startswith(phrase) for phrase in stop_phrases):
            break
        if stripped in noise_exact:
            continue
        cleaned.append(line)
    return cleaned


def trim_to_first_heading(value: str) -> str:
    lines = value.splitlines()
    for index, line in enumerate(lines):
        if re.match(r"^#\s+\S", line):
            return "\n".join(lines[index:]).strip()
    return value


def trim_to_preferred_heading(value: str, title: str) -> str:
    preferred = re.sub(r"\s+", " ", title).strip().lower()
    if not preferred:
        return value
    lines = value.splitlines()
    for index, line in enumerate(lines):
        line_text = re.sub(r"^#+\s*", "", line).strip()
        normalized = re.sub(r"\s+", " ", line_text).lower()
        if normalized == preferred:
            return "\n".join(lines[index:]).strip()
    return value


def fetch(url: str, timeout: int, retries: int) -> bytes:
    headers = {
        "User-Agent": "enterprise-ai-ticketing-knowledge-crawler/1.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    }
    last_error: Exception | None = None
    for attempt in range(1, retries + 1):
        request = urllib.request.Request(url, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                return response.read()
        except (urllib.error.URLError, TimeoutError) as exc:
            last_error = exc
            if attempt < retries:
                time.sleep(min(attempt * 2, 8))
    raise RuntimeError(f"failed to fetch {url}: {last_error}")


def decode_html(payload: bytes) -> str:
    for encoding in ("utf-8", "utf-8-sig"):
        try:
            return payload.decode(encoding)
        except UnicodeDecodeError:
            continue
    return payload.decode("utf-8", errors="replace")


def extract_title(document_html: str, fallback: str) -> str:
    parser = TitleParser()
    parser.feed(document_html)
    return parser.title or fallback


def extract_article_text(document_html: str) -> str:
    parser = ArticleTextParser()
    parser.feed(document_html)
    text = parser.text
    if len(text) >= 300:
        return trim_to_first_heading(text)

    # Some support sites do not expose a clean article/main element. Fall back
    # to a conservative whole-page text extraction so the crawl still produces
    # reviewable raw material instead of silently skipping the source.
    h1_match = re.search(
        r"(?is)<h1[^>]*>(?:\s|<[^>]+>)*(?:Troubleshoot|What are|Set up|Deploy|Manage|Reset|Fix|Resolve)",
        document_html,
    ) or re.search(r"(?is)<h1\b", document_html)
    fallback_source = document_html[h1_match.start():] if h1_match else document_html
    fallback = re.sub(r"(?is)<(script|style|noscript|svg|canvas|iframe).*?</\1>", " ", fallback_source)
    fallback = re.sub(r"(?is)<br\s*/?>", "\n", fallback)
    fallback = re.sub(r"(?is)</(p|div|li|h[1-6]|tr|section|article|main)>", "\n", fallback)
    fallback = re.sub(r"(?is)<[^>]+>", " ", fallback)
    return trim_to_first_heading(clean_markdown_text(html.unescape(fallback)))


def extract_original_content_url(document_html: str) -> str | None:
    match = re.search(
        r'<meta\s+name=["\']original_content_git_url["\']\s+content=["\']([^"\']+)["\']',
        document_html,
        flags=re.IGNORECASE,
    )
    if not match:
        return None
    return html.unescape(match.group(1))


def github_blob_to_raw(url: str) -> str | None:
    match = re.match(r"https://github\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.+)", url)
    if not match:
        return None
    owner, repo, branch, path = match.groups()
    return f"https://raw.githubusercontent.com/{owner}/{repo}/{branch}/{path}"


def extract_markdown_body(document_html: str, timeout: int, retries: int) -> str | None:
    original_url = extract_original_content_url(document_html)
    if not original_url:
        return None
    raw_url = github_blob_to_raw(original_url)
    if not raw_url:
        return None
    try:
        return decode_html(fetch(raw_url, timeout=timeout, retries=retries)).strip()
    except Exception:
        return None


def markdown_for(doc: SourceDoc, source_title: str, body: str, fetched_at: str) -> str:
    title = source_title or doc.title
    metadata = [
        "---",
        f"title: {json.dumps(title, ensure_ascii=False)}",
        f"category: {json.dumps(doc.category, ensure_ascii=False)}",
        f"sourceUrl: {json.dumps(doc.url, ensure_ascii=False)}",
        f"fetchedAt: {json.dumps(fetched_at, ensure_ascii=False)}",
        "contentType: raw-extracted-markdown",
        "---",
        "",
        f"# {title}",
        "",
        f"Source: {doc.url}",
        "",
        body,
        "",
    ]
    return "\n".join(metadata)


def load_docs(manifest: Path | None) -> list[SourceDoc]:
    raw_docs = DEFAULT_DOCS
    if manifest:
        raw_docs = json.loads(manifest.read_text(encoding="utf-8"))
    docs = []
    for item in raw_docs:
        docs.append(SourceDoc(slug=item["slug"], category=item["category"], title=item["title"], url=item["url"]))
    return docs


def crawl_doc(doc: SourceDoc, output_root: Path, timeout: int, retries: int) -> Path:
    payload = fetch(doc.url, timeout=timeout, retries=retries)
    document_html = decode_html(payload)
    source_title = extract_title(document_html, doc.title)
    body = extract_markdown_body(document_html, timeout=timeout, retries=retries) or extract_article_text(document_html)
    body = trim_to_preferred_heading(body, doc.title)
    fetched_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    output_dir = output_root / doc.category
    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{doc.slug}.md"
    output_path.write_text(markdown_for(doc, source_title, body, fetched_at), encoding="utf-8")
    return output_path


def parse_args(argv: Iterable[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, help="Optional JSON document manifest; defaults to the built-in 8 seed docs.")
    parser.add_argument("--output-dir", type=Path, default=Path("samples/knowledge/raw"), help="Output root directory.")
    parser.add_argument("--timeout", type=int, default=25, help="HTTP timeout per request in seconds.")
    parser.add_argument("--retries", type=int, default=2, help="Fetch attempts per URL.")
    parser.add_argument("--continue-on-error", action="store_true", help="Continue crawling remaining URLs when one fails.")
    return parser.parse_args(list(argv))


def main(argv: Iterable[str]) -> int:
    args = parse_args(argv)
    docs = load_docs(args.manifest)
    failures = []
    for doc in docs:
        try:
            output_path = crawl_doc(doc, args.output_dir, timeout=args.timeout, retries=args.retries)
            print(f"wrote {output_path}")
        except Exception as exc:  # noqa: BLE001 - CLI should report all crawl failures.
            failures.append((doc.url, str(exc)))
            print(f"error {doc.url}: {exc}", file=sys.stderr)
            if not args.continue_on_error:
                return 1
    if failures:
        print("\nCompleted with failures:", file=sys.stderr)
        for url, error in failures:
            print(f"- {url}: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
