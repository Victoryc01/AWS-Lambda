package playkosmos.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import playkosmos.dao.UserDAO;
import playkosmos.dbutil.DatabaseConnectionManager;
import playkosmos.entity.User;
import playkosmos.utils.LocalDateTypeAdapter;
import playkosmos.utils.SecretsManagerHelper;
import playkosmos.utils.ValidationResult;
import playkosmos.utils.ValidationUtils;
import software.amazon.awssdk.regions.Region;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
public class UserRegistrationHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SecretsManagerHelper secretsManagerHelper;
    private final Gson gson;

    public UserRegistrationHandler() {
        Region region = Region.of(System.getenv("REGION"));
        String secretName = System.getenv("SECRET_NAME");



        this.secretsManagerHelper = new SecretsManagerHelper(String.valueOf(region), secretName);

        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                .create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        LambdaLogger logger = context.getLogger();

        User user = gson.fromJson(requestEvent.getBody(), User.class);

        CompletableFuture<ValidationResult> usernameValid = ValidationUtils.validateUsername(user.getUsername());
        CompletableFuture<ValidationResult> emailValid = ValidationUtils.validateEmail(user.getEmail());
        CompletableFuture<ValidationResult> phoneNumberValid = ValidationUtils.validatePhoneNumber(user.getPhoneNumber());
        CompletableFuture<ValidationResult> dobValid = ValidationUtils.validateDateOfBirth(user.getDOB());
        CompletableFuture<ValidationResult> passwordValid = ValidationUtils.validatePassword(user.getPassword());

        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(usernameValid, emailValid, phoneNumberValid, dobValid, passwordValid);
            allOf.get();

            StringBuilder validationErrors = new StringBuilder();

            if (!usernameValid.get().isValid()) {
                validationErrors.append(usernameValid.get().getMessage());
            }

            if (!dobValid.get().isValid()) {
                validationErrors.append(dobValid.get().getMessage());
            }
            if (!passwordValid.get().isValid()) {
                validationErrors.append(passwordValid.get().getMessage());
            }

            if (user.getEmail() != null && !emailValid.get().isValid()){
                validationErrors.append(emailValid.get().getMessage());
            } else if (user.getPhoneNumber() != null && !phoneNumberValid.get().isValid()) {
                validationErrors.append(phoneNumberValid.get().getMessage());
            }

            logger.log(validationErrors.toString());

            if (!validationErrors.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody(gson.toJson(Map.of("status", "error", "message", validationErrors.toString().trim())));
            }

            String secret = secretsManagerHelper.getSecret();
            Map<String, Object> secretMap = gson.fromJson(secret, Map.class);

            DatabaseConnectionManager dcm = new DatabaseConnectionManager(secretMap);
            UserDAO userDAO = new UserDAO(dcm);
            userDAO.saveUserToDatabase(user);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(Map.of("status", "success")));
        } catch (InterruptedException | ExecutionException e) {
            logger.log("Error saving user: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
        }
    }
}