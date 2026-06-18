# AJInvestment Platform

Live demo:

```text
http://ajinvestment-thabo.southafricanorth.cloudapp.azure.com:8080/AJInvestment
```

[Open the AJInvestment live demo](http://ajinvestment-thabo.southafricanorth.cloudapp.azure.com:8080/AJInvestment)

AJInvestment Platform is a Java/Jakarta EE web application for client registration,
login, email verification, password reset, and facial-login integration. The project
is built as a multi-module NetBeans/Maven application and deployed to WildFly.

The platform is also connected to an Android facial-recognition application. Clients
first register on AJInvestment, then use their registered username, email address, or
client ID when signing in through the mobile application.

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
- Android facial-login integration for registered AJInvestment clients.
- JSON login responses containing the authentication result, token, username, and message.
- Client dashboard displayed after a successful web or facial login.

## Technology Stack

- Java
- Jakarta EE
- WildFly
- Maven
- MySQL / XAMPP
- HTML, CSS, and JavaScript
- Android and Kotlin
- Jetpack Compose and CameraX
- Google ML Kit Face Detection
- HTTP and JSON REST communication

## Facial Recognition Application Integration

The Android facial-login application connects to the AJInvestment backend through an
HTTP JSON API. The mobile application uses CameraX for the camera preview and Google
ML Kit to detect when a face is ready.

After face detection, the application sends a `POST` request to:

```text
/AJInvestment/FaceLogin
```

The JSON request contains:

```json
{
  "userId": "registered username, email, or client ID",
  "faceConfidence": 1.0,
  "deviceId": "Android device identifier"
}
```

The `FaceLoginServlet` checks the supplied user identifier against the clients
registered in the AJInvestment MySQL database. It then returns a JSON response with
the login result, authentication token, client name, and status message.

The Android application uses this backend base URL:

```text
http://ajinvestment-thabo.southafricanorth.cloudapp.azure.com:8080/AJInvestment/
```

Related Android repository:

```text
https://github.com/LebogangL/LoginFaceRecognition.git
```

## Azure Deployment

AJInvestment is deployed on a Microsoft Azure Ubuntu virtual machine. The production
environment uses WildFly as the Jakarta EE application server and MySQL for persistent
client data. The Azure DNS name points to the VM public IP, and the application is
currently exposed over HTTP on port `8080`.



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
