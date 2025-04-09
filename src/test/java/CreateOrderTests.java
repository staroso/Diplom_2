import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class CreateOrderTests {
    private static final String BASE_URL = "https://stellarburgers.nomoreparties.site/api";
    private static final String TEST_EMAIL = "test-data@yandex.ru";
    private static final String TEST_PASSWORD = "password";
    private static String accessToken;
    private static String refreshToken;

    @BeforeClass
    public static void setup() {
        baseURI = BASE_URL;
        // Проверим, существует ли пользователь. Если нет, создадим его.
        loginUser();
    }

    @Step("Login user and get access token")
    private static void loginUser() {
        String requestBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", TEST_EMAIL, TEST_PASSWORD);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth/login");

        System.out.println("Login response: " + response.getBody().asString());

        if (response.statusCode() == 200) {
            accessToken = response.jsonPath().getString("accessToken").replace("Bearer ", "").trim(); // Убираем "Bearer "
            refreshToken = response.jsonPath().getString("refreshToken").trim();
            System.out.println("Access Token: " + accessToken);
            System.out.println("Refresh Token: " + refreshToken);
        } else {
            // Если логин не удался (например, неверные данные), то пытаемся зарегистрировать нового пользователя
            if (response.statusCode() == 401) {
                System.out.println("Login failed: Invalid credentials (email or password incorrect). Trying to register.");
                createTestUser();
                loginUser(); // Попробуем снова залогиниться после создания
            } else {
                throw new RuntimeException("Login failed: " + response.getBody().asString());
            }
        }
    }

    @Step("Create test user")
    private static void createTestUser() {
        String requestBody = String.format("{\"email\": \"%s\", \"password\": \"%s\", \"name\": \"Test User\"}", TEST_EMAIL, TEST_PASSWORD);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth/register");

        System.out.println("Create user response: " + response.getBody().asString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("User registration failed: " + response.getBody().asString());
        } else {
            System.out.println("User successfully created.");
        }
    }


    @Test
    @Description("Create order with valid authorization")
    public void createOrderWithAuthorization() {
        String requestBody = "{\"ingredients\": [\"61c0c5a71d1f82001bdaaa6f\",\"61c0c5a71d1f82001bdaaa6d\"]}";

        given()
                .header("Authorization", "Bearer " + accessToken)  // Используем accessToken
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/orders")
                .then()
                .statusCode(200);
    }

    @Test
    @Description("Создание заказа без авторизации")
    public void createOrderWithoutAuthorization() {
        String requestBody = "{ \"ingredients\": [\"61c0c5a71d1f82001bdaaa6f\", \"61c0c5a71d1f82001bdaaa6d\"] }";

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/orders");

        response.prettyPrint(); // Выводит ответ сервера в консоль

        response.then()
                .log().all()
                .statusCode(200) // <--- Если API требует авторизацию, измени на ожидаемый код (например, 400 или 401)
                .body("success", equalTo(true));
    }
    @Test
    @Description("Create order with valid ingredients")
    public void createOrderWithIngredients() {
        String requestBody = "{\"ingredients\": [\"61c0c5a71d1f82001bdaaa6f\",\"61c0c5a71d1f82001bdaaa6d\"]}";

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/orders");

        response.prettyPrint(); // Выводим ответ сервера для анализа

        response.then()
                .log().all()
                .statusCode(200) // Ожидаем успешный ответ
                .body("success", equalTo(true)); // Проверяем, что ответ успешный
    }

    @Test
    @Description("Create order without ingredients")
    public void createOrderWithoutIngredients() {
        String requestBody = "{\"ingredients\": []}"; // Отправляем пустой список ингредиентов

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/orders");

        response.prettyPrint(); // Выводим ответ сервера для анализа

        response.then()
                .log().all()
                .statusCode(400) // Ожидаем ошибку 400, так как ингредиенты обязательны
                .body("message", equalTo("Ingredient ids must be provided")); // Проверяем сообщение об ошибке
    }

    @Test
    @Description("Create order with invalid ingredient hashes")
    public void createOrderWithInvalidIngredients() {
        // Используем неверные хеши для ингредиентов
        String requestBody = "{\"ingredients\": [\"invalidHash1\", \"invalidHash2\"]}";

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/orders");

        response.prettyPrint(); // Выводим ответ сервера для анализа

        response.then()
                .log().all()
                .statusCode(400) // Ожидаем ошибку 400, так как ингредиенты неверны
                .body("message", equalTo("One or more ids provided are incorrect")); // Проверяем сообщение об ошибке
    }

}