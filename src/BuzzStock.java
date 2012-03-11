
	import javax.swing.*;
	import java.awt.*;
	import java.io.BufferedReader;
	import java.io.IOException;
	import java.io.InputStreamReader;
	import java.net.URL;
	import java.awt.BorderLayout;
	import java.awt.Font;
	import java.awt.event.ActionEvent;
	import java.awt.event.ActionListener;
	import java.sql.*;
	import java.util.ArrayList;
	import java.util.StringTokenizer;
	import java.util.Vector;
	import javax.swing.JFrame;
	import javax.swing.JLabel;
	import javax.swing.JScrollPane;
	import javax.swing.JTable;
	import javax.swing.table.DefaultTableModel;
	import javax.swing.table.TableModel;
	import java.util.zip.Checksum;
	import java.util.zip.Adler32;
	import java.util.Date;
	import java.text.DateFormat;
	import java.text.SimpleDateFormat;
	import java.util.Calendar;
	import org.jsoup.Jsoup; // used to clean text from html 

	/****************************************************************************************************************
	 * 
	 * WARNING:
	 * This class uses google queries - After ~1000 queries, google might block searches for few hours from your IP
	 ***************************************************************************************************************/

	/****************************************************************************************************************
	 * 
	 * @author Rami
	 * This class tries to search for Israel socks buzz
	 * It will go on every page on webpage_list, and if it detects new lines on pages, it will analyze them. 
	 * In order to know if a given word is an Israel stock, google query will be performed. 
	 ***************************************************************************************************************/
	public class BuzzStock 
	{
		
		static public class WebPage 
		{	
			public String name;
			public String address;
			public String cpCode;
			// Class constructor
			public WebPage()
			{
				name = "1";
				address = "1";
				cpCode = "1";
			}

			public WebPage(String name1, String link1, String cpCode1) {
				name = name1;
				address = link1;
				cpCode = cpCode1;
			}
		}
		// global vars
		public static WebPage[] webpage_list = new WebPage [1]; // webpages list that is to processed
		
		// const
		final public static String DB_NAME="BuzzStock.db";
		// const code pages needed to get the right stream content, rami. 
		final public static String CP_U8="UTF-8";
		final public static String CP_WINHEB="windows-1255";
		final String GOOGLE_CP=CP_U8;
		final String THEMARKER_STOCK_PAGE = "http://finance.themarker.com/f/quoten.jhtml?navBar=comp&sType=ta&secCode=";
		final int STOCK_CODE_LEN = 6;
		final int STOCK_MAX_LEN = 99;
 		
		// for debug
		public static int searchCount=0;
		public static JTextField tfStatus;
		public static JTextField tfOp;
		public static JTextField tfDebug;
		public static JTextField tfError;
		
		public static void main(String[] args) throws Exception 
		{
			new BuzzStock();
		}
		
		// This is the GUI constructor - calls searchBuzz, that actually search for stocks
		public BuzzStock() throws Exception
		{

			JFrame jf=new JFrame();
			JLabel l1=new JLabel("Status:");
			JLabel l2=new JLabel("Current Operaion:");
			JLabel l3=new JLabel("Debug:");
			JLabel l4=new JLabel("Error:");
			
			tfStatus=new JTextField(100);
			tfOp=new JTextField(100);
			tfDebug=new JTextField(100);
			tfError=new JTextField(100);

			JButton bDelete=new JButton("Delete DataBase");
			JButton bUpdate=new JButton("Update");
	
			ActionListener listener = null;

			jf.setTitle("BuzzStock (Debug)");
			jf.setSize(1200,300);

			Container pane=jf.getContentPane();
			pane.setLayout(new GridLayout(5,2) );

			pane.add(l1);
			pane.add(tfStatus);
			pane.add(l2);
			pane.add(tfOp);
			pane.add(l3);
			pane.add(tfDebug);
			pane.add(l4);
			pane.add(tfError);
			pane.add(bDelete);
			pane.add(bUpdate);
			// bDelete.addActionListener(listener) ;
			bDelete.addActionListener(new ActionListener() 
			{
	            public void actionPerformed(ActionEvent e)
	            {
	                //Execute when button is pressed
	                try {
						delDB();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
	            }
	        });     

			bUpdate.addActionListener(new ActionListener() 
			{
				 
	            public void actionPerformed(ActionEvent e)
	            {
	                //Execute when button is pressed
	                try {
						searchBuzz();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
	            }
	        });     

			jf.setVisible(true);
			searchBuzz();
			jf.setDefaultCloseOperation(jf.EXIT_ON_CLOSE);
		}
		
		// This is actually the core method that search for stocks
		private void searchBuzz() throws Exception
		{
			InputStreamReader isr = null;
			BufferedReader in = null;
			URL url;
			String stockName="";
			Integer stockCode;
			String readLine;
			initDB();
			// webpage_list[0]= new WebPage ("talniri RSS","http://www.talniri.co.il/forums/forum_rss.asp?forumid=1", CP_WINHEB);
			// webpage_list[0]= new WebPage ("talniri","http://www.talniri.co.il/forums/forum.asp?id=1", CP_WINHEB);
			webpage_list[0]= new WebPage ("TA 100 wiki","http://he.wikipedia.org/wiki/%D7%9E%D7%93%D7%93_%D7%AA%D7%9C_%D7%90%D7%91%D7%99%D7%91_100", CP_U8);
			for(int i = 0;i< webpage_list.length; i++)
			{
				try 
				{
					// needed to set user agent as a browser, or else google will return 403 // rami 
					// System.setProperty("https.agent", "Mozilla/3.0 (compatible; MSIE 7.0; Windows NT 6.0; Trident/3.0)");
					System.setProperty("http.agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0)"); 
					// url = new URL("http://www.google.co.il/search?q=אפריקה");
					url = new URL(webpage_list[i].address);
					isr = new InputStreamReader(url.openStream(), webpage_list[i].cpCode);   
					in = new BufferedReader(isr);   
					ArrayList<String> savedWebPage = new ArrayList<String>();
					while ((readLine = in.readLine()) != null) 
					{

						savedWebPage.add(readLine);
					}
					
					for (String procLine : savedWebPage) 
					{     
						if (insertLinesHistory(procLine, webpage_list[i].address ) ) //line will be inserted only it is new and was not proccessd b4
						{ // new line, need to check for stocks 
						    tfStatus.setText("Proccess Site: " + webpage_list[i].name + ", HTML Line= " + procLine);
						    //work around for out sbStockName parameter since, I did not create objects to reflects the data base - this way I can return stock name // rami
						    StringBuilder sbStockName = new StringBuilder();
							stockCode = getStock (procLine, sbStockName); 
							stockName = sbStockName.toString(); 
							if (stockCode > 0)
							{
								System.out.println("+++++++++++buzz stockName++++++++++++++++++++++++++");
								System.out.println(stockName);
								insertStock(Integer.toString(stockCode), stockName, THEMARKER_STOCK_PAGE+Integer.toString(stockCode), 1, currTime(), webpage_list[i].address);
							}
						}
					}	
				    tfStatus.setText("Proccess Site: " + webpage_list[i].name + " [FINISHED]");
				    Connection conn =
				  	      DriverManager.getConnection("jdbc:sqlite:"+DB_NAME);
				  	    Statement stat = conn.createStatement();
				    
				  	// View table in frame
				    ResultSet rs = stat.executeQuery("select * from stock_buzz ORDER BY sentiment DESC;");
				    TableModel myData = resultSetToTableModel(rs) ;
				    showTableInFrame (myData);
				    
					}
					catch (Exception e) 
					{
						//rami // for problems like: java.net.SocketException: Connection reset, and other
						// stock=-1;
						
						changeIP(); // best way to deal error 503 after 1000 searches in google, not implemented as it is network specific.
						
						System.out.println("eeeeeeeeeeeee__searchBuzz___eeeeeeeeeeeeeeee");
						e.printStackTrace();	
						System.out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
						tfError.setText(currTime() + " >> " + e.getMessage() );  

					} 
					finally 
					
					{
						try 
						{
							isr.close();
						} 
							catch (IOException ioe) 
					{

					}
				}
	  	      	
			}
		}

		private Integer getStock(String line, StringBuilder sbStockName) 
		{
			String clean_text = Jsoup.parse(line).text();
			// clean the code twice to be sure it is cleaned 
			clean_text = Jsoup.parse(clean_text).text();
			Integer stock = 0;
			StringTokenizer st = new StringTokenizer(clean_text);

			while (st.hasMoreTokens()) 
			{
				searchCount++;
				String token = st.nextToken();
				// . is a regex metacharacter to match anything (except newline). Since we want to match a literal . we escape it with \. 
				// Since both Java Strings and regex engine use \ as escape character we need to use \\, + is the quantifier for one or more to replace a sequence
				// token.replaceAll("\\.+"," ");
				
				token=token.replaceAll(","," ");
				token=token.replaceAll("\\."," ");
				token=token.replaceAll(" ","");
				
				try // try to substring if it is too big - try prevents string index out of range
				{
					token = token.substring(0, STOCK_MAX_LEN);
				} catch (Exception ex) {
				}
				
				tfOp.setText("Google Search " + searchCount  +"="  + token + ", Clean line="  +clean_text);

				stock = googleSearchStock (token);
				if (stock > 0) // no need to search that line for more stocks, as it is not important. most line have one stock if they have it at all...
				{
					System.out.println("+++++++++++toekn getstock++++++++++++++++++++++++++");
					System.out.println(token);
					// // I clean token it here or else i might get error 400 from google. google ignores , and . anyhow. // Server returned HTTP response code: 400 for URL: http://www.google.co.il/search?q=document writeln("\x3cdiv[new line rami]					eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee

					sbStockName.append(token);
					tfDebug.setText("Stock found: "+ token +" - "+ stock );
					return stock;
				}
			}

			return stock;
		}

		// search stock via google assumes stock is only one word, better code should also search for two words stocks  
		private Integer googleSearchStock(String token) 
		{	
			final String GOOGLE_THEMARKER_STAMP = "secCode=";
			int stock=0;
			URL url;
			String strStock ="";
			
			InputStreamReader isr = null;
			BufferedReader in = null;
			String line; 

			final String FALSE_POSITIVE_STOCKS = "מנייה,מניה,NY";
			
			int index = FALSE_POSITIVE_STOCKS.indexOf(token);
			if (index !=-1) //token is on false positive list 
			{
				return 0;
			}

			try 
			{

				// needed to set as a browser, or else google will return 403 // rami
				// System.setProperty("https.agent", "Mozilla/3.0 (compatible; MSIE 7.0; Windows NT 6.0; Trident/3.0)");
				System.setProperty("http.agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0)");
				
				url = new URL("http://www.google.co.il/search?q="+token);
				isr = new InputStreamReader(url.openStream(), GOOGLE_CP);   
				in = new BufferedReader(isr);   

				while ((line = in.readLine()) != null) 
				{
					index = line.indexOf(GOOGLE_THEMARKER_STAMP);
					if (index !=-1) //string found 
					{
						strStock = line.substring(index + GOOGLE_THEMARKER_STAMP.length(),index + GOOGLE_THEMARKER_STAMP.length() + STOCK_CODE_LEN);
						try
						{
							// tfDebug.setText("Stock found: "+strStock); // some times Stock found: 1230" ?? garabge why??
							stock=Integer.parseInt(strStock);
							
						}
						catch (Exception e)
						{
							stock=-2;
						}
						
					}

				}
				isr.close();
			} 
			
			catch (Exception e) {
				//rami // for google problems like: error 503 after 1000 searches in google
				changeIP(); // best way to deal error 503 after 1000 searches in google, not implemented as it is network specific.
				stock=-1;
				System.out.println("eeeeeeeeeeeee__searchStock__eeeeeeeeeeeeeeee");
				e.printStackTrace();	
				System.out.println("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
				tfError.setText(currTime() + " >> " + e.getMessage() );  
			} finally 
			{
				return stock;
			}
		}

		private void changeIP() { // not implemented as it is network specific.
			// TODO Auto-generated method stub
			
		}
		
		public static void delDB() throws Exception 
		{
			Class.forName("org.sqlite.JDBC");
		    Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
		    Statement stat = conn.createStatement();
		    stat.executeUpdate("drop table if exists lines_history;");
		    stat.executeUpdate("drop table if exists stock_history;");
		    stat.executeUpdate("drop table if exists stock_buzz;");
		    conn.close();			
		}
		
		public static void initDB() throws Exception 
		{

			Class.forName("org.sqlite.JDBC");
		    Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
		    tfStatus.setText("Init DB");
		    Statement stat = conn.createStatement();
		    stat.executeUpdate("create table if not exists lines_history (line, crc32 integer, time, webpage, PRIMARY KEY (crc32));");
		    stat.executeUpdate("CREATE INDEX if not exists IDX_lines_history on lines_history (line)");
		    stat.executeUpdate("create table if not exists stock_history (stock_id, stock_name, stock_link, sentiment integer, time, webpage);");
		    // some explicit redundancy ffu
		    stat.executeUpdate("create table if not exists stock_buzz (stock_id, stock_name, stock_link, sentiment integer, last_update, PRIMARY KEY (stock_id) );");

		    conn.setAutoCommit(true);
		    conn.close();			
		}

		// this will insert line if, and only if it is a new line (via crc32 check). if it is a new line it will return true
		private boolean insertLinesHistory(String line, String address)  throws Exception
		{
			Class.forName("org.sqlite.JDBC");
			Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
			Statement stat = conn.createStatement();
			Boolean lineInserted = false;
			Long crc32=String2crc(line);
		    ResultSet rs = stat.executeQuery("select * from lines_history where crc32=" + crc32);
		    if (!rs.next()) 
	    	// select did not fetch record - new line found - inset it
		    { 	
		    			
    			lineInserted = true;
    	 		PreparedStatement prep = conn.prepareStatement(
			      "insert into lines_history values (?, ?, ? ,?);");
			    
			    prep.setString(1, line);
			    prep.setLong(2, crc32);
			    prep.setString(3, currTime() );
			    prep.setString(4, address);
			    prep.addBatch();
			    conn.setAutoCommit(false);
			    prep.executeBatch();
			    conn.setAutoCommit(true);
		    }
		    else
    			lineInserted = false;
		    
		    rs.close();
		    conn.close();
		    return lineInserted;
			
		}

		// this insert stock to stock history, and to the final stock buzz table
		private void insertStock(String stockId, String stockName,
				String stockLink, int sentiment, String currTime, String address) throws Exception 
		{
			Class.forName("org.sqlite.JDBC");
			Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
			Statement stat = conn.createStatement();
			ResultSet rs;
			PreparedStatement prep;
			
			// insert stock to stock history
	 		prep = conn.prepareStatement(
		      "insert into stock_history values (?, ?, ? ,? ,? ,?);");
		    
		    prep.setString(1, stockId);
		    prep.setString(2, stockName);
		    prep.setString(3, stockLink );
		    prep.setInt(4, sentiment);
		    prep.setString(5, currTime);
		    prep.setString(6, address);
		    
		    prep.addBatch();
		    conn.setAutoCommit(false);
		    prep.executeBatch();
		    conn.setAutoCommit(true);
		    // updae buzz final stock buzz table
		    rs = stat.executeQuery("select * from stock_buzz where stock_id=" +"'" +  stockId +"'");
		    if (!rs.next()) 
	    	// select did not fetch record - new line found - insert it
		    { 	
		    	// stock_id, stock_name, stock_link, sentiment integer, last_update
		    	prep = conn.prepareStatement(
		  		      "insert into stock_buzz values (?, ?, ? ,? ,?);");	  		    
	  		    prep.setString(1, stockId);
	  		    prep.setString(2, stockName);
	  		    prep.setString(3, stockLink );
	  		    prep.setInt(4, sentiment);
	  		    prep.setString(5, currTime);
	  		    prep.addBatch();
	  		    conn.setAutoCommit(false);
	  		    prep.executeBatch();
	  		    conn.setAutoCommit(true);

		    }
		    else //update
		    {
		    	int updSentiment  = rs.getInt("sentiment") + sentiment; // add sentiment to the current sentiment of the stock
		    	// stat.executeUpdate("update stock_buzz set sentiment=" +updSentiment+" set last_update=" +"'" + currTime +"'" +" where stock_id=" +"'" + stockId +"'");
		    	stat.executeUpdate("update stock_buzz set sentiment=" +updSentiment+ " where stock_id=" +"'" + stockId +"'");
		    	System.out.println("update stock_buzz set sentiment=" +updSentiment+ " where stock_id=" +"'" + stockId +"'");
		    	stat.executeUpdate("update stock_buzz set last_update=" +"'" + currTime +"'" + " where stock_id=" +"'" + stockId +"'");
		    	System.out.println("update stock_buzz set last_update=" +"'" + currTime +"'" + " where stock_id=" +"'" + stockId +"'");
		    	  
		    }
		    
		    rs.close();
		    conn.close();
		    return;
		}

		private String currTime() {
			   DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			   //get current date time with Date()
			   Date date = new Date();
			   //get current date time with Calendar()
			   Calendar cal = Calendar.getInstance();
			   // // System.out.println(dateFormat.format(cal.getTime()));
			   String Time = dateFormat.format(cal.getTime());
			return Time;
		}
		
		public static long String2crc (String line)
		{
            //Convert string to bytes
            byte bytes[] = line.getBytes();
            Checksum checksum = new Adler32();
            /*
             * To compute the Adler32 checksum for byte array, use
             * 
             * void update(bytes[] b, int start, int length)
             * method of Adler32 class.
             */
            checksum.update(bytes,0,bytes.length);
            /*
             * Get the generated checksum using
             * getValue method of Adler32 class.
             */
            long lngChecksum = checksum.getValue();
            return lngChecksum; 
		}		

		public static  void showTableInFrame(TableModel myData) 
		{
		    JTable table = new JTable(myData); 	    
		    
		    JScrollPane spTable = new JScrollPane(table); 
		    JScrollPane scrollPane = new JScrollPane(table);
	        table.setFillsViewportHeight(true);

	        JLabel lblHeading = new JLabel("Stocks Sentiment");
	        lblHeading.setFont(new Font("Arial",Font.TRUETYPE_FONT,24));
	        
	        final JFrame frame = new JFrame("Buzz Stocks Table");
	        frame.getContentPane().setLayout(new BorderLayout());
	        frame.getContentPane().add(lblHeading,BorderLayout.PAGE_START);
	        frame.getContentPane().add(scrollPane,BorderLayout.CENTER);
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        frame.setSize(750, 400);
	        frame.setVisible(true);
		
		}

	    public static TableModel resultSetToTableModel(ResultSet rs) 
	    {
	        try {
	            ResultSetMetaData metaData = rs.getMetaData();
	            int numberOfColumns = metaData.getColumnCount();
	            Vector columnNames = new Vector();

	            // Get the column names
	            for (int column = 0; column < numberOfColumns; column++) {
	                columnNames.addElement(metaData.getColumnLabel(column + 1));
	            }

	            // Get all rows.
	            Vector rows = new Vector();
	            while (rs.next()) {
	                Vector newRow = new Vector();

	                for (int i = 1; i <= numberOfColumns; i++) {
	                    newRow.addElement(rs.getObject(i));
	                }

	                rows.addElement(newRow);	               
	            }

	            return new DefaultTableModel(rows, columnNames);
	        } catch (Exception e) {
	            e.printStackTrace();
	            return null;
	        }
	    }
	}