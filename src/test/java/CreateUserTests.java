import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import java.util.UUID;

public class CreateUserTests {

    private static String existingUserEmail = "test-data@yandex.ru";
    private static String existingUserPassword = "password";
    private static String accessToken;


    @BeforeClass
    public static void setup() {
        baseURI = "https://stellarburgers.nomoreparties.site/api";

        String requestBody = "{\n" +
                "\"email\": \"" + existingUserEmail + "\",\n" +
                "\"password\": \"" + existingUserPassword + "\",\n" +
                "\"name\": \"TestUser\"\n" +
                "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth/register");

        // Получаем токен после регистрации
        fetchAccessToken();
    }
    @Step("Получение accessToken для удаления тестового пользователя")
    private static void fetchAccessToken() {
        String loginRequestBody = "{\n" +
                "\"email\": \"" + existingUserEmail + "\",\n" +
                "\"password\": \"" + existingUserPassword + "\"\n" +
                "}";

        accessToken = given()
                .contentType(ContentType.JSON)
                .body(loginRequestBody)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("accessToken");
    }

    @Step("Delete test user")
    private static void deleteTestUser(String token) {
        given()
                .header("Authorization", token)
                .when()
                .delete("/auth/user")
                .then()
                .statusCode(202);
    }

    @Test
    @Description("Test creating a new user with unique data")
    public void createUniqueUser() {
        String uniqueEmail = "test-" + UUID.randomUUID() + "@yandex.ru"; // Генерация уникального email

        String requestBody = "{\n" +
                "\"email\": \"" + uniqueEmail + "\",\n" +
                "\"password\": \"password\",\n" +
                "\"name\": \"Username\"\n" +
                "}";

        given()
                .log().all()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth/register")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(uniqueEmail));
    }

    @Test
    @Description("Test creating a user who already exists")
    public void createExistingUser() {
        String requestBody = "{\n" +
                "\"email\": \"" + existingUserEmail + "\",\n" +
                "\"password\": \"" + existingUserPassword + "\",\n" +
                "\"name\": \"Username\"\n" +
                "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }

    @Test
    @Description("Test creating a user without a required field")
    public void createUserWithoutRequiredField() {
        String requestBody = "{\n" +
                "\"email\": \"test-data@yandex.ru\",\n" +
                "\"password\": \"password\"\n" +
                "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }

    @AfterClass
    public static void cleanup() {
        if (accessToken != null) {
            deleteTestUser(accessToken);
        }
    }


}
