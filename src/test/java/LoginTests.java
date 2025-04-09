import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class LoginTests {

    private static String testEmail = "test-data@yandex.ru";
    private static String testPassword = "password";
    private static String accessToken;

    @BeforeClass
    public static void setup() {
        baseURI = "https://stellarburgers.nomoreparties.site/api";
        createTestUser();
    }

    @AfterClass
    public static void cleanup() {
        if (accessToken != null) {
            deleteTestUser(accessToken);
        }
    }

    @Step("Create test user")
    private static void createTestUser() {
        String requestBody = String.format("{\"email\": \"%s\", \"password\": \"%s\", \"name\": \"Test User\"}", testEmail, testPassword);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/auth/register");

        if (response.statusCode() == 200) {
            accessToken = "Bearer " + response.jsonPath().getString("accessToken");
        } else {
            System.out.println("User already exists or error in creation: " + response.getBody().asString());
        }
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

    @Step("Send login request with email: {email} and password: {password}")
    public void loginRequest(String email, String password, int expectedStatus, boolean expectedSuccess, String expectedMessage) {
        Response response = given()
                .log().all()
                .contentType(ContentType.JSON)
                .body(String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password))
                .when()
                .post("/auth/login");

        response.then()
                .log().all()
                .statusCode(expectedStatus)
                .body("success", equalTo(expectedSuccess));

        if (!expectedSuccess) {
            response.then().body("message", equalTo(expectedMessage));
        }
    }

    @Test
    @Description("Test login with existing user")
    public void loginExistingUser() {
        loginRequest(testEmail, testPassword, 200, true, "");
    }

    @Test
    @Description("Test login with invalid credentials")
    public void loginWithInvalidCredentials() {
        loginRequest("wrong-email@yandex.ru", "wrongpassword", 401, false, "email or password are incorrect");
    }
}
