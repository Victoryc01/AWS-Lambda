package playkosmos.dao;


import lombok.RequiredArgsConstructor;
import playkosmos.dbutil.DatabaseConnectionManager;
import playkosmos.entity.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public class UserDAO {

    private final DatabaseConnectionManager dbConnectionManager;

    public void saveUserToDatabase(User user) {
        try (Connection connection = dbConnectionManager.getConnection()) {
            String query = "INSERT INTO user_tbl (username, email, phone_number, DOB, password) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, user.getUsername());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getPhoneNumber());
                statement.setDate(4, java.sql.Date.valueOf(user.getDOB()));
                statement.setString(5, user.getPassword());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving user to database", e);
        }
    }

    public User loginUser(String userInfo, String password) {
        try (Connection connection = dbConnectionManager.getConnection()) {
            String query = "SELECT * FROM user_tbl WHERE (email = ? OR phone_number = ?) AND password = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, userInfo);
                statement.setString(2, userInfo);
                statement.setString(3, password);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        User user = new User();
                        user.setUsername(resultSet.getString("username"));
                        user.setEmail(resultSet.getString("email"));
                        user.setPhoneNumber(resultSet.getString("phone_number"));
                        user.setDOB(resultSet.getDate("DOB").toLocalDate());
                        user.setPassword(resultSet.getString("password"));
                        return user;
                    } else {
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error logging in user", e);
        }
    }
}
