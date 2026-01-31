package ru.practicum.moviehub.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Movie {
    private final String title;
    private final int year;
    private final int id;

    public Movie(String title, int year, int id) {
        this.title = title;
        this.year = year;
        this.id = id;
    }

    public List<String> validate() {
        List<String> result = new ArrayList<>();
        if (title.isEmpty()) {
            result.add("Название не должно быть пустым");
        }
        if (title.length() > 100) {
            result.add("Длина названия превышает 100 символов");
        }
        if (year < 1888) {
            result.add("Год выхода фильма не может быть меньше 1888");
        }
        int currentYear = LocalDate.now().getYear();
        if (year > currentYear + 1) {
            result.add("Год выхода фильма не может больше текущего года + 1");
        }
        return result;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return year == movie.year && id == movie.id && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, year, id);
    }
}