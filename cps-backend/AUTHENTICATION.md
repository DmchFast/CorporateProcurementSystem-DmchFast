# Backend authentication

Only `ADMIN` accounts can create users, query the full user list, and reset
users' passwords. These permissions  are enforced by HTTP security rules and
service method authorization. `MANAGER` remains a separate role. Provision the
first administrator with `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD`.

Authenticated users can change their own password by supplying their current
password. Resetting a password through `/api/users/{id}/password`, including
one's own, requires `ADMIN`.

| Endpoint                       | Access and request                                                                              |
|--------------------------------|-------------------------------------------------------------------------------------------------|
| `GET /api/auth/csrf`           | Public; returns `token`, `headerName`, and `parameterName`                                      |
| `POST /api/auth/login`         | Form fields `username` and `password`; success 204                                              |
| `POST /api/auth/logout`        | Invalidates the current session; success 204                                                    |
| `GET /api/auth/me`             | Authenticated; returns `id`, `username`, `role`, `createdAt`                                    |
| `PUT /api/auth/password`       | Authenticated; JSON `currentPassword`, `newPassword`; success 204                               |
| `GET /api/users`               | Administrator; lists users without credentials                                                  |
| `GET /api/users/{id}`          | Administrator; retrieves a user without credentials                                             |
| `POST /api/users`              | Administrator; JSON `username`, `password`, `role` (`USER`, `MANAGER`, or `ADMIN`); success 201 |
| `PUT /api/users/{id}/password` | Administrator; JSON `password`; enables or resets backend access; success 204                   |

Keep the session cookie received from `/api/auth/csrf`. Send its token using
the returned header name with every modifying request, including login and
logout. After login, request a new CSRF token because Spring rotates the
session ID and clears the pre-login token.
