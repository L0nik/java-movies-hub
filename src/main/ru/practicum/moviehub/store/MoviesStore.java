package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

public class MoviesStore {
    private final Map<Integer, Movie> movies;
    private int lastId = 0;

    public MoviesStore() {
        movies = new HashMap<>();
    }

    public void reset() {
        movies.clear();
        lastId = 0;
    }

    public List<Movie> getMovies() {

        return movies.values().stream().toList();
    }

    public Optional<Movie> getMovieById(int id) {
        Movie movie = movies.get(id);
        return movie == null ? Optional.empty() : Optional.of(movie);
    }

    public boolean deleteMovieById(int id) {
        return movies.remove(id) != null;
    }

    public List<Movie> getMoviesByYear(int year) {
        return movies.values().stream()
                .filter((movie) -> movie.getYear() == year)
                .toList();
    }

    public Movie addMovie(String title, int year) {
        Movie movie = new Movie(title, year, lastId);
        movies.put(lastId, movie);
        lastId++;
        return movie;
    }
}