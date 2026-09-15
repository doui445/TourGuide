# TourGuide

Fork du projet pédagogique [OpenClassrooms-Student-Center/JavaPathENProject8](https://github.com/OpenClassrooms-Student-Center/JavaPathENProject8) — une API de guide touristique (calcul d'attractions à proximité, récompenses, tarification) fournie avec un existant qui ne tenait pas la charge (des dizaines de milliers d'utilisateurs simulés).

## Ce que j'ai fait

Le code métier et les tests de performance étaient fournis par OpenClassrooms ; mon travail a porté sur l'**optimisation des performances** de l'API :
- Remplacement du traitement séquentiel par de la **programmation concurrente** (`CompletableFuture`, `ExecutorService`) pour le suivi de localisation et le calcul des attractions à proximité.
- Passage des tests de montée en charge (`TestPerformance`) : traitement de dizaines de milliers d'utilisateurs dans les temps impartis.

## Stack technique

Java 17 · Spring Boot 3 · `CompletableFuture` / `ExecutorService` (concurrence) · JUnit 5

## Lancer le projet

Le projet dépend de 3 librairies locales (`gpsUtil`, `rewardCentral`, `tripPricer`, fournies dans `TourGuide/libs/`) à installer dans le repo Maven local avant de builder :

```bash
cd TourGuide
mvn install:install-file -Dfile=libs/gpsUtil.jar -DgroupId=gpsUtil -DartifactId=gpsUtil -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=libs/RewardCentral.jar -DgroupId=rewardCentral -DartifactId=rewardCentral -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=libs/TripPricer.jar -DgroupId=tripPricer -DartifactId=tripPricer -Dversion=1.0.0 -Dpackaging=jar

./mvnw spring-boot:run
```

## Tests (dont les tests de performance)

```bash
./mvnw test
```
