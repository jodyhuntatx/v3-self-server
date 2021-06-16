from tkinter import *
from tkinter import ttk
import mysql.connector
from mysql.connector import Error
from dblayer import *

class IdentityInfo:

  def __init__(self, parent):
    identityFrame = ttk.LabelFrame(parent, text='Identity Info',padding="3 3 12 12")
    identityFrame.grid(column=0, row=0, sticky=(N, W, E, S))
    identityFrame.columnconfigure(0, weight=1)
    identityFrame.rowconfigure(0, weight=1)

    self.identity1 = StringVar()
    identity1_entry = ttk.Entry(identityFrame, width=20, textvariable=self.identity1)
    identity1_entry.grid(column=1, row=1, sticky=(W, E))
    ttk.Label(identityFrame, text="Identity 1").grid(column=0, row=1, sticky=W)

    self.identity2 = StringVar()
    identity2_entry = ttk.Entry(identityFrame, width=20, textvariable=self.identity2)
    identity2_entry.grid(column=1, row=2, sticky=(W, E))
    ttk.Label(identityFrame, text="Identity 2").grid(column=0, row=2, sticky=W)
    
  def print(self, *args):
    try:
      print("\"identities\": [")
      print("  {\"identity\": \"!host "+self.identity1.get()+"\"},")
      print("  {\"identity\": \"!host "+self.identity2.get()+"\"}")
      print("]")
    except ValueError:
      pass

##############################
# Writes variables to MySQL database
  def write_to_db(self, projectDbId, accReqDbId):
    # projectDbId - primary key of project record
    # accReqDbId - primary key of access request
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)

      query = "INSERT IGNORE INTO appidentities "		\
                "(project_id, accreq_id, name, auth_method) "	\
                "VALUES(%s,%s,%s,%s)"
      args = (projectDbId, accReqDbId, self.identity1.get(), 'auth_method')
      cursor.execute(query, args)
      if self.identity2.get():
        args = (projectDbId, accReqDbId, self.identity2.get(), 'auth_method')
        cursor.execute(query, args)

      DBLayer.dbConn.commit()

    except Error as e:
      print("MySQL Error:", e)

