package playkosmos.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;

import java.util.Date;

public class JWTProvider {

    private static final String SECRET_KEY = "kjdbvkjhbdkhjabdhvahbiudhiuvhiurhghwqbjdbadvbouasbdfbvajdbjvbouabdvoujabdfvjabdsvjaobdcvaola";

    public String generateJwtToken(String username) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(new Date(nowMillis + 3600000))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public String validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (SignatureException e) {
            throw new RuntimeException("Invalid JWT signature: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("JWT token is expired: " + e.getMessage());
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Malformed JWT token: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("JWT validation error: " + e.getMessage());
        }
    }

}
