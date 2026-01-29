package com.medoc.opd.repository;

import com.medoc.opd.model.Token;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class TokenRepository {
    private final Map<String, Token> tokens = new ConcurrentHashMap<>();

    public Token save(Token token) {
        tokens.put(token.getId(), token);
        return token;
    }

    public Optional<Token> findById(String id) {
        return Optional.ofNullable(tokens.get(id));
    }

    public List<Token> findAll() {
        return tokens.values().stream().collect(Collectors.toList());
    }
}
