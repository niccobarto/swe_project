import DomainModel.*;
import BusinessLogic.*;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        User user=null;
        LoginController lg = new LoginController();
        //FILTRI PRESI DA GUI
        //in ordine: titolo, autore, formato, data inizio, data fine, tags
        Object[] filters=new Object[7];
        filters=new Object[]{null,1,null,null,null,null};
        do {
            user=lg.login("niccolobartoli@gmail.com","ciao");
        }while(user==null);
        AdminController ac=new AdminController(user);
        ac.setModerator(1,false);

        UserController uc = new UserController(user);
        ArrayList<Document> docs = uc.searchDocuments(create_filters(filters));

    }

    public static DocumentSearchCriteria create_filters(Object[] filters){
        DocumentSearchCriteriaBuilder builder=DocumentSearchCriteriaBuilder.getInstance();
        if(filters[0]!=null)
            builder.setDocumentTitle((String)filters[0]);
        if(filters[1]!=null)
            builder.setAuthorId((Integer)filters[1]);
        if(filters[2]!=null)
            builder.setFormat((DocumentFormat)filters[3]);
        if(filters[3]!=null)
            builder.setCreatedAfter((java.util.Date)filters[4]);
        if(filters[4]!=null)
            builder.setCreatedBefore((java.util.Date)filters[5]);
        if(filters[5]!=null)
            builder.setTags((ArrayList<String>)filters[6]);
        return builder.build();
    }
}