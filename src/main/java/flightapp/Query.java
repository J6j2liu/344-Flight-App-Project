package flightapp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.sql.Types;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private static final String LOGIN_SQL = "SELECT username, password FROM USERS_j6j2liu WHERE username = ?";
  private static final String CREATE_CHECK_SQL = "SELECT COUNT(*) AS cnt FROM USERS_j6j2liu WHERE username = ?";
  private static final String CREATE_EXECUTE_SQL = "INSERT INTO USERS_j6j2liu VALUES (?, ?, ?)";
  private static final String SEARCH_DIRECT_SQL = "SELECT TOP(?) "
      + "fid,day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
      + "FROM Flights "
      + "WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled = 0 "
      + "ORDER BY actual_time ASC, fid ASC ";
  private static final String SEARCH_INDIRECT_SQL = "SELECT TOP(?) "
      + "f1.fid AS f1_fid,f1.day_of_month AS f1_day_of_month,f1.carrier_id AS f1_carrier_id,f1.flight_num AS f1_flight_num, "
      + "f1.origin_city AS f1_origin_city,f1.dest_city AS f1_dest_city,f1.actual_time AS f1_actual_time,f1.capacity AS f1_capacity,f1.price AS f1_price, "
      + "f2.fid AS f2_fid,f2.day_of_month AS f2_day_of_month,f2.carrier_id AS f2_carrier_id,f2.flight_num AS f2_flight_num, "
      + "f2.origin_city AS f2_origin_city,f2.dest_city AS f2_dest_city,f2.actual_time AS f2_actual_time,f2.capacity AS f2_capacity,f2.price AS f2_price "
      + "FROM Flights AS f1, Flights AS f2 "
      + "WHERE f1.origin_city = ? AND f2.dest_city = ? AND f1.day_of_month = ? AND f1.day_of_month = f2.day_of_month "
      + "AND f1.dest_city = f2.origin_city AND f1.canceled = 0 AND f2. canceled = 0 "
      + "ORDER BY (f1.actual_time + f2.actual_time) ASC, f1.fid ASC, f2.fid ASC";
  private static final String BOOK_SAME_DAY_SQL = "SELECT COUNT(*) AS count "
      + "FROM RESERVATIONS_j6j2liu AS R, FLIGHTS AS F "
      + "WHERE username = ? AND F.fid = R.fid_1 AND F.day_of_month = ?  ";
  private static final String BOOK_NEW_RES_SQL = "INSERT INTO RESERVATIONS_j6j2liu VALUES (?, ?, ?, ?, ?)";
  private static final String BOOK_FLIGHT_CAPACITY_SQL = "SELECT COUNT(*) AS cap "
      + "FROM RESERVATIONS_j6j2liu AS R "
      + "WHERE R.fid_1 = ? OR R.fid_2 = ?";
  private static final String BOOK_RES_ID_SQL = "SELECT max(rid) AS id FROM RESERVATIONS_j6j2liu";
  private static final String PAY_CHECK_BAL_SQL = "SELECT balance FROM USERS_j6j2liu WHERE username = ?";
  private static final String PAY_UPDATE_BAL_SQL = "UPDATE USERS_j6j2liu SET balance = balance - ? WHERE username = ?";
  private static final String PAY_CHECK_RES_SQL = "SELECT * FROM RESERVATIONS_j6j2liu WHERE rid = ? AND paid = 0";
  private static final String PAY_CHECK_COST_SQL = "SELECT price FROM FLIGHTS WHERE fid = ?";
  private static final String PAY_UPDATE_RES_SQL = "UPDATE RESERVATIONS_j6j2liu SET paid = 1 WHERE rid = ?";
  private static final String RES_GET_FID_SQL = "SELECT * FROM RESERVATIONS_j6j2liu WHERE username = ?";
  private static final String RES_GET_FLIGHT_SQL = "SELECT * FROM FLIGHTS WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;
  private PreparedStatement loginStmt;
  private PreparedStatement createCheckStmt;
  private PreparedStatement createExecuteStmt;
  private PreparedStatement searchDirectStmt;
  private PreparedStatement searchIndirectStmt;
  private PreparedStatement bookSameDayStmt;
  private PreparedStatement bookNewResStmt;
  private PreparedStatement bookFlightCapacityStmt;
  private PreparedStatement bookResIdStmt;
  private PreparedStatement payCheckBalStmt;
  private PreparedStatement payUpdateBalStmt;
  private PreparedStatement payCheckResStmt;
  private PreparedStatement payCheckCostStmt;
  private PreparedStatement payUpdateResStmt;
  private PreparedStatement resGetFidStmt;
  private PreparedStatement resGetFlightStmt;

  //
  // Instance variables
  //
  private String user;
  private int res_id;
  private List<Itinerary> curr_itineraries;

  protected Query() throws SQLException, IOException {
    prepareStatements();
    user = "";
    curr_itineraries = new ArrayList<>();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      Statement s = conn.createStatement();
      s.executeUpdate("DELETE FROM RESERVATIONS_j6j2liu");
      s.executeUpdate("DELETE FROM USERS_j6j2liu");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);

    loginStmt = conn.prepareStatement(LOGIN_SQL);
    createCheckStmt = conn.prepareStatement(CREATE_CHECK_SQL);
    createExecuteStmt = conn.prepareStatement(CREATE_EXECUTE_SQL);
    searchDirectStmt = conn.prepareStatement(SEARCH_DIRECT_SQL);
    searchIndirectStmt = conn.prepareStatement(SEARCH_INDIRECT_SQL);
    bookSameDayStmt = conn.prepareStatement(BOOK_SAME_DAY_SQL);
    bookNewResStmt = conn.prepareStatement(BOOK_NEW_RES_SQL);
    bookFlightCapacityStmt = conn.prepareStatement(BOOK_FLIGHT_CAPACITY_SQL);
    bookResIdStmt = conn.prepareStatement(BOOK_RES_ID_SQL);
    payCheckBalStmt = conn.prepareStatement(PAY_CHECK_BAL_SQL);
    payUpdateBalStmt = conn.prepareStatement(PAY_UPDATE_BAL_SQL);
    payCheckResStmt = conn.prepareStatement(PAY_CHECK_RES_SQL);
    payCheckCostStmt = conn.prepareStatement(PAY_CHECK_COST_SQL);
    payUpdateResStmt = conn.prepareStatement(PAY_UPDATE_RES_SQL);
    resGetFidStmt = conn.prepareStatement(RES_GET_FID_SQL);
    resGetFlightStmt = conn.prepareStatement(RES_GET_FLIGHT_SQL);
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    // TODO: YOUR CODE HERE
    if (!user.equals(""))
      return "User already logged in\n";

    try {

      loginStmt.clearParameters();
      loginStmt.setString(1, username);
      ResultSet rs = loginStmt.executeQuery();

      if (rs.next() && PasswordUtils.plaintextMatchesSaltedHash(password, rs.getBytes("Password"))) {
        user = rs.getString("Username");
        return "Logged in as " + user + "\n";
      } else {
        return "Login failed\n";
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return "Login failed\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (initAmount < 0)
      return "Failed to create user\n";

    boolean created = false;

    try {
      conn.setAutoCommit(false);

      createCheckStmt.clearParameters();
      createCheckStmt.setString(1, username);

      ResultSet rsCheck = createCheckStmt.executeQuery();
      rsCheck.next();
      int cnt = rsCheck.getInt("cnt");
      rsCheck.close();

      if (cnt != 0) {
        conn.rollback();
      } else {
        createExecuteStmt.clearParameters();
        createExecuteStmt.setString(1, username);
        createExecuteStmt.setBytes(2, PasswordUtils.saltAndHashPassword(password));
        createExecuteStmt.setInt(3, initAmount);
        createExecuteStmt.executeUpdate();

        conn.commit();
        created = true;
      }

      conn.setAutoCommit(true);
    } catch (SQLException e) {
      try {
        conn.rollback();
        conn.setAutoCommit(true);
      } catch (SQLException se) {
        se.printStackTrace();
      }
      if (isDeadlock(e)) {
        return transaction_createCustomer(username, password, initAmount);
      }
    }

    if (created) {
      return "Created user " + username + "\n";
    } else {
      return "Failed to create user\n";
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity,
      boolean directFlight, int dayOfMonth,
      int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection
    // attacks) AND only
    // handles searches for direct flights. We are providing it *only* as an example
    // of how
    // to use JDBC; you are required to replace it with your own secure
    // implementation.
    //
    List<Itinerary> itineraries = new ArrayList<>();

    try {
      // one hop itineraries
      searchDirectStmt.clearParameters();
      searchDirectStmt.setInt(1, numberOfItineraries);
      searchDirectStmt.setString(2, originCity);
      searchDirectStmt.setString(3, destinationCity);
      searchDirectStmt.setInt(4, dayOfMonth);

      ResultSet oneHopResults = searchDirectStmt.executeQuery();

      while (oneHopResults.next()) {
        int result_fid = oneHopResults.getInt("fid");
        int result_dayOfMonth = oneHopResults.getInt("day_of_month");
        String result_carrierId = oneHopResults.getString("carrier_id");
        String result_flightNum = oneHopResults.getString("flight_num");
        String result_originCity = oneHopResults.getString("origin_city");
        String result_destCity = oneHopResults.getString("dest_city");
        int result_time = oneHopResults.getInt("actual_time");
        int result_capacity = oneHopResults.getInt("capacity");
        int result_price = oneHopResults.getInt("price");

        Flight f1_direct = new Flight(result_fid, result_dayOfMonth, result_carrierId, result_flightNum,
            result_originCity, result_destCity, result_time, result_capacity, result_price);

        itineraries.add(new Itinerary(true, f1_direct));
      }

      oneHopResults.close();

      // two hop itineraries

      if (!directFlight && numberOfItineraries > itineraries.size()) {
        searchIndirectStmt.clearParameters();
        searchIndirectStmt.setInt(1, numberOfItineraries - itineraries.size());
        searchIndirectStmt.setString(2, originCity);
        searchIndirectStmt.setString(3, destinationCity);
        searchIndirectStmt.setInt(4, dayOfMonth);

        ResultSet twoHopResults = searchIndirectStmt.executeQuery();

        while (twoHopResults.next()) {
          int f1_result_fid = twoHopResults.getInt("f1_fid");
          int f1_result_dayOfMonth = twoHopResults.getInt("f1_day_of_month");
          String f1_result_carrierId = twoHopResults.getString("f1_carrier_id");
          String f1_result_flightNum = twoHopResults.getString("f1_flight_num");
          String f1_result_originCity = twoHopResults.getString("f1_origin_city");
          String f1_result_destCity = twoHopResults.getString("f1_dest_city");
          int f1_result_time = twoHopResults.getInt("f1_actual_time");
          int f1_result_capacity = twoHopResults.getInt("f1_capacity");
          int f1_result_price = twoHopResults.getInt("f1_price");

          int f2_result_fid = twoHopResults.getInt("f2_fid");
          int f2_result_dayOfMonth = twoHopResults.getInt("f2_day_of_month");
          String f2_result_carrierId = twoHopResults.getString("f2_carrier_id");
          String f2_result_flightNum = twoHopResults.getString("f2_flight_num");
          String f2_result_originCity = twoHopResults.getString("f2_origin_city");
          String f2_result_destCity = twoHopResults.getString("f2_dest_city");
          int f2_result_time = twoHopResults.getInt("f2_actual_time");
          int f2_result_capacity = twoHopResults.getInt("f2_capacity");
          int f2_result_price = twoHopResults.getInt("f2_price");

          Flight f1_indirect = new Flight(f1_result_fid, f1_result_dayOfMonth, f1_result_carrierId, f1_result_flightNum,
              f1_result_originCity, f1_result_destCity, f1_result_time, f1_result_capacity, f1_result_price);

          Flight f2_indirect = new Flight(f2_result_fid, f2_result_dayOfMonth, f2_result_carrierId, f2_result_flightNum,
              f2_result_originCity, f2_result_destCity, f2_result_time, f2_result_capacity, f2_result_price);

          itineraries.add(new Itinerary(false, f1_indirect, f2_indirect));
        }

        twoHopResults.close();

      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    Collections.sort(itineraries);
    StringBuffer sb = new StringBuffer();

    if (itineraries.size() == 0) {
      curr_itineraries = new ArrayList<>();
      return "No flights match your selection\n";
    }

    for (int i = 0; i < itineraries.size() && i < numberOfItineraries; i++) {
      itineraries.get(i).id = i;
      sb.append(itineraries.get(i).toString() + "\n");
    }

    if (!user.equals(""))
      curr_itineraries = itineraries;

    return sb.toString();
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    if (user.equals(""))
      return "Cannot book reservations, not logged in\n";
    if (itineraryId < 0 || itineraryId >= curr_itineraries.size() || curr_itineraries.size() == 0)
      return "No such itinerary " + itineraryId + "\n";

    Itinerary curr = curr_itineraries.get(itineraryId);

    try {
      conn.setAutoCommit(false);
      // check to see that there is not another on the same day
      bookSameDayStmt.clearParameters();
      bookSameDayStmt.setString(1, user);
      bookSameDayStmt.setInt(2, curr.f1.dayOfMonth);

      ResultSet sameDayResults = bookSameDayStmt.executeQuery();

      if (!sameDayResults.next()) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Booking failed\n";
      } else if (sameDayResults.getInt("count") > 0) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "You cannot book two flights in the same day\n";
      }
      sameDayResults.close();

      // check to see the flight is still open
      bookFlightCapacityStmt.clearParameters();
      bookFlightCapacityStmt.setInt(1, curr.f1.fid);
      bookFlightCapacityStmt.setInt(2, curr.f1.fid);

      ResultSet f1FlightCap = bookFlightCapacityStmt.executeQuery();

      if (!f1FlightCap.next() || f1FlightCap.getInt("cap") >= checkFlightCapacity(curr.f1.fid)) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Booking failed\n";
      }

      f1FlightCap.close();

      // check f2 is open
      if (!curr.direct) {
        bookFlightCapacityStmt.clearParameters();
        bookFlightCapacityStmt.setInt(1, curr.f2.fid);
        bookFlightCapacityStmt.setInt(2, curr.f2.fid);

        ResultSet f2FlightCap = bookFlightCapacityStmt.executeQuery();

        if (!f2FlightCap.next() || f2FlightCap.getInt("cap") >= checkFlightCapacity(curr.f2.fid)) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "Booking failed\n";
        }

        f2FlightCap.close();
      }
      // time to book

      // get res id

      ResultSet bookResIdResult = bookResIdStmt.executeQuery();
      bookResIdResult.next();
      int res_id = 1 + bookResIdResult.getInt("id");
      bookResIdResult.close();

      bookNewResStmt.clearParameters();
      bookNewResStmt.setInt(1, res_id);
      bookNewResStmt.setString(2, user);
      bookNewResStmt.setInt(3, 0);
      bookNewResStmt.setInt(4, curr.f1.fid);
      if (curr.direct) {
        bookNewResStmt.setNull(5, Types.INTEGER);
      } else {
        bookNewResStmt.setInt(5, curr.f2.fid);
      }

      bookNewResStmt.executeUpdate();
      conn.commit();
      conn.setAutoCommit(true);
      return "Booked flight(s), reservation ID: " + res_id + "\n";
    } catch (SQLException e) {
      try {
        conn.rollback();
        conn.setAutoCommit(true);
      } catch (SQLException se) {
        se.printStackTrace();
      }
      if (isDeadlock(e)) {
        return book(itineraryId);
      }
    }

    return "Booking failed\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    if (user.equals("")) {
      return "Cannot pay, not logged in\n";
    } else if (reservationId < 1) {
      return "Cannot find unpaid reservation " + reservationId + " under user: " + user + "\n";
    }

    try {
      conn.setAutoCommit(false);

      payCheckResStmt.clearParameters();
      payCheckResStmt.setInt(1, reservationId);

      ResultSet payCheckResResult = payCheckResStmt.executeQuery();

      if (!payCheckResResult.next()) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Cannot find unpaid reservation " + reservationId + " under user: " + user + "\n";
      }

      int fid_1 = payCheckResResult.getInt("fid_1");
      int fid_2 = payCheckResResult.getInt("fid_2");
      boolean direct = !payCheckResResult.wasNull();

      payCheckResResult.close();

      payCheckCostStmt.clearParameters();
      payCheckCostStmt.setInt(1, fid_1);

      ResultSet payCheckCostResult = payCheckCostStmt.executeQuery();

      if (!payCheckCostResult.next()) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Failed to pay for reservation " + reservationId + "\n";
      }

      int cost = payCheckCostResult.getInt("price");
      payCheckCostResult.close();

      if (direct) {
        payCheckCostStmt.clearParameters();
        payCheckCostStmt.setInt(1, fid_2);

        ResultSet payCheckCostResult_f2 = payCheckCostStmt.executeQuery();

        if (!payCheckCostResult_f2.next()) {
          conn.rollback();
          conn.setAutoCommit(true);
          return "Failed to pay for reservation " + reservationId + "\n";
        }

        cost += payCheckCostResult_f2.getInt("price");
        payCheckCostResult_f2.close();
      }

      payCheckBalStmt.clearParameters();
      payCheckBalStmt.setString(1, user);

      ResultSet payCheckBalResult = payCheckBalStmt.executeQuery();

      if (!payCheckBalResult.next()) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "Failed to pay for reservation " + reservationId + "\n";
      }

      int bal = payCheckBalResult.getInt("balance");
      payCheckBalResult.close();

      if (cost > bal) {
        conn.rollback();
        conn.setAutoCommit(true);
        return "User has only " + bal + " in account but itinerary costs " + cost
            + "\n";
      }

      payUpdateBalStmt.clearParameters();
      payUpdateBalStmt.setInt(1, cost);
      payUpdateBalStmt.setString(2, user);
      payUpdateBalStmt.executeUpdate();

      payUpdateResStmt.setInt(1, reservationId);
      payUpdateResStmt.executeUpdate();

      conn.commit();
      conn.setAutoCommit(true);
      return "Paid reservation: " + reservationId + " remaining balance: " + (bal - cost) + "\n";
    } catch (SQLException e) {
      try {
        conn.rollback();
        conn.setAutoCommit(true);
      } catch (SQLException se) {
        se.printStackTrace();
      }
      if (isDeadlock(e)) {
        return transaction_pay(reservationId);
      }
    }

    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE
    if (user.equals(""))
      return "Cannot view reservations, not logged in\n";

    try {
      resGetFidStmt.clearParameters();
      resGetFidStmt.setString(1, user);

      ResultSet resGetFidResult = resGetFidStmt.executeQuery();
      StringBuffer sb = new StringBuffer();

      while (resGetFidResult.next()) {
        resGetFlightStmt.clearParameters();
        resGetFlightStmt.setInt(1, resGetFidResult.getInt("fid_1"));

        ResultSet resGetFlightResult_f1 = resGetFlightStmt.executeQuery();

        if (!resGetFlightResult_f1.next())
          return "Failed to retrieve reservations\n";

        int result_fid = resGetFlightResult_f1.getInt("fid");
        int result_dayOfMonth = resGetFlightResult_f1.getInt("day_of_month");
        String result_carrierId = resGetFlightResult_f1.getString("carrier_id");
        String result_flightNum = resGetFlightResult_f1.getString("flight_num");
        String result_originCity = resGetFlightResult_f1.getString("origin_city");
        String result_destCity = resGetFlightResult_f1.getString("dest_city");
        int result_time = resGetFlightResult_f1.getInt("actual_time");
        int result_capacity = resGetFlightResult_f1.getInt("capacity");
        int result_price = resGetFlightResult_f1.getInt("price");

        Flight f1 = new Flight(result_fid, result_dayOfMonth, result_carrierId, result_flightNum,
            result_originCity, result_destCity, result_time, result_capacity, result_price);

        sb.append("Reservation " + resGetFidResult.getInt("rid") + " paid: " +
            (resGetFidResult.getInt("paid") == 0 ? "false" : "true") + ":\n" +
            f1 + "\n");

        resGetFidResult.getInt("fid_2");
        if (!resGetFidResult.wasNull()) {
          resGetFlightStmt.clearParameters();
          resGetFlightStmt.setInt(1, resGetFidResult.getInt("fid_2"));

          ResultSet resGetFlightResult_f2 = resGetFlightStmt.executeQuery();

          if (!resGetFlightResult_f2.next())
            return "Failed to retrieve reservations\n";

          result_fid = resGetFlightResult_f2.getInt("fid");
          result_dayOfMonth = resGetFlightResult_f2.getInt("day_of_month");
          result_carrierId = resGetFlightResult_f2.getString("carrier_id");
          result_flightNum = resGetFlightResult_f2.getString("flight_num");
          result_originCity = resGetFlightResult_f2.getString("origin_city");
          result_destCity = resGetFlightResult_f2.getString("dest_city");
          result_time = resGetFlightResult_f2.getInt("actual_time");
          result_capacity = resGetFlightResult_f2.getInt("capacity");
          result_price = resGetFlightResult_f2.getInt("price");

          Flight f2 = new Flight(result_fid, result_dayOfMonth, result_carrierId, result_flightNum,
              result_originCity, result_destCity, result_time, result_capacity, result_price);

          sb.append(f2 + "\n");
        }
      }

      if (sb.length() == 0)
        return "No reservations found\n";

      return sb.toString();

    } catch (SQLException e) {
      try {
        conn.rollback();
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
    return "Failed to retrieve reservations\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
        int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }

  /**
   * A class to store information about a single flight
   *
   */
  class Itinerary implements Comparable<Itinerary> {
    public int id;
    public boolean direct;
    public Flight f1;
    public Flight f2;
    public int time;

    Itinerary(boolean direct, Flight f1) {
      this.id = 0;
      this.direct = direct;
      this.f1 = f1;
      this.f2 = null;
      this.time = this.f1.time;
    }

    Itinerary(boolean direct, Flight f1, Flight f2) {
      this.id = 0;
      this.direct = direct;
      this.f1 = f1;
      this.f2 = f2;
      this.time = this.f1.time + this.f2.time;
    }

    @Override
    public int compareTo(Itinerary o) {
      if (this.time == o.time) {
        if (this.f1.fid == o.f1.fid) {
          if (this.direct) {
            return -1;
          } else if (o.direct) {
            return 1;
          } else {
            return Integer.compare(this.f2.fid, o.f2.fid);
          }
        } else {
          return Integer.compare(this.f1.fid, o.f1.fid);
        }
      } else {
        return Integer.compare(this.time, o.time);
      }
    }

    @Override
    public String toString() {
      return "Itinerary " + this.id + ": " + (direct ? 1 : 2) + " flight(s), "
          + this.time + " minutes\n"
          + this.f1 + (!direct ? "\n" + this.f2 : "");
    }
  }
}
