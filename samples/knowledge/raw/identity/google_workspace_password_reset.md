---
title: "Reset a user's password  |  User management  |  Google Workspace Help"
category: "identity"
sourceUrl: "https://support.google.com/a/answer/33319"
fetchedAt: "2026-04-24T14:04:34+00:00"
contentType: raw-extracted-markdown
---

# Reset a user's password  |  User management  |  Google Workspace Help

Source: https://support.google.com/a/answer/33319

# Reset a user's password Stay organized with collections Save and categorize content based on your preferences.

If a user forgets the password for their managed Google Account (for example,
their Google Workspace or Cloud Identity account) or if you think their account
has been compromised, you can reset their password from the Google Admin
console.

Resetting a password changes it for the user's online accounts. If the user has
Google Drive for desktop, the password doesn't change there. After resetting a
user's password, you should also reset the user's sign-in cookies.

If your organization has Workspace
guests enabled, you can also
reset guests' passwords.

## On this page

- Step 1: Reset a user's password

- Step 2: Reset the user's sign-in cookies

- Step 3: After changing a password

- Reset another administrator's password

- Recover your own administrator password

## Step 1: Reset a user's password

To reset multiple users' passwords in bulk, go to Add or update multiple users
from a CSV file .

If your account uses single sign-on (SSO) with a third-party identity provider, go here, instead .

To reset a user's password, you must be signed in with an administrator account
that has reset password
privileges .

- In the Google Admin console, go to Menu Directory Users .

Go to Users

Requires having the appropriate User management privilege . Without the correct privilege, you won't see all the controls needed to complete these steps.

Note: For guests, go to Menu Directory Guests

- In the Users list, point to the user and click Reset password at the right .

Or if the list is long , type in the search bar at the very top to find
the user's account page. For guests, select the guest's name to open their
account page. Then at the left of their account page, click Reset password . Show me how

- In the Reset password box, select an option:

- Automatically generate a password , such as for a compromised
account. This generates a secure password, which the user will need to
access the account. When they next sign in, they'll be asked to reset
the password again.

- Create password, for example, to type a simpler password you can
give the user over the phone. To view what you type, click Preview . Check the Ask the user to change their
password box so they can change it to something more secure when they
next sign in.

- By default, password minimum length is 8 characters. You can change
password
requirements for your organization.

- Click Reset .

- Choose how to tell the user or guest about their new password:

- Click Copy Password , for example, to send the password in a Google
chat conversation. Then click Done .

- Click Email Password Send to notify the user or
guest that their password has changed. Send it to an email address where
they can receive email. The user or guest also receives a link to
reset their password. Or, if you unchecked the Ask the user to
change their password box in step 4, they don't receive a link and
must contact you for the password.

- Reset the user or guest's sign-in cookies ( steps
below ).

## Step 2: Reset the user's sign-in cookies

Resetting a user or guest's sign-in cookies prevents them from signing in with
an old password.

- In the Google Admin console, go to Menu Directory Users .

Go to Users

Requires having the appropriate User management privilege . Without the correct privilege, you won't see all the controls needed to complete these steps.

Note: For guests, go to Menu Directory Guests

- Open the user's account page: Click the user's name. Or, at the top, in the search box, enter the user's name and open their account page. For more options, go to Find a user account .

- At the top, click Security Sign in cookies Reset .

- (Optional) To return to the account page, at the top, click the Up arrow .

## Step 3: After changing a password

After you reset a password and sign-in cookies, the user or guest is signed out
of all active sessions. To reopen their apps, the user or guest needs to
complete the following actions:

- Google web apps (such as Gmail or Google Drive)—The user or guest has to
sign in again with their new password.

- Google apps on Android —The user or guest is notified they need to verify
their identity by signing in to their account. Already synced data—for
example, email already received in Gmail—is still accessible, but no new
emails can be sent or received until they sign in again with their new
password.

- Google apps on Apple iOS —The user or guest's Google Account is removed
from the account list. The user or guest has to add their account again,
then sign in with their new password.

- Third-party apps connected via OAuth —Third-party mail apps like Apple
Mail and Mozilla Thunderbird―as well as other applications that use mail
scopes to access a user or guest's mail―will stop syncing data after a
password reset, until a new OAuth 2.0 token is granted. A new token is
granted when the user signs in with their Google Account username and new
password. For details, go to Automatic OAuth 2.0 token revocation upon
password
change .

- Third-party apps that require application-specific passwords
(ASPs) —When 2-Step
Verification is in use, application-specific passwords (ASPs) may be required to use
legacy applications that don't support OAuth. After a password reset,
all ASPs are revoked and need to be regenerated. For details, go to Sign in with App
Passwords .

## Reset another administrator's password

To reset the password of another administrator, you must have Super Admin
privileges . If you have Super Admin
privileges, follow the steps to reset a user's password (above on this page).

## Recover your own administrator password

If you can't sign in to the Admin console and need to reset your own
administrator password, go to Recover administrator access to your
account .

Back to top

Google, Google Workspace, and related marks and logos are trademarks of Google LLC. All other company and product names are trademarks of the companies with which they are associated.

Send feedback

Except as otherwise noted, the content of this page is licensed under the Creative Commons Attribution 4.0 License , and code samples are licensed under the Apache 2.0 License . For details, see the Google Developers Site Policies . Java is a registered trademark of Oracle and/or its affiliates.

Last updated 2026-04-22 UTC.

Need to tell us more? [[["Easy to understand","easyToUnderstand","thumb-up"],["Solved my problem","solvedMyProblem","thumb-up"],["Other","otherUp","thumb-up"]],[["Missing the information I need","missingTheInformationINeed","thumb-down"],["Too complicated / too many steps","tooComplicatedTooManySteps","thumb-down"],["Out of date","outOfDate","thumb-down"],["Samples / code issue","samplesCodeIssue","thumb-down"],["Other","otherDown","thumb-down"]],["Last updated 2026-04-22 UTC."],[],[]]
