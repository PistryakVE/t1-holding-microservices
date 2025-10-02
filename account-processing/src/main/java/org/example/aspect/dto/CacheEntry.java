package org.example.aspect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheEntry {
    private Object value;
    private Instant expirationTime;
    private String key;

    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }
}