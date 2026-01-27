package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MoviesStore {
    private final List<Movie> movies;

    public MoviesStore() {
        this.movies = new ArrayList<>();
    }

    public List<Movie> getMovies() {
        return this.movies;
    }
}