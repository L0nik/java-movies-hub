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
import java.time.Period;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore moviesStore;
    private final Gson gson;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            String json = gson.toJson(this.moviesStore.getMovies());
            sendJson(ex, 200, json);
        } else if (method.equalsIgnoreCase("POST")) {
            if (!requestHasCorrectContentType(ex))
                sendJson(ex, 415, "");
            InputStream inputStream = ex.getRequestBody();
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Movie movie = gson.fromJson(body, Movie.class);
            List<String> validationErrors = movie.validate();
            if (!validationErrors.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", validationErrors);
                sendJson(ex, 422, gson.toJson(errorResponse));
            }
            Movie newMovie = moviesStore.addMovie(movie.getTitle(), movie.getYear());
            sendJson(ex, 201, gson.toJson(newMovie));
        }
    }

    private static boolean requestHasCorrectContentType(HttpExchange ex) {
        Headers headers = ex.getRequestHeaders();
        List<String> headerValues = headers.get("Content-Type");
        return headerValues != null && headerValues.contains(CT_JSON);
    }
}
