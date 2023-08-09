package consumer;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import org.apache.commons.lang3.RandomStringUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static helper.SSMExample.authenticateUser;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
//import org.json.JSONObject;

public class agencyAPITest extends Simulation{

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://randpm1234.execute-api.us-east-1.amazonaws.com/development")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    public static LocalDate randomDate() {
        int hundredYears = 100 * 365;
        return LocalDate.ofEpochDay(ThreadLocalRandom.current().nextInt(-hundredYears, hundredYears));
    }
    private static byte[] readImageBytes(String imagePath) {
        try {
            Path path = Path.of(imagePath);
            return Files.readAllBytes(path);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
    private static Iterator<Map<String, Object>> customFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        long randomEle = Instant.now().getEpochSecond();

                        String imagePath = "/Users/akankshasingh/workdir/gatlingJavaAPI/src/test/resources/bodies/pic.png";
                        byte[] imageBytes = readImageBytes(imagePath);

                        // Encode the image as a base64 string
                        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

//                        // Create a JSON payload
//                        JSONObject payload = new JSONObject();
//                        payload.put("imageKey", base64Image);


                        String email = "cypressUniqMail" + randomEle + "@gmail.com";
                        String primaryContactEmail = "cypressPrimary" + randomEle + "@gmail.com";
                        String consumerportalurl = "consumerportalurl" + randomEle;
                        String consName = "tempcons-3-" + randomEle;
                        String businessWebsite = "https://www.c" + randomEle + ".com";

                        HashMap<String, Object> hmap = new HashMap<String, Object>();
                        hmap.put("consName", consName);
                        hmap.put("email", email);
                        hmap.put("primaryContactEmail", primaryContactEmail);
                        hmap.put("businessWebsite", businessWebsite);
                        hmap.put("consumerportalurl",consumerportalurl);
                        hmap.put("fileContent",base64Image);
                        return hmap;
                    }
            ).iterator();

//    private static ChainBuilder authenticate =
//            exec(http("Authenticate")
//                    .post("/authenticate")
//                    .body(StringBody("{\n" +
//                            "  \"password\": \"admin\",\n" +
//                            "  \"username\": \"admin\"\n" +
//                            "}"))
//                    .check(jmesPath("token").saveAs("jwtToken")));


    private static ChainBuilder createNewGame =
            feed(customFeeder)
                    .exec(http("Create New consumer   basic details")
                            .post("/consumer/consumer-draft")
                            .headers(authenticateUser())
                            .body(ElFileBody("bodies/newConsumerTemplate.json")).asJson()
                            .check(bodyString().saveAs("responseBody"))
                            .check(jmesPath("data.draftId").saveAs("draftId"))
                           )
                    .exec(http("Continue with draft consumer   branding")
                            .put("/consumer/consumer-draft/#{draftId}")
                            .headers(authenticateUser())
                            .body(ElFileBody("bodies/brandingConsumerTemplate.json")).asJson()
                            .check(bodyString().saveAs("responseBody"))
                          )
                    .exec(http("Continue with draft consumer   billing")
                            .put("/consumer/consumer-draft/#{draftId}")
                            .headers(authenticateUser())
                            .body(ElFileBody("bodies/billingConsumerTemplate.json")).asJson()
                            .check(bodyString().saveAs("responseBody"))
                          )
                    .exec(http("Continue with draft consumer   License")
                            .put("/consumer/consumer-draft/#{draftId}")
                            .headers(authenticateUser())
                            .body(ElFileBody("bodies/licenseConsumerTemplate.json")).asJson()
                            .check(bodyString().saveAs("responseBody"))
                         )
                    .exec(http("Continue with draft consumer   carrier")
                            .put("/consumer/consumer-draft/#{draftId}")
                            .headers(authenticateUser())
                            .body(ElFileBody("bodies/carrierConsumerTemplate.json")).asJson()
                            .check(bodyString().saveAs("responseBody"))
                         )
                    .exec(http("Continue with draft consumer   errorsAndOmissions")
                            .put("/consumer/consumer-draft/#{draftId}")
                            .headers(authenticateUser())
                            .body(ElFileBody("bodies/errorsAndOmissionsConsumerTemplate.json")).asJson()
                            .check(bodyString().saveAs("responseBody"))
                          )
                    .pause(10)
                    .exec(http("Continue with draft consumer   final")
                            .post("/Consumer")
                            .headers(authenticateUser())
                            .body(StringBody("{\n" +
                                    "  \"emailId\": \"cypresssuperadmin@gmail.com\",\n" +
                                    "  \"draftId\": \"#{draftId}\",\n" +
                                    "  \"loggedInUserName\": \"Cypress User\",\n" +
                                    "  \"loggedInUserRole\": \"Super Admin\"\n" +
                                    "}"))
                            .check(bodyString().saveAs("responseBody")))
                    .exec(session -> {
                        System.out.println(session.getString("responseBody"));
                        return session;
                    });


    private ScenarioBuilder scn = scenario("Consumer Creation")
            .repeat(1).on(
                    exec(createNewGame)
                            .pause(1)
            );

    {
        setUp(
                scn.injectOpen(atOnceUsers(1),
                        nothingFor(2),
//                        constantUsersPerSec(2).during(1),
                        rampUsers(5).during(5)
                        )
        ).protocols(httpProtocol);
    }

}
