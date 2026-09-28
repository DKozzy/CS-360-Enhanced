# CS-360 Enhanced WeighPoint

This repository contains the enhanced version of my CS-360 WeighPoint application, completed as part of my CS-499 Computer Science Capstone at Southern New Hampshire University.

WeighPoint is an Android weight-tracking application originally developed to allow users to create an account, record daily weight entries, establish a goal weight, and monitor their progress. For my capstone, I expanded the application with weight-history analytics and data-processing features while preserving the functionality of the original application.

## About the Original Project

The original WeighPoint application was developed in Android Studio using Java and SQLite. The application allows users to maintain individual accounts, record and manage daily weight entries, establish a goal weight, and receive an SMS notification when their goal is reached.

Weight information is stored locally in a SQLite database and associated with the appropriate user account.

## Capstone Enhancement

For the Algorithms and Data Structures category of my CS-499 ePortfolio, I enhanced WeighPoint by introducing additional algorithms for organizing, filtering, and analyzing a user's weight history.

Major enhancements include:

- Added a `WeightEntry` model for structured weight-history data
- Added sorting by newest, oldest, lowest, and highest weight
- Added date-range filtering for 7-day, 30-day, 90-day, and all-time views
- Added minimum, maximum, and average weight calculations
- Added overall weight-change calculations
- Added a moving average based on recent recorded weight entries
- Added weight trend analysis for gaining, losing, or stable trends
- Added an analytics interface to display calculated weight information
- Expanded automated testing for analytics and date-based filtering
- Preserved compatibility with the application's existing database and weight-management functionality

## Algorithms and Data Structures

The enhancement uses structured `WeightEntry` objects to represent individual weight records rather than relying exclusively on raw database results. Collections of these objects can then be processed by the application's analytics functionality.

Sorting algorithms organize weight history according to the user's selected display preference. Date-based filtering reduces the dataset to entries within a selected period, while aggregation calculations determine statistics such as minimum, maximum, average, and overall weight change.

The application also calculates a moving average using recent recorded entries and evaluates changes in weight to identify whether the current trend is gaining, losing, or stable.

These enhancements separate analytical processing from database access and user-interface logic, improving the organization, maintainability, and testability of the application.

## Testing

The enhanced project includes automated tests covering the weight analytics functionality. Testing verifies calculations, sorting behavior, date filtering, moving averages, trend analysis, and boundary conditions.

Regression testing was also used to ensure that the enhanced analytics functionality continued to work alongside the application's existing features.

## Technologies

- Java
- Android Studio
- SQLite
- Android SDK
- JUnit
- Gradle

## Skills Demonstrated

This enhancement demonstrates skills in:

- Algorithms and data structures
- Object-oriented programming
- Data sorting and filtering
- Data aggregation and statistical calculations
- Date-based data processing
- Moving-average algorithms
- Trend analysis
- SQLite database integration
- Android application development
- Unit and regression testing
- Backward-compatible software enhancement

## CS-499 ePortfolio

This project represents the **Algorithms and Data Structures** enhancement for my CS-499 Computer Science Capstone ePortfolio.

The enhancement demonstrates my ability to select and implement data structures and algorithms that transform stored application data into useful information for the user. It also demonstrates the integration of algorithmic processing with an existing Android application while maintaining the application's original functionality.
