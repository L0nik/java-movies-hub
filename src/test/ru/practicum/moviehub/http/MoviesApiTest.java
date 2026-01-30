package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static MoviesStore moviesStore;
    private static HttpClient client;
    private static Gson gson;
    private static final String CT_JSON = "application/json; charset=UTF-8";

    @BeforeAll
    static void beforeAll() {
        moviesStore = new MoviesStore();
        gson = new Gson();
        server = new MoviesServer(moviesStore, 8080);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.reset();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    void checkResponseContentType(HttpResponse resp) {
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkResponseContentType(resp);
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsArrayOfMovies() throws Exception {

        moviesStore.addMovie("test movie", 2000);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkResponseContentType(resp);
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String body = resp.body().trim();

        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());

        assertEquals(1, movies.size());
    }

    @Test
    void postMovies_everythingIsOk_returnsNewMovie() throws Exception {
        Movie movie = new Movie("test movie", 2000, 0);
        String json = gson.toJson(movie);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponseContentType(resp);
        assertEquals(201, resp.statusCode(), "Если все корректно, должен вернуть код 201");
        String body = resp.body();
        Movie newMovie = gson.fromJson(body, Movie.class);
        assertEquals(movie, newMovie);
    }

    @Test
    void postMovies_emptyTitle_returnsErrorResponse() throws Exception {
        Movie movie = new Movie("", 2000, 0);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponseContentType(resp);
        assertEquals(422, resp.statusCode(), "При пустом title должен вернуть 422");
        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", errorResponse.getError());
        assertNotNull(errorResponse.getDetails());
        assertTrue(errorResponse.getDetails().contains("Название не должно быть пустым"));

    }

    @Test
    void postMovies_tooLongTitle_returnsErrorResponse() throws Exception {
        Movie movie = new Movie("test movie".repeat(11), 2000, 0);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponseContentType(resp);
        assertEquals(422, resp.statusCode(), "При длине title более 100 символов должен вернуть 422");
        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", errorResponse.getError());
        assertNotNull(errorResponse.getDetails());
        assertTrue(errorResponse.getDetails().contains("Длина названия превышает 100 символов"));
    }

    @Test
    void postMovies_yearBefore1888_returnsErrorResponse() throws Exception {
        Movie movie = new Movie("test movie", 1887, 0);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponseContentType(resp);
        assertEquals(422, resp.statusCode(), "Если год меньше 1888, должен вернуть 422");
        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", errorResponse.getError());
        assertNotNull(errorResponse.getDetails());
        assertTrue(errorResponse.getDetails().contains("Год выхода фильма не может быть меньше 1888"));
    }

    @Test
    void postMovies_yearExceedsCurrentYearBy2_returnsErrorResponse() throws Exception {
        Movie movie = new Movie("test movie", LocalDate.now().getYear() + 2, 0);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponseContentType(resp);
        assertEquals(422, resp.statusCode(), "Если год больше, чем текущий год + 1, должен вернуть 422");
        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", errorResponse.getError());
        assertNotNull(errorResponse.getDetails());
        assertTrue(errorResponse.getDetails().contains("Год выхода фильма не может больше текущего года + 1"));
    }

    @Test
    void postMovies_wrongContentType_returnsStatus415() throws Exception {
        Movie movie = new Movie("test movie", 2000, 0);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponseContentType(resp);
        assertEquals(415, resp.statusCode(), "При некорректном content-type должен вернуть 415");
    }

    @Test
    void getMovieById_everythingIsOk_returnsMovie() throws Exception {
        moviesStore.addMovie("test movie 0", 2000);
        moviesStore.addMovie("test movie 1", 2001);
        moviesStore.addMovie("test movie 2", 2002);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkResponseContentType(resp);
        assertEquals(200, resp.statusCode(), "GET /movies/1 должен вернуть 200");

        String body = resp.body().trim();
        Movie movie = gson.fromJson(body, Movie.class);
        assertEquals(1, movie.getId());
    }

    @Test
    void getMovieById_movieNotFound_returns404() throws Exception {
        moviesStore.addMovie("test movie 0", 2000);
        moviesStore.addMovie("test movie 1", 2001);
        moviesStore.addMovie("test movie 2", 2002);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkResponseContentType(resp);
        assertEquals(404, resp.statusCode(), "GET /movies/999 должен вернуть 404");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);
        assertEquals("Фильм не найден", errorResponse.getError());
    }

    @Test
    void getMovieById_idNotANumber_returns400() throws Exception {
        moviesStore.addMovie("test movie 0", 2000);
        moviesStore.addMovie("test movie 1", 2001);
        moviesStore.addMovie("test movie 2", 2002);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkResponseContentType(resp);
        assertEquals(400, resp.statusCode(), "GET /movies/abc должен вернуть 400");

        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);
        assertEquals("Некорректный ID", errorResponse.getError());
    }

    @Test
    void deleteMovieById_movieNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/999"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkResponseContentType(resp);
        assertEquals(404, resp.statusCode(), "DELETE movies/999 должен вернуть 404");
        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);
        assertEquals("Фильм не найден", errorResponse.getError());
    }

    @Test
    void deleteMovieById_idNotANumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/abc"))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        checkResponseContentType(resp);
        assertEquals(400, resp.statusCode());
        String body = resp.body().trim();
        ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);
        assertEquals("Некорректный ID", errorResponse.getError());
    }

    @Test
    void deleteMovieById_everythingIsOk_deletesMovie() throws Exception {
        moviesStore.addMovie("test movie 0", 2000);
        moviesStore.addMovie("test movie 1", 2001);
        moviesStore.addMovie("test movie 2", 2002);

        int idToDelete = 1;
        Optional<Movie> movieBeforeDeleteOpt = moviesStore.getMovieById(idToDelete);
        assertTrue(movieBeforeDeleteOpt.isPresent());

        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/" + idToDelete))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        checkResponseContentType(resp);
        assertEquals(204, resp.statusCode());

        Optional<Movie> movieAfterDeleteOpt = moviesStore.getMovieById(idToDelete);
        assertTrue(movieAfterDeleteOpt.isEmpty());
    }
}