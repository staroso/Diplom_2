import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class UpdateUserTests {

    private static final String BASE_URL = "https://stellarburgers.nomoreparties.site/api";
    private static final String TEST_EMAIL = "test-data@yandex.ru";
    private static final String TEST_PASSWORD = "password";
    private static String accessToken;

    @BeforeClass
    public static void setup() {
        baseURI = BASE_URL;
        // Авторизация перед тестами
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

        if (response.statusCode() == 200) {
            accessToken = response.jsonPath().getString("accessToken").replace("Bearer ", "").trim();
        } else {
            throw new RuntimeException("Login failed: " + response.getBody().asString());
        }
    }

    @Test
    @Description("Update user data with valid authorization")
    public void updateUserWithAuth() {
        String newName = "Updated User Name";
        String requestBody = "{\n" +
                "\"name\": \"" + newName + "\"\n" +
                "}";

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .patch("/auth/user")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.name", equalTo(newName));
    }

    @Test
    @Description("Try to update user data without authorization")
    public void updateUserWithoutAuth() {
        String newName = "Updated User Name";
        String requestBody = "{\n" +
                "\"name\": \"" + newName + "\"\n" +
                "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .patch("/auth/user")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @Description("Try to update user data with already existing email")
    public void updateUserWithExistingEmail() {
        String existingEmail = "existing-email@yandex.ru";
        String requestBody = "{\n" +
                "\"email\": \"" + existingEmail + "\"\n" +
                "}";

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .patch("/auth/user")
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User with such email already exists"));
    }

    @Test
    @Description("Get user data with valid authorization")
    public void getUserDataWithAuth() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/auth/user")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(TEST_EMAIL));
    }

    @Test
    @Description("Get user data without authorization")
    public void getUserDataWithoutAuth() {
        given()
                .when()
                .get("/auth/user")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }
}