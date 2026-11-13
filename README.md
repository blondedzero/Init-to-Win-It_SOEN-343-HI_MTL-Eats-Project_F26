# Init-to-Win-It_SOEN-343_MTL-Eats-Project_F26

~~ Team Members ~~ <br>
Nicholas Chamoun; ID: 40264135, GitHub ID: blondedzero, Role: Team Leader. <br>
Emre Emuler; ID: 40212481, GitHub ID: e-muler, Role: Back-End Developer. <br>
Maëlle Fieffé-Bédard; ID: 40231764, GitHub ID: maellefb, Role: Scrum Master. <br>
Carlos Guevara Martinez; ID: 40227586, GitHub ID: carlos21guevara, Role: Database Specialist. <br>
Nagha Abdoul-Aziz Kone; ID: 40307505, GitHub ID: osrlazlo, Role: Front End Developer. <br>

## How to build and run a demo of the app
1. Pull the code from ```main```
2. Navigate to folder ```src/``` that contains ```Frontend/``` and ```Backend/```
3. Open the command line from the folder
4. Run the following command to install the node modules needed for the frontend 
```
npm run install:all
``` 
5. If you're on WindowsOS, run the following command to build and start the app 
```
npm run demo
```

5. If you're on MacOS, run the following command to build and start the app 
```
npm run demo:mac
```

- This will automatically run 2 commands with ```concurrently```
    - ```npm run dev``` in ```Frontend/``` to build and start the server that serves the frontend on ```localhost:5173```
    - ```mvnw spring-boot:run``` in ```Backend/``` to build and start the backend server on ```localhost:8080```
6. Open your web browser and navigate to [localhost:5173](http://localhost:5173/) to access the app
