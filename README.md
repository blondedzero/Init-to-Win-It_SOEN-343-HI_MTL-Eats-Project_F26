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
An operator can use **Reset simulation** to restore the state captured when the demo data was first seeded. Existing seed and startup data is preserved: its order, store, courier, and product states are restored. Active orders created after that baseline are canceled, stores added afterward are closed, and couriers added afterward are taken offline. Products added afterward are left unchanged. No accounts, orders, products, or history are deleted.

## Accounts to test the app
<table>
    <tr>
        <th>Email</th>
        <th>Password</th>
        <th>Role</th>
    </tr>
    <tr>
        <td>customer@email.com</td>
        <td>test</td>
        <td>Customer</td>
    </tr>
    <tr>
        <td>store@iga.com</td>
        <td>test</td>
        <td>Grocery Store</td>
    </tr>
    <tr>
        <td>store@superc.com</td>
        <td>superc</td>
        <td>Grocery Store</td>
    </tr>
    <tr>
        <td>courier@email.com</td>
        <td>test</td>
        <td>Courier</td>
    </tr>
    <tr>
        <td>courier2@email.com</td>
        <td>test</td>
        <td>Courier</td>
    </tr>
    <tr>
        <td>courier3@email.com</td>
        <td>test</td>
        <td>Courier</td>
    </tr>
    <tr>
        <td>operator@email.com</td>
        <td>test</td>
        <td>Operator</td>
    </tr>
</table>
