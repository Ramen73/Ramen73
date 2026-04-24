package com.ramen73.ramenchat

object AppConfig {
    // Per abilitare le notifiche push in background:
    // 1. Vai su Firebase Console → Impostazioni progetto → Cloud Messaging
    // 2. Copia il valore "Chiave server" (o "Legacy server key")
    // 3. Sostituisci la stringa qui sotto con la tua chiave
    const val FCM_SERVER_KEY = "INSERISCI_LA_TUA_CHIAVE_SERVER_FCM_QUI"
}
