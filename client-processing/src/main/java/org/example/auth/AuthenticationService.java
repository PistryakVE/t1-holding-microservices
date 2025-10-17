package org.example.auth;

import org.example.config.JwtService;
import org.example.clientModels.enums.Role;
import org.example.clientModels.entity.User;
//Класс AuthenticationService отвечает за обработку логики регистрации и аутентификации пользователя.

import org.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;//Для работы с пользователями в базе данных.
    private final PasswordEncoder passwordEncoder;//Шифрует пароли пользователей перед их сохранением.
    private final JwtService jwtService;//Генерирует и проверяет токены JWT.
    private final AuthenticationManager authenticationManager;//Выполняет проверку учетных данных пользователя.

    public AuthenticationResponse register(RegisterRequest request) {//Регистрация нового пользователя
        if (!request.getPassword().equals(request.getConfirmPassword())) {//Если пароли не совпадают
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (repository.findByEmail(request.getEmail()).isPresent()) {//Если пользователь уже есть с таким email
            throw new IllegalArgumentException("Email is already taken");
        }
        // Создание дополнительных данных для токена

        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        repository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name()); // Добавляем роль пользователя в токен
        var jwtToken = jwtService.generateToken(claims,user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();

    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {//Аутентификация пользователя
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name()); // Добавляем роль пользователя в токен

        var jwtToken = jwtService.generateToken(claims,user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
