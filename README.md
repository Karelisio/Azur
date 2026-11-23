# Azur

Appli météo Android native, belle et précise pour la France (modèles Météo-France via Open-Meteo),
avec widgets, module montagne, radar de pluie et vigilances. Locale, sans compte.

## Installation
Télécharger l'APK de la dernière [release](https://github.com/karelisio/azur/releases/latest).
L'appli se met ensuite à jour toute seule (ou via Vitrine si elle est installée).

## Étapes manuelles
1. **Secrets GitHub** (Settings → Secrets and variables → Actions), déjà en place :
   `KEYSTORE_BASE64` (keystore encodé en base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
   Toujours garder le même keystore, sinon les mises à jour échouent.
2. **Publier une version** : modifier `VERSION` (ex. `0.2.0`) et pousser sur `main`.
   La CI construit l'APK signé, crée le tag `v0.2.0` et la Release avec le changelog.
3. **Vigilances Météo-France (facultatif)** : créer un compte gratuit sur
   https://portail-api.meteofrance.fr, s'abonner à l'API « Données Publiques Vigilance »,
   générer une clé API et la coller dans Azur → Réglages → Vigilances.
4. **Téléphone Honor / MagicOS** : dans l'onboarding, autoriser l'exclusion de l'optimisation
   batterie, puis dans Réglages Android → Batterie → Lancement d'applis, passer Azur en
   « Gérer manuellement » (tout activer) pour que widgets et alertes restent à jour.

## Build local
`./gradlew assembleRelease lint testDebugUnitTest` (sans keystore, l'APK release est signé
avec la clé debug). JDK 21 et SDK Android (compileSdk 37.1) requis.

## Sources de données
Open-Meteo (CC BY 4.0) · Météo-France (modèles AROME/ARPEGE, vigilances) · RainViewer (radar)
· OpenStreetMap (fond de carte, © contributeurs OSM).

## Fonctionnalités
- Accueil : ciel vivant (shaders AGSL), température, ressenti, phrase de résumé, pluie dans l'heure
  par quart d'heure, 48 h (courbe animée), 14 jours, UV, vent, humidité, point de rosée, pression,
  visibilité, arc solaire, Lune, qualité de l'air et pollens. Swipe entre lieux.
- Lieux : position actuelle + favoris réordonnables, recherche de villes.
- Montagne (Réglages) : isotherme 0 °C, limite pluie-neige, neige, vent 850/700 hPa, météo à l'altitude choisie.
- Radar de pluie animé (RainViewer sur MapLibre + OSM).
- Vigilances Météo-France (clé facultative) avec bannière et notification.
- Notifications : pluie dans l'heure (toutes les 15 min), résumé du matin.
- Widgets : compact, horaire, semaine, horloge + météo, pluie dans l'heure (fond ciel ou couleurs dynamiques).
- Tuile Réglages rapides avec la température actuelle.

## Captures (vérification visuelle)
`./gradlew testDebugUnitTest -Pscreenshots` génère des captures Robolectric dans `app/build/screenshots`
(accueil clair/sombre, icônes, widgets).

## Choix et limites
- `meteofrance_seamless` ne couvre qu'environ 4 jours et ne fournit ni probabilité de pluie,
  ni UV, ni visibilité, ni isotherme : la requête demande aussi `best_match` et fusionne
  champ par champ (Météo-France prioritaire).
- Isotherme 0 °C : valeur du modèle si disponible, sinon interpolation entre 850 et 700 hPa ;
  limite pluie-neige estimée = isotherme − 300 m.
- Département (vigilances) : code postal du résultat de recherche, ou géocodage inverse Android
  pour la position actuelle. La vigilance suivie est celle du lieu principal (position actuelle sinon 1er favori).
- Widgets, tuile et notifications utilisent le lieu principal et la dernière position connue.
- Radar : RainViewer limite le zoom de ses tuiles gratuites (zoom max 7, agrandi au-delà).
- Ciel vivant : Android 13+ (AGSL) ; dégradé statique sinon ou si « supprimer les animations » est actif.
- Pas d'émulateur dans l'environnement de développement : le rendu est vérifié par captures Robolectric
  et par le shader transposé en WebGL.
