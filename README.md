
# GeoSnap Industridykk — Android MVP

## Hva er dette?
En minimal Android-app (Kotlin) som lar deg ta et bilde, og automatisk legge på:
- **UTM-koordinater (WGS84)**
- **Retning (N/Ø/S/V)**
- **Adresse (når tilgjengelig)**, kommune og fylke
- **Nøyaktighet (± m)**
- **Kommentar (obligatorisk)**

Appen skriver samme data i **EXIF** (GPS, retning, nøyaktighet, kommentar).

## Bygg og kjør
1. Åpne mappen i **Android Studio (Giraffe/Koala+)**
2. La gradle synce ferdig
3. Kjør på en Android-telefon (Android 8.0+). Godkjenn **kamera** og **lokasjon**.

## Bruk
- Live-HUD viser UTM, retning og nøyaktighet. Adresse vises når nett er tilgjengelig.
- Trykk **utløser** → skriv **kommentar** (påkrevd) → appen lagrer bildet til appens filer,
  legger **overlegg** på pikslene og **EXIF** i filen.

### Filplassering
`Android/data/com.geosnap.industridykk.dev/files/` (debug) eller uten `.dev` i release.

## Kjent MVP-begrensning
- Kommentar-dialogen er synkron for enkelhet i MVP (kan erstattes med `suspend`-flyt).
- Reverse-geocoding bruker systemets `Geocoder` (kan være treg/ulik per enhet). For mer robusthet kan en ekstern API brukes i produksjon.
- Heading bruker lokasjonens `bearing` om tilgjengelig; for mer presis kurs kan sensor-fusjon legges til.

## Neste steg
- Implementer bedre kommentar-dialog med `suspendCancellableCoroutine`.
- Egen delingsskjerm (behold/fjern EXIF).
- Offline-kø for adresser og POI.
- Eksport til Galleri via `MediaStore`.
