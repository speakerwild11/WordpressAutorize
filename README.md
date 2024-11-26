# The Problem
Wordpress, by default, includes four different levels of authorization below that of an administrator. This makes the process of checking and confirming what user group may perform what action or access what resource extremely tedious. Given that Wordpress has an extensive amount of user-generated plugins, fast, reliable authorization testing is needed 

# The Solution
To counter this, Wordpress Autorize automatically logs into and saves the session of a set of user accounts defined in a CSV file. The extension then uses this to make a multitude of requests, such as:

- Can I visit this authed page with X account?
- Can I make this request without a nonce on an admin account?
- Can I make this request without a nonce PARAMETER on an admin account?
- Can I make this request on a lower-privileged account with the administrator's nonce?

etc. 

# Installation
Download the jar under WPAutorize/out/artifacts/WPAutorize_jar. Import it in Burpsuite under the extensions tab.

If you wish to build the extension yourself, you may do some by simply importing the WPAutorize folder into IntelliJ as a project, and building from there.

# Usage

Make a CSV file containing your user credentials (minus the admin account) in the format:

```
Subscriber,password
Editor,password
```

etc.

In the WP Autorize tab, select "Choose File", choose the CSV file, and then "Load Sessions". While in an administrator account and with logging enabled, navigate around sensitive pages and make sensitive requests. Any discrepancies or interesting responses will be logged within the tab.
