import mysql.connector
from mysql.connector import Error
import config

class DBLayer:
  dbConn = mysql.connector.connect(**config.db)

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

