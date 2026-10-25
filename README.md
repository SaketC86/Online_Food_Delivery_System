# # FlavorFlow: Online Food Delivery System

A web-based online food delivery system inspired by Swiggy and Zomato. Customers can register, browse restaurants, order food, pay, track their delivery, and leave feedback.


## Features

- **Accounts:** register and log in with email, phone and password; log out.
- **Browse:** restaurant list with search, and category filters that also match menu items.
- **Menu and cart:** view each restaurant's menu, add items, change quantities, remove items.
- **Checkout:** choose a saved delivery address and a payment method (UPI, Card, Cash on Delivery). Each order is saved in one database transaction.
- **Tracking:** order status moves through Placed, Preparing, Out for Delivery and Delivered. A delivery agent is assigned automatically.
- **Feedback:** rate a delivered order from 1 to 5 stars. Restaurant ratings update automatically.
- **Profile:** edit personal details, and add or delete saved addresses.

## Technology

| Layer | Technology |
|---|---|
| Front end | HTML, Tailwind CSS, JavaScript (`fetch`) |
| Back end | Java 17, Jakarta Servlet 6.0 |
| Server | Apache Tomcat 10.1 |
| Database | PostgreSQL 18, JDBC |
| JSON | Gson |
| Build | Maven |

## Project structure

```
food_delivery/
├── pom.xml                      Maven build file
├── sql/schema.sql               Database tables, constraints, indexes and sample data
├── DB Design/                   Database design documents
└── src/main/
    ├── java/com/flavorflow/servlets/
    │   ├── RestaurantServlet    /api/restaurants
    │   ├── MenuServlet          /api/menu
    │   ├── CartServlet          /api/cart
    │   ├── CheckoutServlet      /api/checkout
    │   ├── OrderServlet         /api/order
    │   ├── TrackingServlet      /api/tracking
    │   ├── CustomerServlet      /api/customer
    │   ├── AddressServlet       /api/address
    │   ├── FeedbackServlet      /api/feedback
    │   ├── LoginServlet         /api/login
    │   ├── RegisterServlet      /api/register
    │   ├── LogoutServlet        /api/logout
    │   ├── SessionServlet       /api/session
    │   ├── Auth.java            Reads the logged-in customer from the session
    │   └── Dispatcher.java      Moves orders through stages and assigns agents
    └── webapp/
        ├── splash.html, login.html, index.html, restaurant.html, cart.html,
        │   checkout.html, confirmation.html, tracking.html, feedback.html, profile.html
        ├── global.css
        └── images/              Restaurant photos
```

## Database

The database has eleven tables: `CUSTOMER`, `ADDRESS`, `RESTAURANT`, `MENU_ITEM`, `DELIVERY_AGENT`, `CART`, `CART_ITEM`, `ORDERS`, `ORDER_ITEM`, `PAYMENT` and `FEEDBACK`. The schema is normalized to BCNF. See the design document in `DB Design/` for the ER diagram, relational schema and normalization steps.

## Setup

### Requirements

- Java 17 or newer (JDK)
- Apache Maven
- Apache Tomcat 10.1
- PostgreSQL 18 (with pgAdmin)
