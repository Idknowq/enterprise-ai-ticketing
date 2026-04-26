---
title: "Manage a user's security settings  |  Security & data protection  |  Google Workspace Help"
category: "identity"
sourceUrl: "https://support.google.com/a/answer/2537800"
fetchedAt: "2026-04-24T14:04:53+00:00"
contentType: raw-extracted-markdown
---

# Manage a user's security settings  |  Security & data protection  |  Google Workspace Help

Source: https://support.google.com/a/answer/2537800

# Manage a user's security settings Stay organized with collections Save and categorize content based on your preferences.

As an administrator for your organization's Google Workspace or Cloud
Identity service, you can view and manage security settings for a user. For
example, you can reset a user's password, add or remove security keys for
multi-factor authentication, and reset user sign-in cookies.

## Open user security settings

To complete these steps, you need the appropriate administrator privileges. Depending on your privileges, you might not see all the controls needed to complete these steps. Learn more about administrator privileges .

- In the Google Admin console, go to Menu Directory Users .

Go to Users

Requires having the appropriate User management privilege . Without the correct privilege, you won't see all the controls needed to complete these steps.

- In the Users list, find the user.

Tip : To find a user, you can also type the user's name or email address
in the search box at the top of your Admin console. If you need help, go to find a user account .

- Click the user's name to open their account page.

- At the top, click Security .

- View or manage the user's security settings by using the following steps.

## View and manage user security settings

#### Reset a user's password

- Click Password Reset Password .

- Choose to automatically generate the password or enter a password.

By default, password minimum length is 8 characters. You can change password requirements for your organization.

- (Optional) To view the password, click Preview .

- (Optional) To require the user to change the password, ensure that Ask for a password change at the next sign-in is On .

- Click Reset .

- (Optional) To paste the password somewhere, such as in a Google Chat conversation with the user, click Click to copy password .

- Choose to email the password to the user, or click Done .

#### Manage a user's passkeys & security keys

The Passkeys & security keys section displays the following information about keys a user has enrolled:

- Name of the key, either default or user-given

- Platform or device that the key was created on

- The way it's enrolled, either by the user, or automatically

- Passwordless support, whether a passkey can be used to skip password verification challenges

- When and where the keys were added and last used

You can add or remove a user's security keys and remove their passkeys.

Passkeys

Passkeys are a simple and secure alternative to passwords. With passkeys, users can sign in to their managed Google Account using a phone, a security key, or your computer's screen lock. If an admin has enabled skip password on a user's account, they can skip password sign-in challenges and instead use a passkey that incorporates first and second-step verification. For details, go to Sign in with a passkey instead of a password?

Remove a user-created passkey

- Click in Passkeys & security keys to display the key information table.

- Scroll the table all the way to the right.

- Hover over the table line for the key you want to remove and click Remove .

- Click Remove to confirm.

- Click Done .

Admin log events adds an entry each time you revoke a passkey.

Remove an automatically created passkey

Passkeys are automatically generated on Android devices when a user signs in to their Google Account. To remove one:

- Go to the Connected applications section in the user's Security Page.

- Locate the Android device in the Application column that you want to remove.

- Hover over that row and click Remove .

- Click Remove .

- Click Done .

To prevent a user from creating another automatic passkey, reset their password and sign them out of their Google Account (including Google Workspace applications) on all devices and browsers.

- Follow the steps to Reset a user's password .

- To sign them out of their Google Account, follow the steps to Reset the user's sign-in cookies .

If you reset a user's cookies without resetting their password, they will still be able to sign in to their Android device that will add an automatically created passkey again.

For more control over automatically created passkey creation, you can restrict the use of Android devices using basic device management. For details, go to Set up basic mobile device management .

Security keys

A security key is a small device that lets you sign in to a Google Account using 2-Step Verification (2SV). It plugs into your computer's USB port or connects to your mobile device using NFC or Bluetooth. For details, go to Use a security key for 2-Step Verification .

Add a security key as a 2SV only key

You can add a security key for a user, or they can add their own keys. If you add a security key this way, it is for 2SV only and the user needs to sign in with their password when using this key.

- To Add a security key for the user:

- Click in Passkeys & security keys to display the Add security key button.

- Click Add security Key .

- Follow the on-screen instructions.

Note: If you have a security key plugged into your computer, remove your key before registering a new key for a user.

- Click Done .

- To let users add their own security key that can be used for 2SV only:

- Make sure that the skip password setting is off for users or else they can only add a security key as a passkey. For the steps, go to Turn skip passwords on or off for users .

- Tell users to follow the instructions in Use a security key for 2-Step Verification .

Remove a security key

Remove a security key only when the key is lost. If a key is temporarily unavailable, you can generate backup security codes as a temporary workaround. For details, go to Get backup verification codes for a user .

- Click in Passkeys & security keys to display the key information table.

- Scroll the table all the way to the right.

- Hover over the table line for the key you want to remove and click Remove .

- Click Remove to confirm.

- Click Done .

Admin log events adds an entry each time you revoke a security key.

#### Check Advanced Protection enrollment

As an admin, you can check a user's Advanced Protection enrollment status and if necessary, you can unenroll them at the user level.

- The On status means that the user is currently enrolled in Advanced Protection.

- The Off status means that the user is not enrolled in Advanced Protection.

If you turn off Advanced Protection enrollment here, only the user can re-enroll again provided that the Enable user enrollment setting is enabled at Security Authentication Advanced Protection Program . For details, go to Enable users to enroll .

#### Check 2-Step Verification settings

Only the user can turn on 2-Step Verification (2SV). As admin, you can check a user's current 2-Step Verification setting and if necessary get a backup code for a locked-out user.

The 2-Step Verification section shows whether 2SV is turned on for the user, and whether 2SV is currently enforced across your organization.

- You have the option of turning off 2SV for a locked-out user, but this isn't recommended. Instead, get a backup code for the user to allow them to sign in to their account.

Note: You can't turn off 2SV for a user if their account is suspended .

- If 2SV is enforced across your organization, the option to turn off 2SV for an individual user is disabled.

#### Get backup verification codes for a user

Users who temporarily can't access their second authentication method may get locked out. For example, a user may have left their security key at home or can't receive an access code by phone. For these users, you can generate backup verification codes to allow them to sign in.

- To view the user's backup verification codes, click 2-Step Verification Get backup verification codes .

Note: Creating new verification codes invalidates any existing code. For example, if you created a user's verification code using the Admin Console and then generate a new verification code using the verification codes, the previous set of codes is invalidated and vice versa.

- Copy one of the existing backup codes or generate new codes. Note : Select Generate new codes If you think the existing backup codes were stolen or have been used up. The old set of backup codes automatically become inactive.

- Tell your user to follow the instructions in Sign in using backup codes.

If the user is required to use 2-Step Verification with a security key:

- The user can't generate their own backup verification codes. An administrator needs to generate these codes and provide them to the user when needed.

- Once you generate codes for the user, the user's grace period for using these codes begins. You'll be informed of the grace period that's left before they need to use their security key to sign in.

For details on setting up 2-Step Verification requirements for users, go to Deploy 2-Step Verification .

Reseller admins

Only super administrators can generate backup verification codes for other admins. This means that admins, including Reseller admins, can only view and create backup verification codes for their users, not other admins or super admins. If you want to allow admins to generate and view backup verification codes for users, admins, and super admins, you must grant them super admin privileges.

#### Force a password change

If you suspect that the user's password has been stolen, you can force the user to reset their password when they next sign in.

- Click Require password change Turn on .

- Click Done .

After the user resets their password, this setting is automatically set to Off .

Note: If your organization uses SSO through a third-party IdP, the force a password change setting isn't available unless you use a network mask to allow some users to sign in directly to Workplace. To check whether a network mask is set up, go to Security SSO with third-party IDPs SSO profile for your organization .

#### Edit a user's recovery information

If Google suspects an unauthorized attempt to sign in to a user's account, a login challenge appears before access to the account is granted. The user must either:

- Enter a verification code that Google sends to their recovery phone number or recovery email address (an email address outside your organization).

- Answer a challenge that only the account owner can solve.

To add or edit a user's recovery information:

- Click Recovery information .

- Add or edit either of the following:

- Email address (outside of your organization)

- Recovery phone number

Note: The recovery phone number should be unique for each user. If the same recovery phone number is used by multiple users, that number is automatically blocked for security reasons.

- Click Save .

#### Temporarily turn off a login or verify-it's-you challenge

If Google suspects an unauthorized attempt to sign in to a user's account, a login challenge appears before access to the account is granted. The user must enter a verification code that Google sends to their phone. Or, the user can choose to answer another challenge that only the account owner can solve.

Also, if a Google Workspace user attempts a sensitive action, that user is sometimes presented with a verify-it's-you challenge. If the user can't enter the requested information, Google will disallow the sensitive action.

If the authorized user can't verify their identity, you can turn off the login or verify-it's-you challenge for 10 minutes to allow the user to sign in.

#### Reset the user's sign-in cookies

If a user loses their computer or mobile device, you can help prevent unauthorized access to their Google Account by resetting their sign-in cookies. This signs the user out of their Google Account (including any Google Workspace applications) across all devices and browsers.

Note: If you suspended a user, you don't need to do this. Suspending a user resets their sign-in cookies.

If you set up single sign-on (SSO) using a third-party identity provider (IdP) , the user's SSO session may still allow access to their Google Account after resetting their sign-in cookies. In this case, terminate their SSO session before resetting their Google sign-in cookies. For help with SSO management, contact your IdP support team.

To reset the user's cookies:

- Click Sign-in cookies Reset .

- Click Done .

It can take up to an hour to sign the user out of current Gmail sessions. The time for other applications can vary.

#### View and revoke application-specific passwords

If your users use 2-Step Verification and need to sign in to apps or devices that don't accept verification codes, they need application-specific passwords to access those apps. Learn more about signing in with app passwords .

Any apps for which the user has created app passwords are listed in the Application-specific password section. Note : If no app passwords are in use, this section is inactive.

Click an app name for more information on when the password for that app was created and when it was last used.

You should revoke an app password if a user loses a device or stops using an app that was authorized with that password.

- Click in the Application-specific password section to view apps using app passwords.

- Mouse over an app name and click Revoke at right.

- Click Revoke .

- Click Done .

Your users can also revoke their own app passwords .

#### View and remove access to third-party applications

The Connected applications section lists all the third-party applications (for example, Google Workspace Marketplace apps) that have access to this user's Google Account data. Learn how authorized access works.

Note : If no third-party applications have been installed, this section is inactive.

Click an application name for more information:

- The Access level column shows the user data that the application can access. A user can grant full or partial access to Google data.

- The Authorization date column shows when the application was granted data access.

To temporarily remove an app's access to data:

- Mouse over an app name and click Remove at right.

- Click Remove .

- Click Done .

Note : Removing data access for an app doesn't prevent a user from using the app in the future (if the user has the necessary permissions). Once a user signs into the app again, data access is restored. To permanently restrict user access to applications, you can block access to specific application scopes and set up an allowlist of approved apps for your organization.

Google, Google Workspace, and related marks and logos are trademarks of Google LLC. All other company and product names are trademarks of the companies with which they are associated.

Send feedback

Except as otherwise noted, the content of this page is licensed under the Creative Commons Attribution 4.0 License , and code samples are licensed under the Apache 2.0 License . For details, see the Google Developers Site Policies . Java is a registered trademark of Oracle and/or its affiliates.

Last updated 2026-04-22 UTC.

Need to tell us more? [[["Easy to understand","easyToUnderstand","thumb-up"],["Solved my problem","solvedMyProblem","thumb-up"],["Other","otherUp","thumb-up"]],[["Missing the information I need","missingTheInformationINeed","thumb-down"],["Too complicated / too many steps","tooComplicatedTooManySteps","thumb-down"],["Out of date","outOfDate","thumb-down"],["Samples / code issue","samplesCodeIssue","thumb-down"],["Other","otherDown","thumb-down"]],["Last updated 2026-04-22 UTC."],[],[]]
