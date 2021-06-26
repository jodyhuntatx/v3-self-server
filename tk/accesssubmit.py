import os
import sys
import time
import datetime
import subprocess
from tkinter import *
from tkinter import ttk
from projectinfo import *
from identityinfo import *
from accountinfo import *

import mysql.connector
from mysql.connector import Error
from dblayer import *

import requests

class AccessRequest:

  def __init__(self, parent):
    mainframe = ttk.Frame(parent, padding="12 12 12 12")
    mainframe.grid(column=0, row=1, sticky=(N, W, E, S))

    projectFrame = ttk.Frame(mainframe, padding="10 10 10 10", style='Cybr.TFrame')
    projectFrame.grid(column=0, row=0, sticky=(N, W, E, S))
    self.projectInfo = ProjectInfo(projectFrame)

    identityFrame = ttk.Frame(mainframe, padding="10 10 10 10", style='Cybr.TFrame')
    identityFrame.grid(column=0, row=1, sticky=(N, W, E, S))
    self.identityInfo = IdentityInfo(identityFrame)

#    accountFrame = ttk.Frame(mainframe, padding="10 10 10 10", style='Cybr.TFrame')
#    accountFrame.grid(column=0, row=2, sticky=(N, W, E, S))
#    self.accountInfo = AccountInfo(accountFrame)

    ttk.Button(parent, text="Submit", command=self.submit, style='Cybr.TButton').grid(column=3, row=3, sticky=E)
    parent.bind("<Return>", self.submit)

    self.projectInfo.project_entry.focus()

    for child in parent.winfo_children():
        child.grid_configure(padx=10, pady=10)

  ######################################
  def submit(self, *args):
    self.write_all_to_db()
    DBLayer.dbClose()
    sys.exit(0)
#    self.provision_approved_reqs()

  ##############################
  # Writes form input variables to MySQL database
  def write_all_to_db(self, *args):
    projectDbId = self.projectInfo.write_to_db()
    accReqDbId = self.write_to_db(projectDbId)
    self.identityInfo.write_to_db(projectDbId, accReqDbId)
#    self.accountInfo.write_to_db(projectDbId)
    DBLayer.dbConn.commit()

  ##############################
  # Writes variables to MySQL database
  def write_to_db(self, projectDbId):
    # projectDbId is primary key for project in DB
    try:
      ts = time.time()
      timestamp = datetime.datetime.fromtimestamp(ts).strftime('%Y-%m-%d %H:%M:%S')

      # if dev environment, auto-approve request w/o waiting for review
      approved = 0
      if self.projectInfo.env.get() == 'dev':
        approved = 1

      cursor = DBLayer.dbConn.cursor(buffered=True)
      query = "INSERT IGNORE INTO accessrequests "									\
      		"(approved, project_id, datetime, environment, vault_name, safe_name, requestor, cpm_name, lob_name) "	\
                "VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s)"
      args = (approved,projectDbId,timestamp,self.projectInfo.env.get(),self.projectInfo.vault.get(),self.projectInfo.safe.get(),self.projectInfo.requestor.get(),'PasswordManager','CICD')
      cursor.execute(query, args)
      accreqDbId = cursor.lastrowid;
      DBLayer.dbConn.commit()

      return accreqDbId
    except Error as e:
      print("AccessRequest:Error inserting access request", e)

  ##############################
  # Provision all approved access requests
  def provision_approved_reqs(self):
    print("starting provisioning...")
    accessRequestIds = ""
    try:
      query = "SELECT id FROM accessrequests WHERE approved = 1 AND provisioned = 0"
      cursor = DBLayer.dbConn.cursor(buffered=True)
      cursor.execute(query)
      accessRequestIds = cursor.fetchall()
    except Error as e:
      print("AccessRequest:Error selecting id from accessrequests", e)

    print("Approved, unprovisioned IDs: ", accessRequestIds) 

    apiEndpoint = 'http://localhost:8080/cybr'  
    pasSessionToken = requests.get(apiEndpoint + '/pas/login', auth=('Administrator', 'Cyberark1')) 
    conjurApiKey = requests.get(apiEndpoint + '/conjur/login', auth=('admin', 'CYberark11@@')) 
    for id in accessRequestIds:
      print("Provisioning accReqId: ", id[0]) 
      r = requests.post(url = apiEndpoint + "/provision?accReqId=" + str(id[0]), data = "")
      print("Provisioning response: ", r)
      try:
        query = "UPDATE accessrequests SET provisioned = 1 WHERE id = %s"
        args = (id)
        cursor.execute(query, args)
        DBLayer.dbConn.commit()
      except Error as e:
        print("AccessRequest:Error updating provisioning status", e)
