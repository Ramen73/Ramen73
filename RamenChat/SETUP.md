# RamenChat – Setup Guide

## Requisiti
- Android Studio Hedgehog (2023.1) o superiore
- JDK 17
- Account Firebase (gratuito)

## 1. Configura Firebase

1. Vai su [Firebase Console](https://console.firebase.google.com)
2. Crea un nuovo progetto → **RamenChat**
3. Aggiungi app Android:
   - Package name: `com.ramen73.ramenchat`
   - App nickname: RamenChat
4. Scarica `google-services.json` e **sostituisci** il file placeholder in `app/google-services.json`

### Abilita i servizi Firebase:

**Authentication**
- Firebase Console → Authentication → Sign-in method
- Abilita **Email/Password**

**Firestore Database**
- Firebase Console → Firestore Database → Create database
- Seleziona **Start in test mode** (poi configura le regole di sicurezza)
- Regole consigliate:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    match /chats/{chatId} {
      allow read, write: if request.auth != null &&
        request.auth.uid in resource.data.participants;
      allow create: if request.auth != null;
      match /messages/{msgId} {
        allow read, write: if request.auth != null;
      }
    }
  }
}
```

**Storage** (per foto profilo future)
- Firebase Console → Storage → Get started

## 2. Apri in Android Studio

```bash
File → Open → seleziona la cartella RamenChat/
```

Attendi che Gradle sincronizzi le dipendenze.

## 3. Esegui l'app

- Connetti un dispositivo Android (API 24+) o avvia un emulatore
- Premi ▶ Run

## Funzionalità

| Schermata | Funzione |
|-----------|----------|
| Splash | Logo animato, controllo sessione |
| Login | Accesso con email e password |
| Registrazione | Crea account con nome, email, password |
| Chat List | Lista tutte le conversazioni, indicatore online |
| Chat | Messaggi in tempo reale con Firebase Firestore |
| Nuova Chat | Cerca utenti per nome |
| Profilo | Modifica nome/bio, logout |

## Struttura del progetto

```
app/src/main/
├── java/com/ramen73/ramenchat/
│   ├── SplashActivity.kt
│   ├── LoginActivity.kt
│   ├── RegisterActivity.kt
│   ├── ChatListActivity.kt
│   ├── ChatActivity.kt
│   ├── NewChatActivity.kt
│   ├── ProfileActivity.kt
│   ├── adapter/
│   │   ├── ChatListAdapter.kt
│   │   ├── MessageAdapter.kt
│   │   └── UserSearchAdapter.kt
│   ├── model/
│   │   ├── User.kt
│   │   ├── Message.kt
│   │   └── ChatRoom.kt
│   └── utils/
│       ├── FirebaseUtils.kt
│       └── Extensions.kt
└── res/
    ├── layout/       (tutte le schermate)
    ├── drawable/     (bolle messaggi, avatar, sfondi)
    └── values/       (colori arancione ramen, temi, stringhe)
```
