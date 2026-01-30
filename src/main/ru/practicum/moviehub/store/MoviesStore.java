package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public Optional<Movie> getMovieById(int id) {
        return this.movies.stream()
                .filter((movie) -> movie.getId() == id)
                .findFirst();
    }

    public boolean deleteMovieById(int id) {
        return this.movies.removeIf((movie) -> movie.getId() == id);
    }

    public List<Movie> getMoviesByYear(int year) {
        return this.movies.stream()
                .filter((movie) -> movie.getYear() == year)
                .toList();
    }

    public Movie addMovie(String title, int year) {
        Movie movie = new Movie(title, year, lastId);
        lastId++;
        this.movies.add(movie);
        return movie;
    }
}