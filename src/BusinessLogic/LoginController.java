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

            User match = userDAO.getUserByEmail(email);
            if (match == null)
                throw new IllegalArgumentException("User not found");
            if (!Objects.equals(match.getPassword(), password))
                throw new IllegalArgumentException("Invalid credentials");
            return match;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public void register(String name, String surname, String email, String password, boolean isModerator,boolean isAdmin) {
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

            User existing = userDAO.getUserByEmail(email);
            if (existing != null)
                throw new IllegalArgumentException("Email already in use");

            userDAO.addUser(name, surname, email, password, isModerator,isAdmin);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public boolean isEmailAvailable(String email) {
        UserDAO userDAO = new UserDAO();
        try {
            if (email == null || email.isBlank())
                throw new IllegalArgumentException("Email cannot be empty");
            User existing = userDAO.getUserByEmail(email);
            return existing == null;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }
    }
}
