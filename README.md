# Documentation CoreHost

Le projet **CoreHost** est un écosystème très bien structuré basé sur un réseau **CloudNet**, avec un proxy **Velocity**, des serveurs **Paper** (ou Bukkit), et une communication en temps réel via **Redis** et **MySQL**.

Voici une analyse complète des plugins, accompagnée d'un guide sur l'endroit où les placer et d'un tutoriel de mise en place de A à Z.

---

## 1. Analyse des Plugins

Le projet est divisé en plusieurs modules, chacun ayant un rôle bien défini pour le bon fonctionnement du réseau.

### `corehost-api` (La Bibliothèque Principale)
* **Rôle** : C'est le cœur du système. Il contient la logique commune à tous les autres serveurs (connexions Redis, base de données MySQL via HikariCP, gestion des amis, des groupes/parties, et du système de "Host").
* **Nature** : Ce n'est pas un plugin exécutable en soi, mais une librairie ("API") qui est incluse (compilée) dans les autres plugins ou placée comme dépendance.

### `corehost-proxy` (Le Cerveau du Réseau)
* **Rôle** : Plugin **Velocity** (le proxy). Il gère les connexions entrantes, l'authentification (système de crack/premium), les commandes globales (`/friend`, `/party`, `/msg`, `/ignore`), l'intégration Discord, et la communication CloudNet pour téléporter les joueurs. Il écoute les événements de tout le réseau via Redis.

### `corehost-lobby` (Le Hub Central)
* **Rôle** : Plugin **Paper/Bukkit** dédié aux serveurs Lobby. Il sécurise le monde (bloque les dégâts, la météo, le cycle jour/nuit, le spawn de mobs, etc.). Il s'occupe de l'interface joueur pour rejoindre les serveurs (via des commandes ou menus) et gère la connexion BungeeCord/CloudNet pour envoyer les joueurs vers les mini-jeux.

### `corehost-game` (Le Socle des Mini-jeux)
* **Rôle** : Plugin **Paper/Bukkit** de base pour **tous** les serveurs de jeu. Il charge les mondes via **AdvancedSlimePaper** (format `.slime` pour des mondes légers et rapides), gère l'isolement des joueurs (IsolationListener) et écoute les événements Redis pour lancer les parties envoyées par le proxy ou le lobby.

### `corehost-sumo` (Le Mode de Jeu Sumo)
* **Rôle** : Plugin **Paper/Bukkit** spécifique au mini-jeu Sumo. Il contient la logique pure du jeu (gestion des arènes, des combats, des victoires, etc.). Il travaille en tandem avec `corehost-game`.

---

## 2. Où placer les plugins (Architecture CloudNet)

Dans CloudNet, les serveurs sont gérés par des **Tasks** (Proxy, Lobby, Sumo...) et des **Templates** (les fichiers de base copiés à chaque démarrage de serveur). Voici comment répartir vos fichiers `.jar` (après les avoir compilés) :

| Module / Fichier `.jar` | Où le placer dans CloudNet (Chemin des Templates) | Explications |
| :--- | :--- | :--- |
| `corehost-proxy.jar` | `local/templates/Proxy/default/plugins/` | Sur le template global de vos serveurs Velocity. |
| `corehost-lobby.jar` | `local/templates/Lobby/default/plugins/` | Sur le template global de vos serveurs Lobby (Paper). |
| `corehost-game.jar` | **Dans tous les templates de jeux** (ex: `local/templates/Sumo/default/plugins/`) | Tous les mini-jeux ont besoin de ce socle pour charger les maps `.slime` et communiquer avec le proxy. |
| `corehost-sumo.jar` | `local/templates/Sumo/default/plugins/` | **Uniquement** sur les serveurs qui font tourner le mode Sumo. Doit être accompagné de `corehost-game.jar`. |
| `corehost-api.jar` | *Nulle part manuellement.* | Il est normalement inclus (shaded) dans vos autres plugins lors de la compilation Maven. |

---

## 3. Tutoriel Complet de Mise en Place

Voici les étapes pas à pas pour déployer ce réseau de zéro.

### Étape 1 : Prérequis de l'Infrastructure
Pour faire tourner ce réseau, vous devez avoir d'installé sur votre machine / VPS :
* **Java 21** (Requis par votre `pom.xml`).
* Un serveur **Redis** en cours d'exécution.
* Un serveur **MySQL** ou **MariaDB** avec une base de données créée (ex: `corehost`).
* **CloudNet v4** installé et configuré.

### Étape 2 : Compilation des Plugins
Ouvrez un terminal dans le dossier racine de votre projet et lancez la compilation Maven pour créer les fichiers `.jar` :
```bash
mvn clean package
```
*Vos plugins compilés se trouveront dans les dossiers `target/` de chaque sous-module.*

### Étape 3 : Création des Tasks sur CloudNet
Dans la console de votre CloudNet, créez les Tasks (groupes de serveurs) nécessaires :

**1. Le Proxy (Velocity) :**
```text
tasks create Proxy --environment=VELOCITY --autoStart=true --minServiceCount=1
```
**2. Le Lobby (Paper / AdvancedSlimePaper) :**
```text
tasks create Lobby --environment=MINECRAFT_SERVER --autoStart=true --minServiceCount=1
```
**3. Le Serveur Sumo (Paper / AdvancedSlimePaper) :**
```text
tasks create Sumo --environment=MINECRAFT_SERVER --autoStart=false
```
*(autoStart à false si vous lancez les serveurs à la volée quand des joueurs créent un "Host").*

### Étape 4 : Déploiement des fichiers
Allez dans le dossier `local/templates/` de votre CloudNet et placez les plugins :

1. Dans `local/templates/Proxy/default/plugins/` : Glissez `corehost-proxy-1.0-SNAPSHOT.jar`.
2. Dans `local/templates/Lobby/default/plugins/` : Glissez `corehost-lobby-1.0-SNAPSHOT.jar`.
3. Dans `local/templates/Sumo/default/plugins/` : Glissez `corehost-game-1.0-SNAPSHOT.jar` **ET** `corehost-sumo-1.0-SNAPSHOT.jar`.

> **Attention pour les serveurs de jeu :** Vous utilisez **AdvancedSlimePaper**. Assurez-vous que l'environnement (le `.jar` de lancement) de vos serveurs de jeu (Lobby, Sumo) dans CloudNet utilise bien une version de serveur compatible avec Slime, sinon `corehost-game` plantera au lancement.

### Étape 5 : Configuration des bases de données
Démarrez vos serveurs une première fois via CloudNet pour qu'ils génèrent leurs fichiers de configuration par défaut, puis éteignez-les.

Allez dans les dossiers de templates respectifs, cherchez les dossiers générés par vos plugins (ex: `local/templates/Proxy/default/plugins/CoreHostProxy/config.yml`) et configurez les accès MySQL et Redis. 

**Exemple typique à remplir dans tous les `config.yml` (Lobby, Proxy, Game...) :**
```yaml
redis:
  host: "127.0.0.1"
  port: 6379
  password: "votre_mot_de_passe_redis" # Si vous en avez un

database:
  host: "127.0.0.1"
  port: 3306
  database: "corehost"
  user: "root"
  password: "votre_mot_de_passe_mysql"
```
*(Pour le Proxy, n'oubliez pas de configurer aussi la section Discord avec le token de votre bot).*

### Étape 6 : Lancement
Démarrez votre node CloudNet. Le proxy et le lobby vont s'allumer (car `minServiceCount=1`).
* Les bases de données vont s'initialiser toutes seules (la migration MySQL <-> Redis se fera au démarrage du proxy).
* Les mondes du Lobby se mettront automatiquement en mode protégé.
* Les systèmes de `/friend`, `/party`, et de Host seront liés via Redis !
