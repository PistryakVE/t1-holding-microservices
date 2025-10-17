package org.example.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F442B472B4B6250645367566B5970";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);//Извлекает email из токена
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);//Универсальный метод для извлечения любого значения из токена
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }//Генерирует токен без дополнительных данных.

    public String generateToken(
            Map<String, Object> extraClaims,//Генерирует токен с: extraClaims — дополнительные данные, которые можно передать в токен.
            UserDetails userDetails
    ){
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())//setSubject — имя пользователя (владельца токена).
                .setIssuedAt(new Date(System.currentTimeMillis()))//setIssuedAt — время создания токена.
                .setExpiration(new Date(System.currentTimeMillis()+ 1000 * 60 * 60 * 24))//setExpiration — время истечения срока действия токена (в данном случае 24 часа).
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)//signWith — подпись токена с использованием алгоритма HS256 и ключа.
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {//Проверяет, валиден ли токен, путем:
        final String username = extractUsername(token);//Сопоставления имени пользователя из токена и данных пользователя.
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);//Проверки, не истек ли срок действия токена.
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }//Проверяет, истек ли токен. Для этого извлекается дата истечения и сравнивается с текущим временем.

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }//Извлекает дату истечения токена.

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }//Извлекает все данные (claims) из токена. Для этого используется Jwts.parserBuilder() с указанием ключа для проверки подписи.

    private Key getSignInKey() {//Метод для получения ключа подписи
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);//Преобразует строку SECRET_KEY из Base64 в массив байтов и
        return Keys.hmacShaKeyFor(keyBytes);// создает ключ HMAC (SHA-256), который используется для подписи и проверки JWT.
    }

}
