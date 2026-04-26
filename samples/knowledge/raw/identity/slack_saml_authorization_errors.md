---
title: "Troubleshoot SAML authorization errors | Slack"
category: "identity"
sourceUrl: "https://slack.com/help/articles/360037402653-Troubleshoot-SAML-authorization-errors?locale=en-US"
fetchedAt: "2026-04-24T14:04:54+00:00"
contentType: raw-extracted-markdown
---

# Troubleshoot SAML authorization errors | Slack

Source: https://slack.com/help/articles/360037402653-Troubleshoot-SAML-authorization-errors?locale=en-US

Troubleshoot SAML authorization errors

# Troubleshoot SAML authorization errors

SAML-based single sign-on (SSO) gives members access to Slack through an identity provider (IDP) of your choice. If you’re having trouble setting this up, find your error message in the table below to learn how to fix it.

Tip: If you don’t see your error message in the table or you’re still having trouble, our Support team is always happy to help. Click the Contact Us button at the top of this page if you need a hand!

## What causes SAML errors?

SAML errors usually occur when there’s missing or incorrect information entered during your SAML setup. You can resolve most of these issues from your IDP settings, but for some, you’ll need to update your SSO settings in Slack as well.

## SAML error messages

Error message

How to fix it

The SAML Response does not contain the correct Identity Provider Issuer. Please check that the Issuer URL in your [IDP] settings matches the Identity Provider Issuer below.

Check your IDP settings to ensure you have the right value copied over to your workspace’s SSO page . The Issuer value in an IDP is typically referred to as an Issuer URL or Entity URL/ID .

The SAML Response is not signed. Please check your [IDP] settings.

Uncheck the Responses Signed box on your workspace’s SSO page or enable signing responses in your IDP settings. If you don’t see these options, contact your IDP.

The SAML Response does not contain the correct Audience. Please check that the Service Provider URL in your [IDP] settings matches the Service Provider Issuer in Advanced Options below.

Make sure the Service Provider Issuer matches the Audience in your IDP settings. The Audience might also be called the SP Entity ID or Relying Party Identifier . We support https://slack.com and your workspace URL ( https://yourteam.slack.com ).

The Assertion of the SAML Response is not signed. Please check your [IDP] settings.

Uncheck the Assertions Signed box on your workspace’s SSO page or enable signing assertions of responses in your IDP settings. If you don’t see these options, contact your IDP.

The SAML Response does not contain the correct Destination, which should look something like https://yourteam.slack.com/sso/saml. Please check your [IDP] settings.

Update the destination in your IDP. The value’s name may vary, but it will typically be one of the following: Reply URL, ACS URL, Assertion Consumer Service URL, Trusted URL, or Endpoint URL.

The SAML Response is missing the ID attribute. Please check your [IDP] settings.

Make sure you’re including the NameID as a claim sent in your IDP in the correct (Persistent) format.

Neither the SAML Response nor Assertion of the SAML Response are signed. Please check your [IDP] settings.

From your IDP settings, enable signing the response , the assertion of the response or both. If you don’t see these options, contact your IDP.

The SAML Response is not signed (though there is a signed and encrypted Assertion with an EncryptedId). Apologies, but Slack doesn’t support this format. Please check your [IDP] settings.

We don’t support this format. Enable signing the response and make sure you’re following the guidelines to set up your SSO properly.

The SAML Response is not version 2.0. Please check your [IDP] settings.

Make sure you’re using SAML 2.0 in your IDP.

The SAML Response was not sent through a HTTP_POST Binding. Please check your [IDP] settings.

Make sure you’re sending the SAML Response in a POST. Then check that you’ve entered the right SSO URL in your IDP settings and configured your IDP properly.

Hmm, it looks like the signature validation failed. Please check the signing certs in your [IDP] settings.

Update the certificate in your workspace's SSO page to match the certificate sent from your IDP.

Drat, your SAML settings were not able to be parsed or saved. Please double-check them and try again.

The maximum length for SAML labels is 20 characters. Check your SAML configuration and make sure you've selected a label that's shorter than 20 characters.

Who can use this feature?

- Workspace Owners and Org Owners

- Available on the Business+ and Enterprise plans

- Available on the Free and Pro plans if you've connected a Salesforce org to Slack

Awesome!

Thanks so much for your feedback!

If you’d like a member of our support team to respond to you, please send a note to feedback@slack.com .

Got it!

If you’d like a member of our support team to respond to you, please send a note to feedback@slack.com .

Was this article helpful? Yes, thanks! Not really

Sorry about that! What did you find most unhelpful? This article didn’t answer my questions or solve my problem I found this article confusing or difficult to read I don’t like how the feature works Other

0 /600

Submit article feedback

If you’d like a member of our support team to respond to you, please send a note to feedback@slack.com .

Oops! We're having trouble. Please try again later!

IN THIS ARTICLE
