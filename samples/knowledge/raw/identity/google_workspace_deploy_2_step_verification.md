---
title: "Deploy 2-Step Verification  |  Security & data protection  |  Google Workspace Help"
category: "identity"
sourceUrl: "https://support.google.com/a/answer/9176657"
fetchedAt: "2026-04-24T14:04:50+00:00"
contentType: raw-extracted-markdown
---

# Deploy 2-Step Verification  |  Security & data protection  |  Google Workspace Help

Source: https://support.google.com/a/answer/9176657

# Deploy 2-Step Verification Stay organized with collections Save and categorize content based on your preferences.

You and your users play important roles in setting up 2-Step Verification (2SV).
Your users can choose their 2SV method, or you can enforce a method for certain
users or groups in your organization. For example, you can require a small team
in Sales to use security keys.

Important : Google is enforcing 2SV for administrator accounts. For details,
go to About 2SV enforcement for
admins .

## Step 1: Notify users of 2-Step Verification deployment

Before deploying 2SV, communicate your company's plans to your users, including
the following:

- What 2SV is and why your company is using it.

- Whether 2SV is optional or required.

- If required, the date by which users must turn on 2SV.

- Which 2SV method is required or recommended.

## Step 2: Allow users to turn on 2-Step Verification

User accounts created before December 2016 have 2SV on by default.

Let users turn on 2SV and use any verification method.

### Watch the video

### To allow users to turn on 2SV

Before you begin: If needed, learn how to apply the setting to a department or group .

You must be signed in as a super administrator for this task.

- In the Google Admin console, go to Menu Security Authentication 2-step verification .

Go to 2-step verification

You must be signed in as a super administrator for this task.

- (Optional) To apply the setting only to some users, at the side, select an organizational unit (often used for departments) or configuration group (advanced).

Group settings override organizational units. Learn more

- Check the Allow users to turn on 2-Step Verification box.

- Select Enforcement Off .

- Click Save. Or, you might click Override for an organizational unit.

To later restore the inherited value, click Inherit (or Unset for a group).

## Step 3: Tell your users to enroll in 2-Step Verification

- Tell your users to enroll in 2SV by following the instructions in Turn on
2-Step Verification .

- Provide instructions for enrolling in 2SV methods:

- Security keys

- Google prompt, text message, or phone call

- Google Authenticator app

- Backup codes

## Step 4: Track users' enrollment

Use reports to measure and track your users' enrollment in 2SV. Check users'
enrollment status, enforcement status, and number of security keys.

### Watch the video

### Track 2SV enrollment

- In the Google Admin console, go to Menu Reporting User Reports Security .

Go to Security

Requires having the Reports administrator privilege.

- (Optional) To add a new column of information, click Settings Add new column . Select the
column to add to the table and click Save .

For more information, go to Manage a user's security
settings .

### View enrollment trends

- From the Admin console Home page, go to Reports Apps
Reports Accounts .

### Identify organizational units and groups that aren't using 2-Step Verification

- In the Google Admin console, go to Menu Security Security center Security health .

Go to Security health

Requires having the Security center administrator privilege, plus read access to users and organizational units .

- Search Security Health for Two-step verification for admins or Two-step verification for users to review 2SV information.

## Step 5: Enforce 2-Step Verification (Optional)

Before you begin: Make sure users are enrolled in 2SV.

Important: When 2SV is enforced, users who have not completed the 2SV
enrollment process, but have added 2-Factor Authentication (2FA) information to
their account, such as a security key or phone number, will be able to sign in
using this information. If you see a sign in from an unenrolled user who belongs
to an organizational unit where 2SV has been enforced, that is a 2SV sign-in.

Before you begin: If needed, learn how to apply the setting to a department or group .

You must be signed in as a super administrator for this task.

- In the Google Admin console, go to Menu Security Authentication 2-step verification .

Go to 2-step verification

You must be signed in as a super administrator for this task.

- (Optional) To apply the setting only to some users, at the side, select an organizational unit (often used for departments) or configuration group (advanced).

Group settings override organizational units. Learn more

- Click Allow users to turn on 2-Step Verification .

- For Enforcement , choose an option:

- On —Starts immediately.

- Turn on enforcement from date —Select the start date. Users see
reminders to enroll in 2SV when they sign in. Users might also receive
email from Google reminding them to enroll in 2SV.

Note: When using the On from date option, enforcement will start
within 24-48 hours of the chosen date. If you want a precise enforcement
start time, use the On option.

- (Optional) To give new employees time to enroll before enforcement applies
to their accounts, for New user enrollment period , select a time frame
from 1 day to 6 months.

During this period, users can sign in with just their passwords.

- (Optional) To let users avoid repeated 2SV checks on trusted devices, under Frequency , check the Allow user to trust the device box.

The first time a user signs in from a new device, they can check a box to
trust their device. Then the user isn't prompted for 2SV on the device
unless the user clears their cookies or revokes the
device or you reset the
user's sign-in cookie.

Avoiding 2SV on trusted devices isn't recommended unless your users
frequently move between devices.

- For Methods , select the enforcement method:

- Any —Users can set up any 2SV method.

- Any except verification codes via text, phone call —Users can set up
any 2SV method except using their phones to receive 2SV verification
codes.

Important : Users who use texts and phone calls to verify will be
locked out of their accounts. To avoid locking out these users from
their accounts:

- Before enforcement, tell users to start using another 2SV method
since 2SV codes won't be available on their phones after the
enforcement date.

- You can use the login_verification Login Audit activity event to
track users who use codes from a text message or voice call. If the
login_challenge_method parameter has the value
idv_preregistered_phone, the user authenticates with a text or voice
verification code.

- Only security key —Users must set up a security key.

Before selecting this enforcement method, find users who already set up
security keys (report data could be delayed up to 48 hours). To view
real-time 2SV status for each user, go to Manage a user's security
settings .

Important: Since the addition of passkeys, the Only security key option now supports both security keys and passkeys as a 2SV method.
Passkeys and security keys both have the same level of phishing
protection. For details, go to Sign in with a passkey instead of a
password .

- If you select Only security key , set the 2-Step Verification policy
suspension grace period .

This period lets users sign in with a backup verification code that you
generate for the user, which is useful when a user loses their security key.
Select the length of this grace period, which starts when you generate the
verification code. For information on backup codes, go to Get backup
verification codes for a user .

Important: If 2SV is enforced in Only security key mode, users
cannot generate their own backup verification codes. An admin must provide
these codes to the user.

- For Security codes , choose whether users can sign in with a security
code.

- Don't allow users to generate security codes —Users can't generate
security codes.

- Allow security codes without remote access —Users can generate
security codes via g.co/sc and use them on the same
device or local network (NAT or LAN).

- Allow security codes with remote access —Users can generate security
codes via g.co/sc and use them on other devices or
networks, such as when accessing a remote server or a virtual machine.

Security codes are different from one-time codes that apps like Google
Authenticator generate. To generate a security code, a user taps the
security key on their device to generate a security code. The security
codes are valid for 5 minutes.

- Click Save. Or, you might click Override for an organizational unit.

To later restore the inherited value, click Inherit (or Unset for a group).

### If users don't comply by the enforcement date

You can give users extra time to enroll by adding them to a group where 2SV
isn't enforced. While this workaround allows users to sign in, it's not
recommended as a standard practice. Learn how to avoid account lockouts when
2-Step Verification is enforced .

## Related topic

View Apps reports on your organization

Send feedback

Except as otherwise noted, the content of this page is licensed under the Creative Commons Attribution 4.0 License , and code samples are licensed under the Apache 2.0 License . For details, see the Google Developers Site Policies . Java is a registered trademark of Oracle and/or its affiliates.

Last updated 2026-04-22 UTC.

Need to tell us more? [[["Easy to understand","easyToUnderstand","thumb-up"],["Solved my problem","solvedMyProblem","thumb-up"],["Other","otherUp","thumb-up"]],[["Missing the information I need","missingTheInformationINeed","thumb-down"],["Too complicated / too many steps","tooComplicatedTooManySteps","thumb-down"],["Out of date","outOfDate","thumb-down"],["Samples / code issue","samplesCodeIssue","thumb-down"],["Other","otherDown","thumb-down"]],["Last updated 2026-04-22 UTC."],[],[]]
