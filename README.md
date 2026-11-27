# Init-to-Win-It_SOEN-343_MTL-Eats-Project_F26

~~ Team Members ~~ <br>
Nicholas Chamoun; ID: 40264135, GitHub ID: blondedzero, Role: Team Leader. <br>
Emre Emuler; ID: 40212481, GitHub ID: e-muler, Role: Back-End Developer. <br>
Maëlle Fieffé-Bédard; ID: 40231764, GitHub ID: maellefb, Role: Scrum Master. <br>
Carlos Guevara Martinez; ID: 40227586, GitHub ID: carlos21guevara, Role: Database Specialist. <br>
Nagha Abdoul-Aziz Kone; ID: 40307505, GitHub ID: osrlazlo, Role: Front End Developer. <br>

## Instructions and Information
- [How to build and run the app locally](#how-to-build-and-run-the-app-locally)
- [How to run the app with Docker](#run-locally-with-docker)
- [How to run characterization tests](#run-characterization-tests)
- [Seeded accounts to test the app](#accounts-to-test-the-app)

## How to build and run the app locally

### Prerequisites
- Node.js 22 or newer
- Java 21
- npm

### 1. Configure the backend
Create `src/Backend/.env.local` with credentials for a PostgreSQL/Neon database:

```properties
PGUSER=<database username>
PGPASSWORD=<database password>
JWT_SECRET=<generated JWT signing key>
```

Ask a team member for the shared development database credentials, or provide credentials for your own PostgreSQL database.

***Alternatively, you can run the app with a local demo PostgreSQL database through a Docker Container by following [these instructions](#run-locally-with-docker)***

### 2. Configure the frontend
Create `src/Frontend/.env.local` with a Google Maps API key:

```properties
VITE_GOOGLE_MAPS_API_KEY=<Google Maps API key>
```

The Google Maps key is optional. If it is omitted, the rest of the app can still run, but the map page will show a configuration message. You can get a free rate limited Google Maps API demo key [here](https://mapsplatform.google.com/maps-demo-key/)

### 3. Install dependencies
From the repository root, enter the `src` directory and install the frontend and demo-runner dependencies:

```powershell
cd src
npm run install:all
```

### 4. Start the app
On Windows:

```powershell
npm run demo
```

On macOS or Linux:

```bash
npm run demo:mac
```

These commands start both the frontend and backend together:
- Frontend: [http://localhost:5173](http://localhost:5173/)
- Backend: [http://localhost:8080](http://localhost:8080/)

The app can be accessed by navigating to [http://localhost:5173](http://localhost:5173/) 

Press `Ctrl+C` in the terminal to stop both servers.

## Accounts to test the app
These accounts are created by the startup seed (`ApplicationSeeder`). All passwords are `test`. You may also add additional accounts by registering.
<table>
    <tr>
        <th>Email</th>
        <th>Password</th>
        <th>Role</th>
    </tr>
    <tr>
        <td>customer1@email.com ... customer4@email.com</td>
        <td>test</td>
        <td>Customer</td>
    </tr>
    <tr>
        <td>store@iga.com, store@superc.com, store@walmart.com, store@provigo.com, store@maxi.com</td>
        <td>test</td>
        <td>Grocery Store</td>
    </tr>
    <tr>
        <td>courier1@email.com ... courier6@email.com</td>
        <td>test</td>
        <td>Courier</td>
    </tr>
    <tr>
        <td>operator@email.com</td>
        <td>test</td>
        <td>Operator</td>
    </tr>
</table>

## Reset simulation
An operator can use **Reset simulation** to restore the state captured when the demo data was first seeded. Existing seed and startup data is preserved. Active orders created after that baseline are canceled, stores added afterward are closed, and couriers added afterward are taken offline.

## Run characterization tests
The characterization tests are in `src/Backend`. From the repository root, enter that directory:

```powershell
cd src/Backend
```

Run all characterization tests (the pattern matches both `Characterization` and `Characterisation`):

On Windows:

```powershell
.\mvnw.cmd "-Dtest=*Character*Test" test
```

On macOS or Linux:

```bash
./mvnw '-Dtest=*Character*Test' test
```

To run one characterization test, replace the class name below with the desired test class:

```powershell
.\mvnw.cmd -Dtest=DLVModuleCharacterizationTest test
```

For macOS or Linux, use `./mvnw` instead of `.\mvnw.cmd`. Current characterization test classes are `DLVModuleCharacterizationTest`, `ORDModuleCancelOrderCharacterizationTest`, `GSModuleCharacterisationTest`, and `StoreContextDataCharacterisationTest`.

### Frontend component tests
From the repository root, enter `src/Frontend` and run the test suite:

```powershell
cd src/Frontend
npm test
```

To run only the `OrderHistoryLine` tests, use `npm test -- src/tests/OrderHistoryLine.test.jsx`.

## Run locally with Docker
Requires Docker Desktop. The stack is `web` (nginx serving the frontend build and proxying `/api`), `backend` (Spring Boot) and `db` (Postgres 17). The database starts empty and is initialized by the app's seed data.

Create `src/Frontend/.env.local` with a Google Maps API key:

```properties
VITE_GOOGLE_MAPS_API_KEY=<Google Maps API key>
```

The Google Maps key is optional. If it is omitted, the rest of the app can still run, but the map page will show a configuration message. You can get a free rate limited Google Maps API demo key [here](https://mapsplatform.google.com/maps-demo-key/)

From the repository root, build and start the local Docker stack:

```bash
docker compose --env-file src/Frontend/.env.local -f demo/compose.yaml -f demo/rehearse.yaml up --build
```

The local overlay builds the backend and frontend images; Compose passes the key from `.env.local` into the frontend build. The key is embedded in the browser bundle, so use a Google Maps key restricted to the required APIs and your development domains. If you do not need the map, omit `--env-file` and the app still runs.

Open [http://localhost:8088](http://localhost:8088/). Stop the stack with `Ctrl+C`, then remove its containers and database volume with:

```
docker compose -f demo/compose.yaml -f demo/rehearse.yaml down -v
```
