# RamenChat – Setup Guide

## Requisiti
- Android Studio Hedgehog (2023.1) o superiore
- JDK 17
- Account Firebase (gratuito)

## 1. Configura Firebase

1. Vai su [Firebase Console](https://console.firebase.google.com)
2. Crea un nuovo progetto → **RamenChat**
3. Aggiungi app Android con package name: `com.ramen73.ramenchat`
4. Scarica `google-services.json` e sostituisci il file in `app/google-services.json`

## 2. Abilita Authentication

Firebase Console → **Authentication** → **Sign-in method** → abilita **Email/Password**

## 3. Crea il database Firestore

Firebase Console → **Firestore Database** → **Crea database** → **modalità test**

### ⚠️ Indice composto necessario

Per la lista chat serve un indice composto. Se non esiste, la prima volta che l'app
parte vedrai un messaggio di errore con un **link**: clicca quel link e Firebase
creerà l'indice automaticamente.

Manualmente: Firestore → Indici → Aggiungi indice composto:
- Collezione: `chats`
- Campi: `participants` (array), `lastMessageTime` (descending)

### Regole consigliate (produzione)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    match /chats/{chatId} {
      allow read, update: if request.auth != null &&
        request.auth.uid in resource.data.participants;
      allow create: if request.auth != null &&
        request.auth.uid in request.resource.data.participants;
      match /messages/{msgId} {
        allow read, create: if request.auth != null &&
          request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.participants;
      }
    }
  }
}
```

## 4. Abilita Storage (per foto profilo + audio)

Firebase Console → **Storage** → **Inizia**

Regole consigliate:
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    match /chats/{chatId}/{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## 5. Apri in Android Studio

```
File → Open → seleziona la cartella RamenChat/
```

Attendi Gradle sync, collega dispositivo o emulatore, premi ▶.

## Funzionalità

| Schermata | Funzione |
|-----------|----------|
| Splash | Controllo sessione |
| Login/Registrazione | Email + password |
| Chat List | Lista cronologia conversazioni, online indicator |
| Chat | Messaggi testo + **audio** in tempo reale |
| Nuovo gruppo | **Chat di gruppo** con più partecipanti |
| Profilo | Modifica nome/bio, **foto profilo**, logout |

## Novità ultimi aggiornamenti
- ✅ Foto profilo caricabili
- ✅ Messaggi vocali (tieni premuto il microfono)
- ✅ Chat di gruppo
- ✅ Ricerca utenti case-insensitive
- ✅ Fix stato online/offline
