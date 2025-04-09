import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.junit.BeforeClass;
import org.junit.Test;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class GetIngredientsTests {

    @BeforeClass
    @Step("Setup the API base URI")
    public static void setup() {
        baseURI = "https://stellarburgers.nomoreparties.site/api";
    }

    @Test
    @Description("Test getting ingredients")
    public void testGetIngredients() {
        given()
                .when()
                .get("/ingredients")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }
}
