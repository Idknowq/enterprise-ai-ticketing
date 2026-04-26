---
title: "Troubleshoot Azure VPN Client - Azure VPN Gateway | Microsoft Learn"
category: "network"
sourceUrl: "https://learn.microsoft.com/en-us/azure/vpn-gateway/troubleshoot-azure-vpn-client"
fetchedAt: "2026-04-24T14:04:47+00:00"
contentType: raw-extracted-markdown
---

# Troubleshoot Azure VPN Client - Azure VPN Gateway | Microsoft Learn

Source: https://learn.microsoft.com/en-us/azure/vpn-gateway/troubleshoot-azure-vpn-client

Troubleshoot Azure VPN Client



				In this article

					  Summary

 This article helps you troubleshoot Azure VPN Client connection and configuration issues.

 View Status Logs

 View the status log for error messages.

  Click the arrows icon at the bottom-right corner of the Azure VPN Client window to show the  Status Logs .

  Check the logs for errors that might indicate the problem.

  Error messages are displayed in red.

 Clear sign-in information

 This step applies to Microsoft Entra ID authentication. If you're using certificate authentication, this step isn't applicable.

 Clear the sign-in information.

  Select the … next to the profile that you want to troubleshoot. Select  Configure .

  Select  Clear Saved Account .

  Select  Save .

  Try to connect.

  If the connection still fails, continue to the next section.

 Run diagnostics

 Run diagnostics on the VPN client.

  Click the  …  next to the profile on which you want to run diagnostics.

  Select  Diagnose -> Run Diagnosis .

  The client runs a series of tests and displays the results of the tests. The tests include:

 Internet Access – Checks to see if the client has Internet connectivity.

 Client Credentials – Check to see if the Microsoft Entra ID authentication endpoint is reachable.

 Server Resolvable – Contacts the DNS server to resolve the IP address of the configured VPN server.

 Server Reachable – Checks to see if the VPN server is responding or not

  If any of the tests fail, contact your network administrator to resolve the issue. To collect logs, see  Collect client log files .

 Collect client log files

 Collect the VPN client log files. The log files can be sent to support/administrator via a method of your choosing. For example, e-mail.

  Click the "…" next to the profile that you want to run diagnostics on. Select  Diagnose -> Show Logs Directory

  Windows Explorer opens to the folder that contains the log files.

 Next steps

 To report an Azure VPN Client problem, see  Use Feedback Hub - Azure VPN Client .


					Was this page helpful?




								Need help with this topic?
