import mysql.connector
from mysql.connector import Error

class DBLayer:
  dbConfig = {	'host': 'localhost',
		'database': 'appgovdb',
		'user': 'root',
		'password': 'Cyberark1'}

  dbConn = mysql.connector.connect(**dbConfig)

  def __init__(self):
    try:
      if dbConn.is_connected():
        db_Info = self.dbConn.get_server_info()
        print("Connected to MySQL Server version ", db_Info)
        cursor = dbConn.cursor()
        cursor.execute("select database();")
        record = cursor.fetchone()
        print("You're connected to database: ", record)
    except Error as error:
        print(error)

  def dbClose():
      if DBLayer.dbConn.is_connected():
        DBLayer.dbConn.close()

