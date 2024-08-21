package playkosmos.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import playkosmos.security.JWTProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuthHandler implements RequestHandler<APIGatewayCustomAuthorizerEvent, Map<String, Object>> {

    public final JWTProvider jwtProvider = new JWTProvider();
    @Override
    public Map<String, Object> handleRequest(APIGatewayCustomAuthorizerEvent requestEvent, Context context) {


        LambdaLogger logger = context.getLogger();
        logger.log("Received event: " + requestEvent.toString());


        String authorizationToken = requestEvent.getAuthorizationToken();


        if (authorizationToken == null || !authorizationToken.startsWith("Bearer ")) {
            logger.log("Authorization header missing or invalid");
            return generatePolicy("user", "Deny", requestEvent.getMethodArn(), "Authorization header missing or invalid");
        }

        String token = authorizationToken.substring(7);

        try {
            String username = jwtProvider.validateToken(token);
            logger.log("Token to be validated: " + token);
            if (username == null) {
                throw new RuntimeException("Invalid token: no subject found");
            }
            logger.log("Token validated for user: " + username);
            return generatePolicy(username, "Allow", requestEvent.getMethodArn());

        } catch (Exception e) {
            logger.log("Authorization failed: " + e.getMessage());
            return generatePolicy("user", "Deny", requestEvent.getMethodArn(), e.getMessage());
        }
    }

    private Map<String, Object> generatePolicy(String principalId, String effect, String resource) {
        return generatePolicy(principalId, effect, resource, null);
    }

    private Map<String, Object> generatePolicy(String principalId, String effect, String resource, String contextMessage) {
        Map<String, Object> policyDocument = new HashMap<>();
        policyDocument.put("Version", "2012-10-17");

        Map<String, String> statement = new HashMap<>();
        statement.put("Effect", effect);
        statement.put("Action", "execute-api:Invoke");
        statement.put("Resource", resource);

        policyDocument.put("Statement", Collections.singletonList(statement));

        Map<String, Object> response = new HashMap<>();
        response.put("principalId", principalId);
        response.put("policyDocument", policyDocument);

        if (contextMessage != null) {
            Map<String, String> context = new HashMap<>();
            context.put("message", contextMessage);
            response.put("context", context);
        }

        return response;
    }

}
