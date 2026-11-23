# Azur — appli météo Android (com.karelisio.azur)

## Standards
- Kotlin + Jetpack Compose, Material 3 Expressive (`MaterialExpressiveTheme`), français uniquement.
- Module unique `app`, DI manuelle via `AppContainer` (`context.container`), ViewModels via `azurViewModel { }`.
- Données internes toujours en °C, km/h, mm, hPa ; conversion à l'affichage via `domain/Units`.
- Pas de secret dans le dépôt : signature par variables d'env (CI), clé Météo-France saisie dans l'appli.
- Dépendances uniquement dans `gradle/libs.versions.toml`. Un commit par fonctionnalité, build vert.
- Animations : toujours respecter `LocalReduceMotion` (fond statique, pas d'animation infinie).

## Commandes
- `./gradlew assembleRelease lint testDebugUnitTest` : vérification complète (comme la CI).
- `./gradlew testDebugUnitTest` : tests unitaires (fixtures JSON dans `app/src/test/resources`).
- `./gradlew installDebug` : installe `com.karelisio.azur.debug`.
- Version : fichier `VERSION` (X.Y.Z) ; versionCode = `-PversionCode` ou `GITHUB_RUN_NUMBER`.

## Architecture (`app/src/main/java/com/karelisio/azur`)
- `data/net` : client OkHttp + kotlinx.serialization (Open-Meteo, RainViewer, vigilance MF).
- `data/db` : Room (lieux + cache JSON brut par lieu) ; `data/SettingsRepository` : DataStore.
- `data/WeatherRepository`, `data/PlaceRepository`, `data/LocationProvider`.
- `domain/` : modèles, codes WMO, pluie dans l'heure, phrase de résumé, unités, Lune, montagne, air.
- `ui/` : `theme`, `nav` (routes typées), écrans (`home`, `day`, `places`, `radar`, `settings`,
  `onboarding`, `permissions`), `sky` (ciel vivant AGSL), `components`, `update`.
- `widget/` (Glance), `notif/` (WorkManager), `tile/` (Réglages rapides), `update/` (MAJ in-app).

## Données
- Forecast : `models=meteofrance_seamless,best_match`, fusion champ par champ (Météo-France
  prioritaire, `best_match` pour jours 5-14, probabilité de pluie, UV, visibilité).
- Air : air-quality-api (AQI européen, pollens). Recherche : geocoding-api.

## CI / release
- `.github/workflows/android.yml` : sur `main`, si le tag `vX.Y.Z` n'existe pas → APK signé,
  tag + GitHub Release (APK + changelog des commits). Sinon build de vérification.
- L'appli lit `releases/latest` (au plus toutes les 6 h) et installe via PackageInstaller,
  ou propose Vitrine (`vitrine://app/com.karelisio.azur`) si installée.
