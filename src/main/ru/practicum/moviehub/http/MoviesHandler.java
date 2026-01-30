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
        String[] splitStrings = path.split("/");
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            if (splitStrings.length <= 2) {
                String json = gson.toJson(this.moviesStore.getMovies());
                sendJson(ex, 200, json);
            } else {
                String stringId = splitStrings[2];
                try {
                    int id = Integer.parseInt(stringId);
                    Optional<Movie> movieOpt = this.moviesStore.getMovieById(id);
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
        } else if (method.equalsIgnoreCase("DELETE")) {
            String stringId = splitStrings[2];
            try {
                int id = Integer.parseInt(stringId);
                if (this.moviesStore.deleteMovieById(id)) {
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
    }

    private static boolean requestHasCorrectContentType(HttpExchange ex) {
        Headers headers = ex.getRequestHeaders();
        List<String> headerValues = headers.get("Content-Type");
        return headerValues != null && headerValues.contains(CT_JSON);
    }
}
