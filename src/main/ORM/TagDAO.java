package ORM;

import DomainModel.Tag;
import java.util.List;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TagDAO extends BaseDAO {

    private static final Logger LOGGER = Logger.getLogger(TagDAO.class.getName());

    public TagDAO(){
        super();
    }

    private Tag map(ResultSet resultSet) throws SQLException {
        String label = resultSet.getString("tag_label");
        String description = resultSet.getString("description");
        return new Tag(label, description);
    }

    private String normalize(String s){
        return s == null ? null: s.trim().toLowerCase();
    }

    public void addTag(Tag t) {
        try {
            String q = "INSERT INTO tag (tag_label, description) VALUES (?, ?)";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setString(1, t.getLabel());
            ps.setString(2, t.getDescription());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Error during addTag(label=" + t.getLabel() + ")", e);
        }
    }

    public Tag findByLabelNormalized(String label){
        try{
            String query = "SELECT tag_label, description FROM tag WHERE LOWER(TRIM(tag_label)) = LOWER(TRIM(?))";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, label);
            ResultSet resultSet = preparedStatement.executeQuery();
            Tag out = null;
            if (resultSet.next())
                out = map(resultSet);
            resultSet.close();
            preparedStatement.close();
            return out;
        } catch (SQLException e){
        LOGGER.log(Level.SEVERE, "Error findBylabelNormalized(tagLabel=" + label + ")", e);}
        return null;
    }

}
