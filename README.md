# OIDC Backchannel Logout Plugin

A Jenkins plugin that implements **OIDC Backchannel Logout** support for any compliant OpenID Connect (OIDC) Identity Provider.

This plugin bridges the gap between your OIDC Identity Provider and Jenkins by ensuring that when a user logs out from the IdP (or when their session is terminated via the IdP's admin console), their session in Jenkins is immediately invalidated.

> [!WARNING]
> **Breaking change: the backchannel logout endpoint URL has changed.**
>
> The plugin previously registered itself under the `/oidc` path, which conflicted with the [`oidc-provider`](https://plugins.jenkins.io/oidc-provider/) plugin (both claimed `/oidc/*`, making one of them unreachable). To allow both plugins to coexist, this plugin now uses a dedicated, namespaced path.
>
> | | URL |
> |---|---|
> | **Before** | `https://<YOUR_JENKINS_URL>/oidc/backchannel-logout` |
> | **After**  | `https://<YOUR_JENKINS_URL>/oidc-backchannel/logout` |
>
> **Action required:** after upgrading, update the **Backchannel Logout URL** in your Identity Provider's client/application configuration to the new path. If you skip this step, the IdP will keep calling the old URL, receive a `404`, and logout will silently fail (sessions stay active).

## 🚀 Features

* **Session Mapping:** Automatically tracks the relationship between OIDC Session IDs (`sid`) and Jenkins HTTP Sessions upon login.
* **Backchannel Endpoint:** Exposes a secure, unauthenticated endpoint to receive Logout Tokens from your OIDC provider.
* **Instant Termination:** Parses JWT Logout Tokens, validates them, and invalidates the active Jenkins session without requiring user interaction.

## 📋 Prerequisites

To use this plugin, you must have the following installed and configured:

1.  **Jenkins** (Version 2.492.3 or newer recommended).
2.  **OpenId Connect Authentication Plugin** [(`oic-auth`)](https://plugins.jenkins.io/oic-auth/) installed and configured as the Security Realm.
    * *Note:* This plugin relies on `oic-auth` to handle the OIDC Core Features. 

## ⚙️ Configuration

You need to configure your OIDC Identity Provider to send backchannel logout requests to Jenkins.

1.  Log in to your OIDC provider's admin console.
2.  Navigate to the client/application representing Jenkins.
3.  Locate the **Backchannel Logout URL** or equivalent field.
4.  Enter the URL in the following format:
    ```
    https://<YOUR_JENKINS_URL>/oidc-backchannel/logout
    ```
5.  Save the configuration.

## 🔍 How It Works

1.  **Login:**
    When a user logs in via OIDC, this plugin's `SessionListener` captures the `sid` (Session ID) from the ID Token and maps it to the current Jenkins `HttpSession` in memory.

2.  **Logout Event:**
    When a logout occurs in your OIDC provider (e.g., user signs out or an admin terminates the session), the provider sends a `POST` request with a signed JWT (`logout_token`) to the URL configured above.

3.  **Invalidation:**
    The plugin validates the token, extracts the `sid`, looks up the corresponding Jenkins session, and calls `session.invalidate()`. The next time the user tries to click a link in Jenkins, they will be redirected to the login page.

## 🛠️ Demo
*This demo below uses Keycloak as an example OIDC Identity Provider.*
![Demo](/docs/images/demo.gif)

## ⚠️ Limitations

**SessionTracker is in-memory only:**
This plugin uses an in-memory map to track the relationship between OIDC session IDs (`sid`) and Jenkins HTTP sessions. As a result, it does **not** support High Availability (HA) or clustered Jenkins environments (such as CloudBees CI HA) where multiple controller instances are used. Session mappings will not be shared across nodes, so backchannel logout may not work reliably in these scenarios.

## 🤝 Contributing

Contributions are welcome! Please submit a Pull Request or open an issue if you encounter any bugs.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.