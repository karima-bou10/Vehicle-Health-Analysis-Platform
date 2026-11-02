# Vehicle Health Analysis Platform

Plateforme backend de suivi de l’état de santé de véhicules connectés, développée avec Java 21, Spring Boot, PostgreSQL et Docker. Elle reçoit des données télématiques, calcule un score de santé, génère des alertes et expose les résultats via des API REST.

## Sommaire

- [1. Objectif](#1-objectif)
- [2. Architecture](#2-architecture)
- [3. Microservices](#3-microservices)
- [4. Flux fonctionnel](#4-flux-fonctionnel)
- [5. Règles métier](#5-règles-métier)
- [6. Technologies](#6-technologies)
- [7. Structure du dépôt](#7-structure-du-dépôt)
- [8. Démarrage rapide avec Docker](#8-démarrage-rapide-avec-docker)
- [9. Vérification des conteneurs](#9-vérification-des-conteneurs)
- [10. Tester les API](#10-tester-les-api)
- [11. Documentation Swagger](#11-documentation-swagger)
- [12. Bases de données](#12-bases-de-données)
- [13. Fournisseur externe et WireMock](#13-fournisseur-externe-et-wiremock)
- [14. Tests](#14-tests)
- [15. Gestion des erreurs](#15-gestion-des-erreurs)
- [16. Commandes Docker utiles](#16-commandes-docker-utiles)
- [17. Exécution locale sans Docker](#17-exécution-locale-sans-docker)
- [18. Limites et évolutions possibles](#18-limites-et-évolutions-possibles)

## 1. Objectif

La plateforme permet de :

1. recevoir la télémétrie d’un véhicule connecté ;
2. enregistrer les données reçues ;
3. déclencher une analyse de manière asynchrone ;
4. calculer un `health score` compris entre 0 et 100 ;
5. expliquer le score à travers des pénalités détaillées ;
6. générer des alertes selon les anomalies détectées ;
7. consulter le dernier rapport et les alertes d’un véhicule ;
8. prendre en compte les campagnes de rappel retournées par un fournisseur externe simulé avec WireMock pendant les tests.

## 2. Architecture

La solution contient deux microservices indépendants et deux bases PostgreSQL séparées.

```text
Client / véhicule / Postman
             |
             | POST /api/v1/telemetry
             v
+---------------------------+
| telemetry-service         |
| Port : 8081               |
| Base : telemetry_db       |
+---------------------------+
             |
             | REST asynchrone avec @Async
             | POST /internal/api/v1/analyses
             v
+---------------------------+
| vehicle-analysis-service  |
| Port : 8082               |
| Base : analysis_db        |
+---------------------------+
        |              |
        |              | GET /api/v1/recalls/{vin}
        |              v
        |       Fournisseur externe
        |       simulé par WireMock
        |
        +--> Health score, pénalités, alertes et rapport
```

### Principes appliqués

- Une base est dédiée à chaque microservice.
- Un microservice ne lit jamais directement les tables de l’autre.
- La communication entre les services est réalisée par HTTP REST.
- `telemetry-service` retourne rapidement `202 Accepted` puis déclenche l’analyse avec `@Async`.
- `telemetryId` garantit qu’une télémétrie ne produit qu’un seul rapport.
- Le fournisseur externe est tolérant aux pannes : si son appel échoue, l’analyse locale continue sans rappel.

## 3. Microservices

### 3.1 telemetry-service

Responsabilités :

- exposer `POST /api/v1/telemetry` ;
- valider le payload reçu ;
- enregistrer la télémétrie dans `telemetry_db` ;
- gérer les statuts `RECEIVED`, `PROCESSING`, `ANALYZED` et `FAILED` ;
- déclencher l’analyse dans un thread asynchrone ;
- appeler `vehicle-analysis-service` par REST.

Architecture interne simple en couches :

```text
controller
service
repository
entity
dto
client
config
exception
```

### 3.2 vehicle-analysis-service

Responsabilités :

- exposer l’API interne d’analyse ;
- calculer le health score ;
- générer les pénalités ;
- générer et enregistrer les alertes ;
- appeler le fournisseur externe des campagnes de rappel ;
- enregistrer les rapports dans `analysis_db` ;
- exposer le dernier rapport et les alertes d’un véhicule.

API exposées :

```text
POST /internal/api/v1/analyses
GET  /api/v1/vehicles/{vin}/health-report
GET  /api/v1/vehicles/{vin}/alerts
```

## 4. Flux fonctionnel

### 4.1 Réception d’une télémétrie

1. Le client appelle `POST /api/v1/telemetry`.
2. `telemetry-service` valide et enregistre les données.
3. Le service retourne immédiatement `202 Accepted`.
4. `AnalysisDispatcher` démarre le traitement avec `@Async`.
5. Le statut passe de `RECEIVED` à `PROCESSING`.
6. `telemetry-service` appelle l’API interne de `vehicle-analysis-service`.
7. Le service d’analyse vérifie si un rapport existe déjà pour le `telemetryId`.
8. Il appelle le fournisseur des campagnes de rappel.
9. Il calcule le score, les pénalités et les alertes.
10. Le rapport est enregistré dans `analysis_db`.
11. Le statut de la télémétrie devient `ANALYZED`.
12. Si l’analyse échoue, le statut devient `FAILED`.

### 4.2 Idempotence

La colonne `telemetry_id` du rapport est unique. Si la même télémétrie est envoyée une deuxième fois, le rapport existant est retourné et aucun doublon n’est créé.

## 5. Règles métier

Le score initial est égal à `100`. Les pénalités sont additionnées, puis retirées du score initial.

```text
healthScore = max(0, 100 - totalDesPenalites)
```

Le score ne peut jamais être inférieur à `0` ni supérieur à `100`.

### 5.1 Kilométrage

| Kilométrage | Pénalité |
|---:|---:|
| Moins de 50 000 km | 0 |
| De 50 000 à 99 999 km | 5 |
| De 100 000 à 149 999 km | 10 |
| 150 000 km ou plus | 15 |

### 5.2 Pression des pneus

Chaque pneu est évalué séparément.

| Pression | Pénalité par pneu |
|---:|---:|
| De 2,0 à 2,8 bar inclus | 0 |
| De 1,8 à moins de 2,0 bar | 3 |
| Moins de 1,8 bar | 8 |
| Plus de 2,8 bar | 5 |

La pénalité totale des pneus est plafonnée à `20` points.

### 5.3 Batterie

| Batterie | Pénalité |
|---:|---:|
| 50 % ou plus | 0 |
| De 20 % à 49 % | 5 |
| Moins de 20 % | 15 |

### 5.4 Température moteur

| Température | Pénalité |
|---:|---:|
| Jusqu’à 105 °C inclus | 0 |
| Plus de 105 °C jusqu’à 115 °C inclus | 10 |
| Plus de 115 °C | 25 |

### 5.5 Niveau de carburant

| Carburant | Pénalité |
|---:|---:|
| 20 % ou plus | 0 |
| De 10 % à 19 % | 3 |
| Moins de 10 % | 8 |

### 5.6 Codes d’erreur

- 10 points par code unique ;
- les valeurs nulles ou vides sont ignorées ;
- les codes sont normalisés en majuscules ;
- un code dupliqué est compté une seule fois ;
- la pénalité totale est plafonnée à 30 points.

### 5.7 Campagnes de rappel

Seules les campagnes avec le statut `OPEN` influencent le score.

| Gravité | Pénalité |
|---|---:|
| `LOW` | 3 |
| `MEDIUM` | 7 |
| `HIGH` | 15 |
| `CRITICAL` | 25 |

La pénalité totale des rappels est plafonnée à `40` points. Les campagnes `COMPLETED` et `CANCELLED` sont ignorées.

### 5.8 Statut global

| Score | Statut |
|---:|---|
| 80 à 100 | `HEALTHY` |
| 60 à 79 | `WARNING` |
| 40 à 59 | `DEGRADED` |
| 0 à 39 | `CRITICAL` |

### 5.9 Alertes

Les alertes possibles sont :

```text
TIRE_PRESSURE
LOW_BATTERY
ENGINE_TEMPERATURE
LOW_FUEL
ERROR_CODE
ACTIVE_RECALL
```

Les niveaux de gravité sont :

```text
LOW
MEDIUM
HIGH
CRITICAL
```

Un kilométrage élevé réduit le score, mais ne génère pas d’alerte. Chaque code d’erreur unique et chaque campagne de rappel ouverte génèrent une alerte distincte.

## 6. Technologies

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Jakarta Validation
- Spring `RestClient`
- Spring `@Async`
- PostgreSQL
- Hibernate
- Maven
- JUnit 5
- AssertJ
- Mockito
- WireMock
- Springdoc OpenAPI / Swagger UI
- Docker
- Docker Compose

## 7. Structure du dépôt

```text
vehicle-health-platform/
├── telemetry-service/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── Dockerfile
│   └── .dockerignore
│
├── vehicle-analysis-service/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── Dockerfile
│   └── .dockerignore
│
├── compose.yaml
└── README.md
```

## 8. Démarrage rapide avec Docker

### 8.1 Prérequis

Installer :

- Git ;
- Docker Desktop, ou Docker Engine avec le plugin Docker Compose.

Java, Maven et PostgreSQL ne sont pas requis sur la machine si le projet est lancé entièrement avec Docker.

Vérifier l’installation :

```bash
git --version
docker --version
docker compose version
docker info
```

`docker info` doit fonctionner sans erreur. Sous Windows ou macOS, démarrer Docker Desktop avant de poursuivre.

### 8.2 Cloner le projet

```bash
git clone <URL_DU_DEPOT_GIT>
cd vehicle-health-platform
```

Remplacer `<URL_DU_DEPOT_GIT>` par l’URL réelle du dépôt.

### 8.3 Vérifier la configuration Compose

```bash
docker compose config
```

Cette commande doit terminer sans erreur.

### 8.4 Construire les images

```bash
docker compose build
```

Le premier build peut prendre plusieurs minutes car Docker télécharge les images Maven, Java et PostgreSQL.

### 8.5 Démarrer la plateforme

```bash
docker compose up -d
```

### 8.6 Vérifier l’état des services

```bash
docker compose ps
```

Résultat attendu après le démarrage :

```text
telemetry-db               healthy
analysis-db                healthy
vehicle-analysis-service   healthy
telemetry-service          healthy
```

Si un service n’est pas encore `healthy`, attendre quelques secondes et relancer :

```bash
docker compose ps
```

### 8.7 Vérifier les endpoints de santé

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Réponse attendue :

```json
{
  "status": "UP"
}
```

### 8.8 Tester les API

Utiliser Postman, Swagger UI ou les commandes `curl` de la section suivante.

### 8.9 Arrêter la plateforme

Sans supprimer les données :

```bash
docker compose down
```

Les volumes PostgreSQL sont conservés.

Pour supprimer également toutes les données :

```bash
docker compose down -v
```

Attention : l’option `-v` supprime définitivement les volumes de développement.

## 9. Vérification des conteneurs

### Afficher les logs de tous les services

```bash
docker compose logs -f
```

### Logs de telemetry-service

```bash
docker compose logs -f telemetry-service
```

### Logs de vehicle-analysis-service

```bash
docker compose logs -f vehicle-analysis-service
```

### Logs des bases

```bash
docker compose logs -f telemetry-db
docker compose logs -f analysis-db
```

Utiliser `Ctrl+C` pour quitter l’affichage des logs. Les conteneurs restent actifs.

## 10. Tester les API

### 10.1 Envoyer une télémétrie

```http
POST http://localhost:8081/api/v1/telemetry
Content-Type: application/json
```

Exemple de payload :

```json
{
  "vin": "VF1ABC12345678901",
  "timestamp": "2026-11-02T10:00:00Z",
  "mileageKm": 125000,
  "tirePressure": {
    "frontLeft": 2.3,
    "frontRight": 2.2,
    "rearLeft": 2.4,
    "rearRight": 1.7
  },
  "batteryLevelPercent": 35,
  "engineTemperatureCelsius": 108,
  "fuelLevelPercent": 18,
  "errorCodes": [
    "P0217",
    "P0562"
  ]
}
```

Commande `curl` :

```bash
curl -X POST http://localhost:8081/api/v1/telemetry \
  -H "Content-Type: application/json" \
  -d '{
    "vin": "VF1ABC12345678901",
    "timestamp": "2026-11-02T10:00:00Z",
    "mileageKm": 125000,
    "tirePressure": {
      "frontLeft": 2.3,
      "frontRight": 2.2,
      "rearLeft": 2.4,
      "rearRight": 1.7
    },
    "batteryLevelPercent": 35,
    "engineTemperatureCelsius": 108,
    "fuelLevelPercent": 18,
    "errorCodes": ["P0217", "P0562"]
  }'
```

Réponse attendue :

```http
202 Accepted
```

```json
{
  "telemetryId": "UUID_GENERE",
  "vin": "VF1ABC12345678901",
  "status": "RECEIVED",
  "message": "Telemetry accepted for analysis"
}
```

Le traitement est asynchrone. Attendre environ une seconde avant de consulter le rapport.

### 10.2 Consulter le dernier rapport

```http
GET http://localhost:8082/api/v1/vehicles/VF1ABC12345678901/health-report
```

```bash
curl http://localhost:8082/api/v1/vehicles/VF1ABC12345678901/health-report
```

Exemple de réponse :

```json
{
  "reportId": "UUID_DU_RAPPORT",
  "telemetryId": "UUID_DE_LA_TELEMETRIE",
  "vin": "VF1ABC12345678901",
  "healthScore": 44,
  "status": "DEGRADED",
  "telemetryTimestamp": "2026-11-02T10:00:00Z",
  "analyzedAt": "2026-11-02T10:00:01Z",
  "penalties": [
    {
      "category": "MILEAGE",
      "points": 10,
      "reason": "Mileage is between 100000 and 149999 km"
    },
    {
      "category": "TIRE_PRESSURE",
      "points": 8,
      "reason": "rear-right tire pressure is critically low: 1.7 bar"
    },
    {
      "category": "BATTERY",
      "points": 5,
      "reason": "Battery level is low: 35%"
    },
    {
      "category": "ENGINE_TEMPERATURE",
      "points": 10,
      "reason": "Engine temperature is high: 108.0 °C"
    },
    {
      "category": "FUEL",
      "points": 3,
      "reason": "Fuel level is low: 18%"
    },
    {
      "category": "ERROR_CODE",
      "points": 20,
      "reason": "Diagnostic error codes detected: P0217, P0562"
    }
  ]
}
```

Le fournisseur de rappels n’est pas lancé dans Docker. Son indisponibilité est journalisée et l’analyse continue sans pénalité `RECALL`.

### 10.3 Consulter les alertes

```http
GET http://localhost:8082/api/v1/vehicles/VF1ABC12345678901/alerts
```

```bash
curl http://localhost:8082/api/v1/vehicles/VF1ABC12345678901/alerts
```

Exemple simplifié :

```json
{
  "vin": "VF1ABC12345678901",
  "alertCount": 6,
  "alerts": [
    {
      "id": "UUID_ALERTE",
      "type": "ENGINE_TEMPERATURE",
      "severity": "HIGH",
      "message": "Engine temperature is too high",
      "measuredValue": "108.0 °C",
      "thresholdValue": "maximum 105 °C",
      "createdAt": "2026-11-02T10:00:01Z"
    }
  ]
}
```

Un véhicule sans anomalie retourne `200 OK` avec :

```json
{
  "vin": "VF1ABC12345678901",
  "alertCount": 0,
  "alerts": []
}
```

### 10.4 Tester une erreur de validation

Exemple avec un VIN trop court et une batterie invalide :

```json
{
  "vin": "ABC",
  "timestamp": "2026-11-02T10:00:00Z",
  "mileageKm": 25000,
  "tirePressure": {
    "frontLeft": 2.3,
    "frontRight": 2.2,
    "rearLeft": 2.4,
    "rearRight": 2.1
  },
  "batteryLevelPercent": 120,
  "engineTemperatureCelsius": 90,
  "fuelLevelPercent": 60,
  "errorCodes": []
}
```

Résultat attendu :

```http
400 Bad Request
```

### 10.5 Consulter un rapport absent

```http
GET http://localhost:8082/api/v1/vehicles/VF1UNKNOWN0000001/health-report
```

Résultat attendu :

```http
404 Not Found
```

## 11. Documentation Swagger

Après le démarrage Docker :

### telemetry-service

```text
http://localhost:8081/swagger-ui/index.html
```

### vehicle-analysis-service

```text
http://localhost:8082/swagger-ui/index.html
```

Les spécifications OpenAPI sont également disponibles via :

```text
http://localhost:8081/v3/api-docs
http://localhost:8082/v3/api-docs
```

## 12. Bases de données

### 12.1 telemetry-db

Connexion depuis la machine hôte :

```text
Host     : localhost
Port     : 5433
Database : telemetry_db
User     : telemetry_user
Password : telemetry_password
```

Données principales :

- télémétries ;
- pressions des pneus ;
- codes d’erreur ;
- statut du traitement.

### 12.2 analysis-db

Connexion depuis la machine hôte :

```text
Host     : localhost
Port     : 5434
Database : analysis_db
User     : analysis_user
Password : analysis_password
```

Données principales :

- rapports de santé ;
- pénalités ;
- alertes.

### 12.3 Accéder aux bases depuis les conteneurs

```bash
docker compose exec telemetry-db \
  psql -U telemetry_user -d telemetry_db
```

```bash
docker compose exec analysis-db \
  psql -U analysis_user -d analysis_db
```

Exemples SQL :

```sql
SELECT id, vin, status, created_at
FROM telemetries
ORDER BY created_at DESC;
```

```sql
SELECT id, vin, health_score, status, analyzed_at
FROM health_reports
ORDER BY analyzed_at DESC;
```

```sql
SELECT category, points, reason
FROM score_penalties;
```

```sql
SELECT type, severity, message, created_at
FROM alerts
ORDER BY created_at DESC;
```

## 13. Fournisseur externe et WireMock

`vehicle-analysis-service` contient un `RecallProviderClient` qui appelle :

```http
GET /api/v1/recalls/{vin}
```

WireMock est utilisé uniquement dans les tests d’intégration automatisés. Il simule notamment :

- un véhicule avec un rappel ouvert ;
- un véhicule sans rappel ;
- une erreur HTTP du fournisseur.

Le test vérifie que :

- le client appelle le bon endpoint ;
- la réponse JSON est désérialisée ;
- un rappel `OPEN` réduit le score ;
- une alerte `ACTIVE_RECALL` est générée ;
- une erreur du fournisseur n’empêche pas l’analyse locale.

En exécution Docker normale, aucun WireMock permanent n’est lancé. Un avertissement peut donc apparaître pour `RECALL_PROVIDER_URL`, mais le rapport et les pénalités locales restent produits.

## 14. Tests

### 14.1 Exécuter les tests de telemetry-service

Sous Linux ou macOS :

```bash
cd telemetry-service
./mvnw test
```

Sous Windows :

```powershell
cd telemetry-service
.\mvnw.cmd test
```

### 14.2 Exécuter les tests de vehicle-analysis-service

Sous Linux ou macOS :

```bash
cd vehicle-analysis-service
./mvnw test
```

Sous Windows :

```powershell
cd vehicle-analysis-service
.\mvnw.cmd test
```

### 14.3 Types de tests

- `HealthScoreCalculatorTest` : tests unitaires des règles de score ;
- `AlertGeneratorTest` : tests unitaires des alertes ;
- `VehicleAnalysisServiceIntegrationTest` : intégration service, JPA et PostgreSQL ;
- `RecallProviderClientIntegrationTest` : intégration REST et WireMock.

## 15. Gestion des erreurs

Les API retournent une structure d’erreur uniforme :

```json
{
  "timestamp": "2026-11-02T10:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "batteryLevelPercent",
      "message": "must be less than or equal to 100"
    }
  ]
}
```

Codes principaux :

| Cas | Code HTTP |
|---|---:|
| Télémétrie acceptée | `202` |
| Consultation réussie | `200` |
| Payload invalide | `400` |
| Rapport introuvable | `404` |
| Erreur interne inattendue | `500` |

L’indisponibilité du fournisseur de rappels est journalisée mais ne bloque pas l’analyse.

## 16. Commandes Docker utiles

### Construire et démarrer

```bash
docker compose up -d --build
```

### Afficher l’état

```bash
docker compose ps
```

### Afficher les logs

```bash
docker compose logs -f
```

### Redémarrer un service

```bash
docker compose restart telemetry-service
docker compose restart vehicle-analysis-service
```

### Reconstruire uniquement un microservice

```bash
docker compose build telemetry-service
docker compose up -d telemetry-service
```

```bash
docker compose build vehicle-analysis-service
docker compose up -d vehicle-analysis-service
```

### Arrêter sans supprimer les données

```bash
docker compose down
```

### Réinitialiser complètement

```bash
docker compose down -v
docker compose up -d --build
```

### Supprimer les images locales du projet

```bash
docker image rm telemetry-service:local vehicle-analysis-service:local
```

## 17. Exécution locale sans Docker

Cette option nécessite Java 21, Maven ou Maven Wrapper et PostgreSQL local.

Créer deux bases :

```sql
CREATE DATABASE telemetry_db;
CREATE DATABASE analysis_db;
```

Puis démarrer les services dans cet ordre :

### vehicle-analysis-service

Sous Linux ou macOS :

```bash
cd vehicle-analysis-service
./mvnw spring-boot:run
```

Sous Windows :

```powershell
cd vehicle-analysis-service
.\mvnw.cmd spring-boot:run
```

### telemetry-service

Sous Linux ou macOS :

```bash
cd telemetry-service
./mvnw spring-boot:run
```

Sous Windows :

```powershell
cd telemetry-service
.\mvnw.cmd spring-boot:run
```

## 18. Limites et évolutions possibles

### Limites connues

- `@Async` exécute les tâches dans la mémoire de `telemetry-service`. Une tâche peut être perdue si le service s’arrête pendant son exécution.
- Le fournisseur de rappel n’est pas lancé en permanence dans l’environnement Docker local.
- Les seuils et pénalités sont des hypothèses métier documentées pour le prototype.

### Évolutions possibles

- remplacer `@Async` par RabbitMQ ou Apache Kafka ;
- ajouter Spring Security et OAuth 2.0 ;
- ajouter la pagination et le filtrage des alertes ;
- conserver l’historique complet des rapports ;
- corréler les codes d’erreur avec les mesures pour éviter certains doubles comptages ;
- déployer les services sur Kubernetes ou une plateforme cloud.

---

## Résumé des endpoints

| Service | Méthode | Endpoint | Description |
|---|---|---|---|
| telemetry-service | POST | `/api/v1/telemetry` | Reçoit et enregistre une télémétrie |
| vehicle-analysis-service | POST | `/internal/api/v1/analyses` | Déclenche une analyse interne |
| vehicle-analysis-service | GET | `/api/v1/vehicles/{vin}/health-report` | Retourne le dernier rapport |
| vehicle-analysis-service | GET | `/api/v1/vehicles/{vin}/alerts` | Retourne les alertes du véhicule |
| telemetry-service | GET | `/actuator/health` | État du microservice |
| vehicle-analysis-service | GET | `/actuator/health` | État du microservice |