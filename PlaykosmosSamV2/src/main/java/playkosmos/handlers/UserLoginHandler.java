package playkosmos.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import playkosmos.dao.UserDAO;
import playkosmos.dbutil.DatabaseConnectionManager;
import playkosmos.entity.User;
import playkosmos.security.JWTProvider;
import playkosmos.utils.SecretsManagerHelper;
import software.amazon.awssdk.regions.Region;

import java.util.Map;


public class UserLoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private final JWTProvider jwtProvider = new JWTProvider();
    private final SecretsManagerHelper secretsManagerHelper;

    public UserLoginHandler() {

        Region region = Region.of(System.getenv("REGION"));
        String secretName = System.getenv("SECRET_NAME");


        this.secretsManagerHelper = new SecretsManagerHelper(String.valueOf(region), secretName);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

        LambdaLogger logger = context.getLogger();

        Map<String, String> requestBody = gson.fromJson(requestEvent.getBody(), Map.class);
        String userInfo = requestBody.get("userInfo");
        String password = requestBody.get("password");

        String secret = secretsManagerHelper.getSecret();
        Map<String, Object> secretMap = gson.fromJson(secret, Map.class);

        DatabaseConnectionManager dcm = new DatabaseConnectionManager(secretMap);
        UserDAO userDAO = new UserDAO(dcm);
        User user = userDAO.loginUser(userInfo, password);

        if (user == null) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody(gson.toJson(Map.of("status", "error", "message", "Invalid email/phone number or password")));
        }

        String token = jwtProvider.generateJwtToken(user.getUsername());
        logger.log(token);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(gson.toJson(Map.of("status", "success", "token", token)));
    }


}
