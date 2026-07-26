# FlavorFlow Desktop Project Structure

/src
  /main
    /resources
      /fxml
        - Splash.fxml
        - Login.fxml
        - Dashboard.fxml
        - RestaurantDetails.fxml
        - ShoppingCart.fxml
        - Checkout.fxml
        - OrderConfirmation.fxml
        - OrderTracking.fxml
        - Profile.fxml
        - FeedbackDialog.fxml
      /css
        - global.css
      /assets
        /images
          - logo.png
          - banner_promo.jpg
          - food_bg.jpg
          /icons
            - dashboard_icon.svg
            - order_icon.svg
            - cart_icon.svg
            - profile_icon.svg
    /java
      /com/flavorflow
        /controllers
          - SplashController.java
          - LoginController.java
          - DashboardController.java
          - NavigationManager.java (Main Routing Logic)
        /models
          - User.java
          - Restaurant.java
          - Order.java
