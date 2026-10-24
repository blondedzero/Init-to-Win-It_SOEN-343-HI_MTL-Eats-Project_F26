# ShopEasy Package Diagram

```mermaid
flowchart TB
    subgraph src["src"]
        direction TB

        subgraph ui["UI"]
            direction TB
            pages["LoginPage · RegisterPage<br/>TempAccountPage"]
            widgets["CustomerDetails · StoreDetails<br/>CourierDetails"]
            client["api/REG<br/>Login · Register · Auth"]
            pages --> widgets
            pages --> client
        end

        subgraph application["Application"]
            direction TB
            web["AuthController"]
            errors["ApiExceptionHandler"]
            web --- errors
        end

        subgraph domain["Domain"]
            direction TB
            registration["REGModule"]
            auth["AuthTokenUtil · PasswordEncoder"]
            factories["UserFactory<br/>Customer · Store · Courier · Operator"]
            models["Account · GroceryStore · Item<br/>Order · PreparationQueue · EventLog"]
            states["GroceryStoreStates<br/>Open · Paused · Closed"]
            registration --> factories
            registration --> auth
            models --> states
        end

        subgraph foundation["Foundation"]
            direction TB
            entities["entities<br/>User · Customer · GroceryStore<br/>Order · Cart · Product · PaymentInfo"]
            repositories["repositories<br/>User · Order · Product · Cart · …"]
            entities --- repositories
        end

        ui -. "HTTP / JSON" .-> application
        application -. "uses" .-> domain
        application -. "uses entities / repositories" .-> foundation
        domain -. "uses entities / repositories" .-> foundation
    end

    database[("PostgreSQL")]
    foundation -. "JPA persistence" .-> database

    classDef uiStyle fill:#d6e6fa,stroke:#555,color:#111
    classDef appStyle fill:#d7efa0,stroke:#555,color:#111
    classDef domainStyle fill:#fff99a,stroke:#555,color:#111
    classDef foundationStyle fill:#f6d5b8,stroke:#555,color:#111
    classDef databaseStyle fill:#eeeeee,stroke:#555,color:#111
    class pages,widgets,client uiStyle
    class web,errors appStyle
    class registration,auth,factories,models,states domainStyle
    class entities,repositories foundationStyle
    class database databaseStyle
```

The UI box lists the routed React pages and registration details; the API client sends requests to `AuthController`. The `Browse.html`, `ManageAccount.html`, and `Order.html` files exist as static UI files but are not currently wired into the React router. The older `Domain` model classes are shown separately from the persisted `Foundation.entities` classes because both packages currently exist in the codebase.
