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

    public User register(String name, String surname, String email, String password, boolean isModerator,boolean isAdmin) {
        User user=null;
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
            if (existing != null){
                System.err.println("Email already in use");
                return null;
            }

            userDAO.addUser(name, surname, email, password, isModerator,isAdmin);
            user= userDAO.getUserByEmail(email);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return user;
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
