package com.evcharging.service;

import com.evcharging.dao.TokenDAO;
import com.evcharging.dao.UserDAO;
import com.evcharging.dto.LoginRequest;
import com.evcharging.dto.LoginResponse;
import com.evcharging.model.User;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final TokenDAO tokenDAO = new TokenDAO();

    public LoginResponse login(LoginRequest request) throws Exception {
        if (request == null ||
                request.getUsername() == null ||
                request.getPassword() == null ||
                request.getUsername().isBlank() ||
                request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password are required.");
        }

        User user = userDAO.authenticate(request.getUsername(), request.getPassword());

        if (user == null) {
            return null;
        }

        String token = tokenDAO.createToken(user.getUsername());

        return new LoginResponse(token, user.getUsername(), user.getRole());
    }
}