# Backend authentication

The backend authenticates usernames and BCrypt passwords with Spring Security's
`AuthenticationManager`. It issues a signed JWT access token and an opaque refresh
token. Spring Security's OAuth2 Resource Server validates Bearer tokens. HTTP
sessions, form login, Basic authentication, and authentication cookies are disabled.
All request and response bodies below are JSON.

Only `ADMIN` accounts can create users, query the full user list, and reset users'
passwords. HTTP security and `@PreAuthorize` enforce these permissions. `MANAGER`
remains a procurement role. Users can change their own password by supplying the
current password.

## Configuration

Set `JWT_SECRET` to a Base64-encoded cryptographically random secret containing at
least 32 bytes. There is no default signing secret. Keep this value stable across
restarts and shared across backend instances. Changing it invalidates existing
access tokens. The issuer is an identifier and does not require a running server
at its URL.

| Property                       | Default                               |
|--------------------------------|---------------------------------------|
| `cps.security.jwt.issuer`      | `https://corporate-procurement.local` |
| `cps.security.jwt.audience`    | `cps-api`                             |
| `cps.security.jwt.access-ttl`  | `PT15M`                               |
| `cps.security.jwt.refresh-ttl` | `P30D`                                |

Provision the first administrator with `BOOTSTRAP_ADMIN_USERNAME` and
`BOOTSTRAP_ADMIN_PASSWORD`.

## Endpoints

| Endpoint                       | Access and body                                    | Success            |
|--------------------------------|----------------------------------------------------|--------------------|
| `POST /api/auth/login`         | Public; `username`, `password`                     | 200 token response |
| `POST /api/auth/refresh`       | Refresh token; `refreshToken`                      | 200 token response |
| `POST /api/auth/logout`        | Refresh token; `refreshToken`                      | 204                |
| `GET /api/auth/me`             | Bearer access token                                | 200 user details   |
| `PUT /api/auth/password`       | Bearer token; `currentPassword`, `newPassword`     | 204                |
| `GET /api/users`               | Admin Bearer token                                 | 200 all users      |
| `GET /api/users/{id}`          | Admin Bearer token                                 | 200 user details   |
| `POST /api/users`              | Admin Bearer token; `username`, `password`, `role` | 201                |
| `PUT /api/users/{id}/password` | Admin Bearer token; `password`                     | 204                |

A token response contains `accessToken`, `refreshToken`, `tokenType` (`Bearer`),
`expiresIn`, and `refreshExpiresIn` (lifetimes in seconds). Send the access JWT in
`Authorization: Bearer <accessToken>` for protected requests. Refresh and logout
authenticate with the refresh token in the JSON body and work even when an access
JWT supplied in a header has expired. Tokens are never accepted from cookies.
CSRF tokens are unnecessary for this explicit-header/body token transport.

Refresh rotates the refresh token and returns a new access JWT. Replace both stored
tokens after each successful refresh and serialize refresh calls. Reusing any
consumed refresh token revokes its entire session, including the most recently
issued tokens. Session lifetime is absolute and does not extend with rotation.

## Brute-force limits

| Property                                   | Default |
|--------------------------------------------|---------|
| `cps.security.rate-limit.account-attempts` | 5       |
| `cps.security.rate-limit.account-window`   | `PT15M` |
| `cps.security.rate-limit.ip-attempts`      | 30      |
| `cps.security.rate-limit.ip-window`        | `PT1M`  |
| `cps.security.rate-limit.refresh-attempts` | 60      |
| `cps.security.rate-limit.max-tracked-keys` | 10000   |

Limits are atomic within a backend process and stored in bounded memory. Active
lockouts are not evicted to make room for new usernames. Limits return 429 with
`Retry-After` in seconds. Authentication/session failures return 401,
authorization failures return 403, and validation failures return 400.
