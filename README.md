# AJInvestment Platform

AJInvestment Platform is a Java/Jakarta EE web application for client registration,
login, email verification, and password reset workflows. The project is built as a
multi-module NetBeans/Maven application and deployed to WildFly.

## Project Modules

- `AJInvestment-ear` - Enterprise archive packaging for deployment.
- `AJInvestment-ejb` - Business services, database setup, email service, and EJB components.
- `AJInvestment-web` - Web application pages and servlets.

## Main Features

- Client registration with form validation.
- Email verification after registration.
- Login page and client access flow.
- Forgot-password request page.
- Secure reset-password link using database tokens.
- Server-side validation for email, passwords, and reset tokens.
- MySQL database initialization through the application startup listener.

## Technology Stack

- Java
- Jakarta EE
- WildFly
- Maven
- MySQL / XAMPP
- HTML, CSS, and JavaScript

## Local Run Notes

1. Start MySQL from XAMPP.
2. Start WildFly.
3. Deploy the EAR file.
4. Open:

```text
http://localhost:8080/AJInvestment
```

For public testing with ngrok:

```cmd
ngrok http 8080
```

Then open the generated ngrok URL with `/AJInvestment` appended.
