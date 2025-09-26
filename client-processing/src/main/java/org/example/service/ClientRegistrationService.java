package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.clientModels.enums.DocumentType;
import org.example.dto.ClientRegistrationRequest;
import org.example.dto.ClientRegistrationResponse;
import org.example.clientModels.entity.Client;
import org.example.clientModels.entity.User;
import org.example.repository.ClientRepository;
import org.example.repository.UserRepository;
import org.example.repository.BlacklistRegistryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientRegistrationService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final BlacklistRegistryRepository blacklistRepository;

    @Transactional
    public ClientRegistrationResponse registerClient(ClientRegistrationRequest request) {
        // 1. ПРОВЕРКА ЧЕРНОГО СПИСКА
        checkClientNotBlacklisted(request.getDocumentType(), request.getDocumentId());

        // 2. Проверяем уникальность логина и email
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new IllegalArgumentException("Login already exists: " + request.getLogin());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // 3. Создаем и сохраняем User
        User user = new User();
        user.setLogin(request.getLogin());
        user.setPassword(request.getPassword()); // Без шифрования для простоты
        user.setEmail(request.getEmail());
        User savedUser = userRepository.save(user);

        // 4. Создаем и сохраняем Client с заготовленным clientId
        Client client = new Client();
        client.setClientId(generateUniqueClientId());
        client.setUser(savedUser);
        client.setFirstName(request.getFirstName());
        client.setMiddleName(request.getMiddleName());
        client.setLastName(request.getLastName());
        client.setDateOfBirth(request.getDateOfBirth());
        client.setDocumentType(request.getDocumentType());
        client.setDocumentId(request.getDocumentId());
        client.setDocumentPrefix(request.getDocumentPrefix());
        client.setDocumentSuffix(request.getDocumentSuffix());
        Client savedClient = clientRepository.save(client);

        // 5. Формируем ответ
        ClientRegistrationResponse response = new ClientRegistrationResponse();
        response.setMessage("Client registered successfully");
        response.setClientId(savedClient.getClientId());
        response.setUserId(savedUser.getId());
        response.setClientProfileId(savedClient.getId());

        return response;
    }

    /**
     * Проверяет, что клиент не находится в черном списке
     */
    private void checkClientNotBlacklisted(DocumentType documentType, String documentId) {
        boolean isBlacklisted = blacklistRepository.existsActiveBlacklistEntry(documentType, documentId);

        if (isBlacklisted) {
            throw new IllegalArgumentException(
                    "Client with document " + documentType + " " + documentId + " is blacklisted"
            );
        }
    }

    private String generateUniqueClientId() {
        return "77" + System.currentTimeMillis() % 100000000;
    }
}