package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MoviesStore {
    private final List<Movie> movies;
    private static int lastId = 0;

    public MoviesStore() {
        this.movies = new ArrayList<>();
    }

    public void reset() {
        this.movies.clear();
        lastId = 0;
    }

    public List<Movie> getMovies() {
        return this.movies;
    }

    public Movie addMovie(String title, int year) {
        Movie movie = new Movie(title, year, lastId);
        lastId++;
        this.movies.add(movie);
        return movie;
    }
}