package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore moviesStore;
    private final Gson gson;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();
        Map<String, String> queryParams = parseQueryParams(query);
        String[] pathParts = path.split("/");
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            handleGet(ex, pathParts, queryParams);
        } else if (method.equalsIgnoreCase("POST")) {
            handlePost(ex);
        } else if (method.equalsIgnoreCase("DELETE") && pathParts.length == 3) {
            handleDelete(ex, pathParts);
        } else {
            sendMethodNotAllowed(ex);
        }
    }

    private void handleGet(HttpExchange ex, String[] pathParts, Map<String, String> queryParams) throws IOException {
        if (pathParts.length <= 2) {
            if (queryParams.isEmpty()) {
                String json = gson.toJson(moviesStore.getMovies());
                sendJson(ex, 200, json);
                return;
            }
            String yearString = queryParams.get("year");
            try {
                int year = Integer.parseInt(yearString);
                List<Movie> movies = moviesStore.getMoviesByYear(year);
                sendJson(ex, 200, gson.toJson(movies));
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse(
                        "Некорректный параметр запроса — 'year'",
                        new ArrayList<>()
                );
                sendJson(ex, 400, gson.toJson(errorResponse));
            }
        } else {
            String stringId = pathParts[2];
            try {
                int id = Integer.parseInt(stringId);
                Optional<Movie> movieOpt = moviesStore.getMovieById(id);
                if (movieOpt.isPresent()) {
                    sendJson(ex, 200, gson.toJson(movieOpt.get()));
                } else {
                    ErrorResponse errorResponse = new ErrorResponse("Фильм не найден", new ArrayList<>());
                    sendJson(ex, 404, gson.toJson(errorResponse));
                }
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse("Некорректный ID", new ArrayList<>());
                sendJson(ex, 400, gson.toJson(errorResponse));
            }
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        if (!requestHasCorrectContentType(ex))
            sendJson(ex, 415, "");
        InputStream inputStream = ex.getRequestBody();
        String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        if (body.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Тело запроса не может быть пустым",
                    new ArrayList<>()
            );
            sendJson(ex, 422, gson.toJson(errorResponse));
            return;
        }
        Movie movie = gson.fromJson(body, Movie.class);
        List<String> validationErrors = movie.validate();
        if (!validationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", validationErrors);
            sendJson(ex, 422, gson.toJson(errorResponse));
            return;
        }
        Movie newMovie = moviesStore.addMovie(movie.getTitle(), movie.getYear());
        sendJson(ex, 201, gson.toJson(newMovie));
    }

    private void handleDelete(HttpExchange ex, String[] pathParts) throws IOException {
        String stringId = pathParts[2];
        try {
            int id = Integer.parseInt(stringId);
            if (moviesStore.deleteMovieById(id)) {
                sendNoContent(ex);
            } else {
                ErrorResponse errorResponse = new ErrorResponse("Фильм не найден", new ArrayList<>());
                sendJson(ex, 404, gson.toJson(errorResponse));
            }
        } catch (NumberFormatException e) {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный ID", new ArrayList<>());
            sendJson(ex, 400, gson.toJson(errorResponse));
        }
    }

    private static boolean requestHasCorrectContentType(HttpExchange ex) {
        Headers headers = ex.getRequestHeaders();
        List<String> headerValues = headers.get("Content-Type");
        return headerValues != null && headerValues.contains(CT_JSON);
    }
}
