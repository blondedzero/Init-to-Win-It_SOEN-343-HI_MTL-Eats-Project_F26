# Init-to-Win-It_SOEN-343_MTL-Eats-Project_F26

~~ Team Members ~~ <br>
Nicholas Chamoun; ID: 40264135, GitHub ID: blondedzero, Role: Team Leader. <br>
Emre Emuler; ID: 40212481, GitHub ID: e-muler, Role: Back-End Developer. <br>
Maëlle Fieffé-Bédard; ID: 40231764, GitHub ID: maellefb, Role: Scrum Master. <br>
Carlos Guevara Martinez; ID: 40227586, GitHub ID: carlos21guevara, Role: Database Specialist. <br>
Nagha Abdoul-Aziz Kone; ID: 40307505, GitHub ID: osrlazlo, Role: Front End Developer. <br>

## How to build and run a demo of the app
1. Pull the code from ```main```
2. Add a .env file in ```src/Backend``` for the Neon database connection. You can add your own or get it from our team
```
PGUSER=[your neon db username]
PGPASSWORD=[your neon db password]
JWT_SECRET=[your generated jwt key]
```
3. Add a .env file in ```src/Frontend``` for the Google Maps API key.
```
VITE_GOOGLE_MAPS_API_KEY=[ypur gppgle maps api key]
```
4. Navigate to folder ```src/``` that contains ```Frontend/``` and ```Backend/```
5. Open the command line from the folder
6. Run the following command to install the node modules needed for the frontend 
```
npm run install:all
``` 
5. If you're on WindowsOS, run the following command to build and start the app 
```
npm run demo
```
If you're on MacOS, run the following command to build and start the app 
```
npm run demo:mac
```

- This will automatically run 2 commands with ```concurrently```
    - ```npm run dev``` in ```Frontend/``` to build and start the server that serves the frontend on ```localhost:5173```
    - ```mvnw spring-boot:run``` in ```Backend/``` to build and start the backend server on ```localhost:8080```
6. Open your web browser and navigate to [localhost:5173](http://localhost:5173/) to access the app

## Reset simulation
An operator can use **Reset simulation** to restore the state captured when the demo data was first seeded. Existing seed and startup data is preserved. Active orders created after that baseline are canceled, stores added afterward are closed, and couriers added afterward are taken offline.

## Accounts to test the app
These accounts are created by the startup seed (`ApplicationSeeder`). All passwords are `test`.
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

## Run with Docker (SOEN 343 demo platform)
Requires Docker Desktop. The stack is `web` (nginx serving the frontend build and proxying `/api`), `backend` (Spring Boot) and `db` (Postgres 17), and starts with no `.env` file and an empty database that the seed fills.

1. Build the images locally, tagged with the names used in `demo/compose.yaml`:
```
docker build --platform linux/amd64 -t <backend image from demo/compose.yaml> src/Backend
docker build --platform linux/amd64 -t <frontend image from demo/compose.yaml> src/Frontend
```
2. Start the stack and open [localhost:8088](http://localhost:8088/):
```
docker compose -f demo/compose.yaml -f demo/rehearse.yaml up
```
3. Stop it and delete its data with `docker compose -f demo/compose.yaml -f demo/rehearse.yaml down -v`

On every push to `main`, the `Publish demo images` GitHub workflow builds both images and publishes them to GHCR. Its run summary prints the `image:` lines to paste into `demo/compose.yaml` before submitting it on the platform. The Google Maps key is not included in the images, so the map page shows a message instead of the map.
