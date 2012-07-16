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

            pst = con.prepareStatement("SELECT * FROM servers WHERE `servername` = '" + client + "'");
            result = pst.executeQuery();
            
            while (result.next()) {
            	String name = result.getString("name");
            	String ip = result.getString("ip");
            	String hash = result.getString("hash");
            	String servername = result.getString("servername");
            	String lobby_channel = result.getString("lobby_channel");
            	String console_channel = result.getString("console_channel");
                data = name + " " + ip + " " + hash + " " + servername + " " + lobby_channel + " " + console_channel;
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
