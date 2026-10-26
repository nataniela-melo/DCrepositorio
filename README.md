
# Weather Information App

## Overview

The Weather Information App is a Java Swing desktop application that retrieves current weather conditions and a three-day forecast using the Open-Meteo API.

## Features

- City search using the Open-Meteo Geocoding API.
- Current temperature and apparent temperature.
- Humidity, wind speed, and precipitation information.
- Three-day weather forecast.
- Celsius/Fahrenheit temperature conversion.
- km/h and mph wind speed conversion.
- Weather condition symbols.
- Daytime and nighttime interface themes.
- Recent search history with timestamps.
- Input validation and error handling.

## Requirements

- Java Development Kit (JDK) 17 or later.
- Apache Maven.
- An internet connection.

No personal API key is required for the public Open-Meteo endpoints used by this project.

## Project Structure

```text
weather-information-app/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── WeatherInformationApp.java
```

## Installation and Execution

1. Install JDK 17 or later.
2. Install Apache Maven.
3. Create the project structure shown above.
4. Place `WeatherInformationApp.java` inside `src/main/java/`.
5. Save the Maven configuration as `pom.xml` in the project root.
6. Open a terminal in the project root.
7. Run the application using:

```bash
mvn clean compile exec:java
```

Maven downloads the required JSON dependency and compiles the Java source code.

## How to Use the Application

1. Launch the application.
2. Enter a city name in the search field.
3. Click Search or press Enter.
4. Review the current weather information.
5. Examine the three-day forecast.
6. Change the temperature or wind speed units using the selection controls.
7. Review recent successful searches in the history panel.

## API Information

The application uses these public endpoints:

- Geocoding: https://geocoding-api.open-meteo.com/v1/search
- Weather forecast: https://api.open-meteo.com/v1/forecast

Weather data is retrieved over HTTP using Java's built-in `HttpClient` class. JSON responses are processed using the JSON-java library.

## Error Handling

The application validates empty input and reports unsuccessful location searches, HTTP errors, and network failures. Weather requests are executed using `SwingWorker` so that the interface remains responsive.

## Limitations

Search history is maintained in memory and is not saved between application sessions. The application requires internet access to retrieve weather information. Forecast accuracy depends on the external weather service.

## Author

Student Name: [Enter your name]

Course: [Enter your course name]

Assignment: Weather Information App
