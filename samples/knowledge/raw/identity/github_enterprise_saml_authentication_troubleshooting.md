---
title: "Troubleshooting SAML authentication - GitHub Enterprise Cloud Docs"
category: "identity"
sourceUrl: "https://docs.github.com/en/enterprise-cloud@latest/admin/managing-iam/using-saml-for-enterprise-iam/troubleshooting-saml-authentication"
fetchedAt: "2026-04-24T14:04:36+00:00"
contentType: raw-extracted-markdown
---

# Troubleshooting SAML authentication - GitHub Enterprise Cloud Docs

Source: https://docs.github.com/en/enterprise-cloud@latest/admin/managing-iam/using-saml-for-enterprise-iam/troubleshooting-saml-authentication

# Troubleshooting SAML authentication

If you use SAML single sign-on (SSO) and people are unable to authenticate to access GitHub, you can troubleshoot the problem.


## In this article

- Error: "Current time is earlier than NotBefore condition"

- Users are repeatedly redirected to authenticate

- Error: Digest mismatch

## Error: "Current time is earlier than NotBefore condition"

This error can occur when there's too large of a time difference between your IdP and GitHub, which commonly occurs with self-hosted IdPs.

If you encounter this error, make sure the time on your IdP is properly synced with your NTP server.

If you use ADFS as your IdP, also set NotBeforeSkew in ADFS to 1 minute for GitHub. If NotBeforeSkew is set to 0, even very small time differences, including milliseconds, can cause authentication problems.

## Users are repeatedly redirected to authenticate

If users are repeatedly redirected to the SAML authentication prompt in a loop, you may need to increase the SAML session duration in your IdP settings.

The SessionNotOnOrAfter value sent in a SAML response determines when a user will be redirected back to the IdP to authenticate. If a SAML session duration is configured for 2 hours or less, GitHub will refresh a SAML session 5 minutes before it expires. If your session duration is configured as 5 minutes or less, users can get stuck in a SAML authentication loop.

To fix this problem, we recommend configuring a minimum SAML session duration of 4 hours. For more information, see SAML configuration reference .

## Error: Digest mismatch

A "Digest mismatch" error indicates that your SAML IdP is using a different SAML signing certificate than the one you have uploaded to GitHub.

Re-download this SAML certificate from your IdP and validate it using a tool such as the Format a x509 cert tool from OneLogin. Then update the certificate saved in the GitHub SAML settings.
