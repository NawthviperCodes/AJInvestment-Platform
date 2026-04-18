package com.aj.investment.dao;

//import com.aj.investment.model.Clients;
//import com.aj.investment.model.Logdata;
//import com.aj.investment.model.LogdataStatus;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
// import java.util.Date;

@Stateless
public class ClientDAO {

    // WildFly injects this — no EntityManagerFactory needed
    @PersistenceContext(unitName = "AJInvestmentPU")
    private EntityManager em;

  //  public boolean registerClient(Logdata user) {
   //     try {
   //         em.persist(user);
   //         Clients client = new Clients();
     //       client.setLogDataId(user);
    //        client.setCreationTime(new Date());
      //      em.persist(client);
     //       return true;  // container auto-commits on success
       // } catch (Exception e) {
      //      System.err.println("ERROR registering client: " + e.getMessage());
     //       e.printStackTrace();
       //     return false; // container auto-rolls back on exception
      //  }
   // }

   // public LogdataStatus getDefaultStatus() {
//        try {
  //          return em.createQuery(
    //            "SELECT s FROM LogdataStatus s WHERE s.status = :s",
      //          LogdataStatus.class)
        //        .setParameter("s", "Active")
          //      .getSingleResult();
     //   } catch (Exception e) {
       //     System.err.println("Active status not found: " + e.getMessage());
  //          return null;
    //    }
  //  }
}
