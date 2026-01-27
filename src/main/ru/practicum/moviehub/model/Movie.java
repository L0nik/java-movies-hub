package ru.practicum.moviehub.model;

import java.util.ArrayList;
import java.util.List;

public class Movie {
    private final String title;
    private final int year;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
    }

    public List<String> validate() {
        List<String> result = new ArrayList<>();
        if (this.title.isEmpty()) {
            result.add("Название не должно быть пустым");
        }
        if (this.title.length() > 100) {
            result.add("Длина названия превышает 100 символов");
        }
        if (this.year < 1888) {
            result.add("Год выхода фильма не может быть меньше 1888");
        }
        if (this.year > 2026 + 1) {
            result.add("Год выхода фильма не может больше текущего года + 1");
        }
        return result;
    }
}