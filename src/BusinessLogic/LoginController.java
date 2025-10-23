package BusinessLogic;

import ORM.UserDAO;
import DomainModel.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LoginController {

    public User login(String email, String password) {
        UserDAO userDAO = new UserDAO();
        try {
            if (email == null || email.isBlank())
                throw new IllegalArgumentException("Email cannot be empty");
            if (password == null || password.isBlank())
                throw new IllegalArgumentException("Password cannot be empty");

            List<User> matches = userDAO.getUserByEmail(email);
            if (matches == null || matches.isEmpty())
                throw new IllegalArgumentException("User not found");

            if (matches.size() > 1) {
                System.err.println("Warning: multiple users found with same email; taking the first");
            }
            User user = matches.get(0);
            if (!Objects.equals(user.getPassword(), password))
                throw new IllegalArgumentException("Invalid credentials");
            return user;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public void register(String name, String surname, String email, String password, boolean isModerator) {
        UserDAO userDAO = new UserDAO();
        try {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Name cannot be empty");
            if (surname == null || surname.isBlank())
                throw new IllegalArgumentException("Surname cannot be empty");
            if (email == null || email.isBlank())
                throw new IllegalArgumentException("Email cannot be empty");
            if (password == null || password.isBlank())
                throw new IllegalArgumentException("Password cannot be empty");

            List<User> existing = userDAO.getUserByEmail(email);
            if (existing != null && !existing.isEmpty())
                throw new IllegalArgumentException("Email already in use");

            userDAO.addUser(name, surname, email, password, isModerator);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public boolean isEmailAvailable(String email) {
        UserDAO userDAO = new UserDAO();
        try {
            if (email == null || email.isBlank())
                throw new IllegalArgumentException("Email cannot be empty");
            List<User> existing = userDAO.getUserByEmail(email);
            return existing == null || existing.isEmpty();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }
    }
}
