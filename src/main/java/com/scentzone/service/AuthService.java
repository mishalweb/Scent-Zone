package com.scentzone.service;

import com.scentzone.dao.UserDAO;
import com.scentzone.model.User;
import com.scentzone.security.PasswordUtil;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final UserDAO users = new UserDAO();

    public Optional<User> login(String email, String password) throws SQLException {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        var candidate = users.findByEmail(normalized);
        if (candidate.isPresent() && PasswordUtil.verify(password, candidate.get().passwordHash())) {
            return Optional.of(candidate.get().user());
        }
        return Optional.empty();
    }

    public void register(String name, String email, String password, String confirm) throws SQLException {
        if (name == null || name.trim().length() < 2 || name.trim().length() > 100)
            throw new IllegalArgumentException("Enter a name between 2 and 100 characters.");
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 150 || !EMAIL.matcher(normalized).matches())
            throw new IllegalArgumentException("Enter a valid email address.");
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("Password must have at least 8 characters.");
        if (!password.equals(confirm))
            throw new IllegalArgumentException("Passwords do not match.");
        users.create(name.trim(), normalized, PasswordUtil.hash(password), "CUSTOMER");
    }
}
