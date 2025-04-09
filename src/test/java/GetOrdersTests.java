import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.BeforeClass;
import org.junit.Test;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class GetOrdersTests {

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
    @Description("Test getting orders for an authorized user")
    public void getOrdersForAuthorizedUser() {
        // Используем настоящий токен для авторизации
        String authToken = "Bearer " + accessToken; // Используем accessToken, полученный при логине

        Response response = given()
                .header("Authorization", authToken)
                .when()
                .get("/orders");

        response.prettyPrint(); // Выводим ответ сервера для анализа

        response.then()
                .log().all()
                .statusCode(200) // Ожидаем, что статус код будет 200
                .body("success", equalTo(true)); // Проверяем, что ответ содержит "success": true
    }



@Test
    @Description("Test getting orders for an unauthorized user")
    public void getOrdersForUnauthorizedUser() {
        given()
                .when()
                .get("/orders")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }
}
