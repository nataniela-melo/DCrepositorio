
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherInformationApp extends JFrame {

    private final JTextField cityField = new JTextField(18);
    private final JButton searchButton = new JButton("Search");
    private final JComboBox<String> temperatureUnits =
            new JComboBox<>(new String[]{"Celsius (°C)", "Fahrenheit (°F)"});
    private final JComboBox<String> windUnits =
            new JComboBox<>(new String[]{"km/h", "mph"});

    private final JLabel locationLabel = new JLabel("Enter a city to begin");
    private final JLabel iconLabel = new JLabel("🌤️");
    private final JLabel temperatureLabel = new JLabel("--");
    private final JLabel conditionLabel = new JLabel("Weather conditions");
    private final JLabel feelsLikeLabel = new JLabel("Feels like: --");
    private final JLabel humidityLabel = new JLabel("Humidity: --");
    private final JLabel windLabel = new JLabel("Wind: --");
    private final JLabel precipitationLabel = new JLabel("Precipitation: --");

    private final JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
    private final JPanel weatherPanel = new JPanel(new GridLayout(0, 1, 5, 5));
    private final JPanel forecastPanel = new JPanel(new GridLayout(1, 3, 10, 10));
    private final JPanel historyPanel = new JPanel(new GridLayout(0, 1, 3, 3));

    private final List<SearchRecord> history = new ArrayList<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    private WeatherData currentWeather;
    private String currentLocation = "";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WeatherInformationApp() {
        super("Weather Information App");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 700);
        setLocationRelativeTo(null);

        buildInterface();

        searchButton.addActionListener(e -> searchWeather());

        cityField.addActionListener(e -> searchWeather());

        temperatureUnits.addActionListener(e -> refreshDisplayedWeather());
        windUnits.addActionListener(e -> refreshDisplayedWeather());
    }

    private void buildInterface() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(225, 239, 250));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("City:"));
        searchPanel.add(cityField);
        searchPanel.add(searchButton);
        searchPanel.add(temperatureUnits);
        searchPanel.add(windUnits);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(searchPanel, BorderLayout.NORTH);

        locationLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        locationLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(locationLabel, BorderLayout.SOUTH);

        weatherPanel.setOpaque(false);
        weatherPanel.setBorder(BorderFactory.createTitledBorder("Current Weather"));

        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 45));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        temperatureLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        temperatureLabel.setHorizontalAlignment(SwingConstants.CENTER);

        conditionLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        conditionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        weatherPanel.add(iconLabel);
        weatherPanel.add(temperatureLabel);
        weatherPanel.add(conditionLabel);
        weatherPanel.add(feelsLikeLabel);
        weatherPanel.add(humidityLabel);
        weatherPanel.add(windLabel);
        weatherPanel.add(precipitationLabel);

        forecastPanel.setOpaque(false);
        forecastPanel.setBorder(BorderFactory.createTitledBorder("Three-Day Forecast"));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(weatherPanel, BorderLayout.NORTH);
        centerPanel.add(forecastPanel, BorderLayout.CENTER);

        historyPanel.setOpaque(false);
        historyPanel.setBorder(BorderFactory.createTitledBorder("Recent Searches"));

        JScrollPane historyScroll = new JScrollPane(historyPanel);
        historyScroll.setPreferredSize(new java.awt.Dimension(270, 300));
        historyScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel historyContainer = new JPanel(new BorderLayout());
        historyContainer.setOpaque(false);
        historyContainer.add(historyScroll, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(historyContainer, BorderLayout.EAST);

        setContentPane(mainPanel);
    }

    private void searchWeather() {
        String city = cityField.getText().trim();

        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a city name.",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        searchButton.setEnabled(false);
        searchButton.setText("Loading...");

        new SwingWorker<WeatherData, Void>() {

            @Override
            protected WeatherData doInBackground() throws Exception {
                JSONObject location = findLocation(city);

                if (location == null) {
                    throw new IllegalArgumentException(
                            "Location not found. Please check the city name."
                    );
                }

                double latitude = location.getDouble("latitude");
                double longitude = location.getDouble("longitude");
                String resolvedName = location.optString("name", city);
                String country = location.optString("country", "");

                JSONObject forecast = fetchForecast(latitude, longitude);

                return parseWeather(
                        forecast,
                        resolvedName,
                        country,
                        latitude,
                        longitude
                );
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);
                searchButton.setText("Search");

                try {
                    currentWeather = get();
                    currentLocation = currentWeather.location;

                    addHistory(currentLocation);
                    refreshDisplayedWeather();

                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null
                            ? ex.getCause() : ex;

                    String message = cause.getMessage();

                    if (message == null || message.isBlank()) {
                        message = "Unable to retrieve weather information.";
                    }

                    JOptionPane.showMessageDialog(
                            WeatherInformationApp.this,
                            message,
                            "Weather Request Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private JSONObject findLocation(String city) throws Exception {
        String encodedCity = URLEncoder.encode(
                city, StandardCharsets.UTF_8
        );

        String url = "https://geocoding-api.open-meteo.com/v1/search"
                + "?name=" + encodedCity
                + "&count=1&language=en&format=json";

        JSONObject response = getJson(url);
        JSONArray results = response.optJSONArray("results");

        if (results == null || results.isEmpty()) {
            return null;
        }

        return results.getJSONObject(0);
    }

    private JSONObject fetchForecast(double latitude, double longitude)
            throws Exception {

        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,relative_humidity_2m,"
                + "apparent_temperature,is_day,precipitation,"
                + "weather_code,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,"
                + "temperature_2m_min"
                + "&forecast_days=3"
                + "&timezone=auto";

        return getJson(url);
    }

    private JSONObject getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "The weather service returned HTTP "
                            + response.statusCode() + ". Please try again."
            );
        }

        return new JSONObject(response.body());
    }

    private WeatherData parseWeather(
            JSONObject response,
            String city,
            String country,
            double latitude,
            double longitude) {

        JSONObject current = response.getJSONObject("current");
        JSONObject daily = response.getJSONObject("daily");

        String location = country.isBlank()
                ? city : city + ", " + country;

        WeatherData data = new WeatherData();

        data.location = location;
        data.latitude = latitude;
        data.longitude = longitude;
        data.temperature = current.getDouble("temperature_2m");
        data.feelsLike = current.getDouble("apparent_temperature");
        data.humidity = current.getInt("relative_humidity_2m");
        data.windSpeed = current.getDouble("wind_speed_10m");
        data.precipitation = current.getDouble("precipitation");
        data.weatherCode = current.getInt("weather_code");
        data.isDay = current.optInt("is_day", 1) == 1;

        JSONArray dates = daily.getJSONArray("time");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray maxTemps = daily.getJSONArray("temperature_2m_max");
        JSONArray minTemps = daily.getJSONArray("temperature_2m_min");

        for (int i = 0; i < dates.length(); i++) {
            data.forecast.add(new ForecastDay(
                    dates.getString(i),
                    codes.getInt(i),
                    maxTemps.getDouble(i),
                    minTemps.getDouble(i)
            ));
        }

        return data;
    }

    private void refreshDisplayedWeather() {
        if (currentWeather == null) {
            return;
        }

        WeatherData data = currentWeather;
        boolean fahrenheit = temperatureUnits.getSelectedIndex() == 1;
        boolean mph = windUnits.getSelectedIndex() == 1;

        locationLabel.setText(data.location);

        iconLabel.setText(weatherIcon(data.weatherCode, data.isDay));
        conditionLabel.setText(weatherDescription(data.weatherCode));

        temperatureLabel.setText(
                String.format(
                        Locale.US,
                        "%.1f °%s",
                        convertTemperature(data.temperature, fahrenheit),
                        fahrenheit ? "F" : "C"
                )
        );

        feelsLikeLabel.setText(String.format(
                Locale.US,
                "Feels like: %.1f °%s",
                convertTemperature(data.feelsLike, fahrenheit),
                fahrenheit ? "F" : "C"
        ));

        humidityLabel.setText("Humidity: " + data.humidity + "%");

        double wind = mph ? data.windSpeed * 0.621371 : data.windSpeed;

        windLabel.setText(String.format(
                Locale.US,
                "Wind: %.1f %s",
                wind,
                mph ? "mph" : "km/h"
        ));

        precipitationLabel.setText(String.format(
                Locale.US,
                "Precipitation: %.1f mm",
                data.precipitation
        ));

        // Update the interface theme using the API daylight indicator.
        if (data.isDay) {
            mainPanel.setBackground(new Color(225, 239, 250));
        } else {
            mainPanel.setBackground(new Color(45, 55, 80));
        }

        Color foreground = data.isDay ? Color.BLACK : Color.WHITE;
        setForegroundRecursively(mainPanel, foreground);

        displayForecast(data, fahrenheit);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void displayForecast(WeatherData data, boolean fahrenheit) {
        forecastPanel.removeAll();

        for (ForecastDay day : data.forecast) {
            JPanel dayPanel = new JPanel(new GridLayout(0, 1, 4, 4));
            dayPanel.setOpaque(false);
            dayPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(8, 5, 8, 5)
            ));

            JLabel date = new JLabel(day.date, SwingConstants.CENTER);
            JLabel icon = new JLabel(
                    weatherIcon(day.weatherCode, true),
                    SwingConstants.CENTER
            );
            JLabel condition = new JLabel(
                    weatherDescription(day.weatherCode),
                    SwingConstants.CENTER
            );

            JLabel high = new JLabel(String.format(
                    Locale.US,
                    "High: %.1f°",
                    convertTemperature(day.maxTemperature, fahrenheit)
            ), SwingConstants.CENTER);

            JLabel low = new JLabel(String.format(
                    Locale.US,
                    "Low: %.1f°",
                    convertTemperature(day.minTemperature, fahrenheit)
            ), SwingConstants.CENTER);

            dayPanel.add(date);
            dayPanel.add(icon);
            dayPanel.add(condition);
            dayPanel.add(high);
            dayPanel.add(low);

            forecastPanel.add(dayPanel);
            setForegroundRecursively(dayPanel,
                    data.isDay ? Color.BLACK : Color.WHITE);
        }

        forecastPanel.revalidate();
        forecastPanel.repaint();
    }

    private void addHistory(String location) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);

        history.add(0, new SearchRecord(location, timestamp));

        // Keep only the ten most recent successful searches.
        while (history.size() > 10) {
            history.remove(history.size() - 1);
        }

        historyPanel.removeAll();

        for (SearchRecord record : history) {
            JLabel entry = new JLabel(
                    "<html>" + record.location + "<br>"
                            + record.timestamp + "</html>"
            );
            entry.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            historyPanel.add(entry);
        }

        historyPanel.revalidate();
        historyPanel.repaint();
    }

    private double convertTemperature(double celsius, boolean fahrenheit) {
        return fahrenheit ? (celsius * 9.0 / 5.0) + 32.0 : celsius;
    }

    private String weatherIcon(int code, boolean isDay) {
        if (code == 0) {
            return isDay ? "☀️" : "🌙";
        }

        if (code == 1 || code == 2) {
            return isDay ? "🌤️" : "☁️";
        }

        if (code == 3) {
            return "☁️";
        }

        if (code == 45 || code == 48) {
            return "🌫️";
        }

        if (code >= 51 && code <= 67) {
            return "🌧️";
        }

        if (code >= 71 && code <= 77) {
            return "❄️";
        }

        if (code >= 80 && code <= 82) {
            return "🌦️";
        }

        if (code >= 85 && code <= 86) {
            return "🌨️";
        }

        if (code >= 95) {
            return "⛈️";
        }

        return "🌡️";
    }

    private String weatherDescription(int code) {
        switch (code) {
            case 0:
                return "Clear sky";
            case 1:
                return "Mainly clear";
            case 2:
                return "Partly cloudy";
            case 3:
                return "Overcast";
            case 45:
            case 48:
                return "Fog";
            case 51:
                return "Light drizzle";
            case 53:
                return "Moderate drizzle";
            case 55:
                return "Dense drizzle";
            case 56:
            case 57:
                return "Freezing drizzle";
            case 61:
                return "Slight rain";
            case 63:
                return "Moderate rain";
            case 65:
                return "Heavy rain";
            case 66:
            case 67:
                return "Freezing rain";
            case 71:
                return "Slight snowfall";
            case 73:
                return "Moderate snowfall";
            case 75:
                return "Heavy snowfall";
            case 77:
                return "Snow grains";
            case 80:
                return "Slight rain showers";
            case 81:
                return "Moderate rain showers";
            case 82:
                return "Violent rain showers";
            case 85:
            case 86:
                return "Snow showers";
            case 95:
                return "Thunderstorm";
            case 96:
            case 99:
                return "Thunderstorm with hail";
            default:
                return "Unknown conditions";
        }
    }

    private void setForegroundRecursively(
            java.awt.Component component, Color color) {

        if (component instanceof JLabel
                || component instanceof JPanel) {
            component.setForeground(color);
        }

        if (component instanceof JPanel) {
            for (java.awt.Component child
                    : ((JPanel) component).getComponents()) {
                setForegroundRecursively(child, color);
            }
        }
    }

    private static class WeatherData {
        String location;
        double latitude;
        double longitude;
        double temperature;
        double feelsLike;
        double windSpeed;
        double precipitation;
        int humidity;
        int weatherCode;
        boolean isDay;
        List<ForecastDay> forecast = new ArrayList<>();
    }

    private static class ForecastDay {
        String date;
        int weatherCode;
        double maxTemperature;
        double minTemperature;

        ForecastDay(
                String date,
                int weatherCode,
                double maxTemperature,
                double minTemperature) {

            this.date = date;
            this.weatherCode = weatherCode;
            this.maxTemperature = maxTemperature;
            this.minTemperature = minTemperature;
        }
    }

    private static class SearchRecord {
        String location;
        String timestamp;

        SearchRecord(String location, String timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            WeatherInformationApp app = new WeatherInformationApp();
            app.setVisible(true);
        });
    }
}
