# Adventure Book API

A Spring Boot REST API for choose-your-own-adventure books. Players navigate sections, pick options, track health, and save progress across multiple sessions.

## Prerequisites

- Java 21 or higher
- Maven 3.9.4+

## Tech stack

- Java 21
- Spring Boot 3.4.4 (Web, Data JPA, Validation, Actuator)
- H2 file-based database
- Lombok
- SLF4J for logging

## Seed data

On first startup (empty database), the API loads:

- A valid version of the "The Crystal Caverns" by Evelyn Stormrider (replaced gotoId: 666 with 1100 and removed section 666 entirely)
- Two players: Mike (id 1) and Nancy (id 2)


## Configuration

Settings live in `src/main/resources/application.properties`.

| Property                  | Default                        | Description                          |
|---------------------------|--------------------------------|--------------------------------------|
| `server.port`             | `8080`                         | HTTP listen port                     |
| `spring.datasource.url`  | `jdbc:h2:file:./data/adventurebook` | H2 file-based database location |
| `spring.jpa.hibernate.ddl-auto` | `update`                | Schema migration strategy            |
| `spring.h2.console.enabled` | `true`                      | H2 web console at `/h2-console`      |
| `spring.jpa.show-sql`    | `true`                         | Log SQL statements                   |

Data lives in the `data/` directory and stays there across restarts. Seed data only loads if the database is empty.

## Build

```bash
mvn clean package
```

This compiles the code, runs tests, and produces an executable JAR in `target/`.

To skip tests:

```bash
mvn clean package -DskipTests
```

## Run
 
```bash
mvn spring-boot:run
```
 
The API starts on http://localhost:8080 by default.

## Run tests

```bash
mvn test
```

## API endpoints

### Players

#### Register a player

```
POST /api/v1/players
```

Creates a new player. The `name` field is required, must not be blank, and has a max length of 100 characters.

```bash
curl -X POST http://localhost:8080/api/v1/players \
  -H "Content-Type: application/json" \
  -d '{"name": "Billy"}'
```
 

#### List all players

```
GET /api/v1/players
```

```bash
curl http://localhost:8080/api/v1/players
```

#### Get player details

```
GET /api/v1/players/{id}
```

```bash
curl http://localhost:8080/api/v1/players/1
```

### Books

#### List / search books

```
GET /api/v1/books
```

Returns all books. Supports optional query parameters for filtering:

| Parameter    | Type   | Description                                                  |
|--------------|--------|--------------------------------------------------------------|
| `title`      | String | Partial, case-insensitive match on the book title            |
| `author`     | String | Partial, case-insensitive match on the author name           |
| `category`   | Enum   | Exact match: `FICTION`, `SCIENCE`, `HORROR`, `ADVENTURE`, `FANTASY`, `MYSTERY`, `ROMANCE`, `THRILLER` |
| `difficulty` | Enum   | Exact match: `EASY`, `MEDIUM`, `HARD`                        |

Filters can be combined:

```bash
curl "http://localhost:8080/api/v1/books?difficulty=EASY&category=FANTASY"
```
 
#### Get book details

```
GET /api/v1/books/{id}
```

Returns a book by ID, including its sections, options, and consequences.

```bash
curl http://localhost:8080/api/v1/books/1
```
 

#### Create a book

```
POST /api/v1/books
```

Creates a new adventure book. The payload is validated before saving. A `400 Bad Request` is returned if any of these checks fail:

- `title` and `author` must not be blank
- Exactly one `BEGIN` section is required
- At least one `END` section is required
- At least one `END` must be reachable from `BEGIN`
- All `gotoId` values must point to valid section IDs in the book
- Non-ending sections need at least one option

It also returns warnings when it finds potential issues:

- Unknown JSON properties (e.g. a `"type"` field on the book level that the model doesn't have)
- Unreachable sections (no option points to them and they aren't `BEGIN`)
- Duplicate section IDs

Section types: `BEGIN`, `NODE`, `END`
Consequence types: `LOSE_HEALTH`, `GAIN_HEALTH`

```bash
curl -X POST http://localhost:8080/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Dark Forest",
    "author": "Jane Doe",
    "difficulty": "MEDIUM",
    "categories": ["HORROR", "ADVENTURE"],
    "sections": [
      {
        "id": 1,
        "text": "You enter a dark forest...",
        "type": "BEGIN",
        "options": [
          { "description": "Go left", "gotoId": 2 },
          { "description": "Go right", "gotoId": 3 }
        ]
      },
      {
        "id": 2,
        "text": "A wolf attacks you!",
        "type": "NODE",
        "options": [
          {
            "description": "Fight the wolf",
            "gotoId": 3,
            "consequence": { "type": "LOSE_HEALTH", "value": 5, "text": "The wolf bites your arm." }
          }
        ]
      },
      {
        "id": 3,
        "text": "You find a clearing and escape the forest.",
        "type": "END"
      }
    ]
  }'
```
 
#### Add categories to a book

```
PUT /api/v1/books/{id}/categories
```

Adds categories to an existing book. Send a JSON array:

```bash
curl -X PUT http://localhost:8080/api/v1/books/1/categories \
  -H "Content-Type: application/json" \
  -d '["THRILLER", "MYSTERY"]'
```

Response: the full book object with the updated categories.

#### Remove a category from a book

```
DELETE /api/v1/books/{id}/categories/{category}
```

```bash
curl -X DELETE http://localhost:8080/api/v1/books/1/categories/MYSTERY
```

Response: the full book object with the category removed.

### Games

#### Start a game

```
POST /api/v1/games/start/{bookId}?playerId={playerId}
```

Starts a new play-through for a player. They begin at the `BEGIN` section with 10 HP.

```bash
curl -X POST "http://localhost:8080/api/v1/games/start/1?playerId=1"
```


#### Get current game state

```
GET /api/v1/games/{sessionId}
```

Returns the session's current state: player name, health, section text, available options, and paused/alive/finished flags. Same response shape as start game.

```bash
curl http://localhost:8080/api/v1/games/1
```

#### Make a choice

```
POST /api/v1/games/{sessionId}/choose?option={index}
```

Picks an option by zero-based index. Each option has a `hasConsequence` flag so players can see which choices carry risk.

```bash
curl -X POST "http://localhost:8080/api/v1/games/1/choose?option=1"
```

#### Pause a game

```
POST /api/v1/games/{sessionId}/pause
```

Pauses an active session. Progress is saved, and choices are blocked until resumed. Only works on games that are alive, not finished, and not already paused.

```bash
curl -X POST http://localhost:8080/api/v1/games/1/pause
```

#### Resume a game

```
POST /api/v1/games/{sessionId}/resume
```

Unpauses a session so the player can keep playing.

```bash
curl -X POST http://localhost:8080/api/v1/games/1/resume
```

#### Get game history

```
GET /api/v1/games/{sessionId}/history
```

Returns the turn-by-turn log: each choice, consequence, and health change.

```bash
curl http://localhost:8080/api/v1/games/1/history
```

#### Get player sessions

```
GET /api/v1/games/player/{playerId}
```

Lists all sessions for a player, most recent first.

```bash
curl http://localhost:8080/api/v1/games/player/1
```

### Error logs

```
GET /api/v1/errors?hours={hours}
```

Returns recent errors from the database, newest first. `hours` defaults to 24.


```bash
curl http://localhost:8080/api/v1/errors
```
