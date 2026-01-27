package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

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
        }
    }
}
