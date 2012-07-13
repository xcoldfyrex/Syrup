package com.coldfyre.syrup.Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQL {
	
	public static String getWaffleSettings(String client)  {
		ResultSet result;
		String data = "";
		
		Connection con = null;
        PreparedStatement pst = null;

        try {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.error("Cannot find mysql class","LIGHT_RED");
            } 
            con = DriverManager.getConnection(Config.sqlurl, Config.sqluser, Config.sqlpassword);

            pst = con.prepareStatement("SELECT * FROM servers WHERE id = 1");
            result = pst.executeQuery();
            
            while (result.next()) {
            	String name = result.getString("name");
            	String ip = result.getString("ip");
            	String hash = result.getString("hash");
                data = name + " " + ip + " " + hash;

            }

            
        } catch (SQLException e) {
            Log.error(e.getMessage() + " " + e,"LIGHT_RED");

        } finally {

            try {
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }

            } catch (SQLException e) {
            }
        }
		
		return data;
	}


}
