package com.scentzone.tools;

import com.scentzone.dao.UserDAO;
import com.scentzone.security.PasswordUtil;
import java.io.Console;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Scanner;

/** One-time trusted local setup: creates an admin. Public registration creates customers only. */
public final class AdminSetup {
    public static void main(String[] args) {
        System.out.println("Scent Zone - Create an ADMIN account");
        System.out.println("Requires a configured MySQL database and SCENT_DB_USER / SCENT_DB_PASSWORD.");
        Console console = System.console();
        Scanner scanner = console == null ? new Scanner(System.in) : null;
        String name, email, password;
        if (console != null) {
            name = console.readLine("Full name: ");
            email = console.readLine("Email: ");
            char[] secret = console.readPassword("Password (8+ characters): ");
            password = secret == null ? "" : new String(secret);
            if (secret != null) java.util.Arrays.fill(secret, '\0');
        } else {
            System.out.print("Full name: "); name = scanner.nextLine();
            System.out.print("Email: "); email = scanner.nextLine();
            System.out.print("Password (input visible in this terminal): "); password = scanner.nextLine();
        }
        try {
            if (name == null || name.trim().length() < 2 || name.length() > 100)
                throw new IllegalArgumentException("Invalid name.");
            if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 150)
                throw new IllegalArgumentException("Invalid email.");
            new UserDAO().create(name.trim(), email.trim().toLowerCase(Locale.ROOT),
                    PasswordUtil.hash(password), "ADMIN");
            System.out.println("Admin account created. You can now log in to the GUI.");
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("Admin setup failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
