import os
import sys
import time
import datetime
import subprocess
from tkinter import *
from tkinter import ttk

import mysql.connector
from mysql.connector import Error
from dblayer import *

class AccessReview:

######################################
  def __init__(self, parent):
    mainframe = ttk.Frame(parent, width=1000, padding="12 12 12 12")
    mainframe.grid(column=0, row=1, sticky=(N, W, E, S), columnspan=3)

    ttk.Button(parent, text="Exit", command=self.exit, style='Cybr.TButton').grid(column=3, row=5, sticky=E)
    parent.bind("<Return>", self.exit)

    accreqLabel = ttk.Label(parent,text='Access Requests',font=('Helvetica bold',14),anchor='center')
    accreqLabel.grid(column=0, row=1, sticky=(N, W, E, S), columnspan=3)

    #################################
    # Unapproved access requests
    #################################
    unapprFrame = ttk.LabelFrame(parent, text='Unapproved', padding="3 3 12 12")
    unapprFrame.grid(column=0, row=2, sticky=(N, W, E, S), columnspan=3)
    unapprFrame.columnconfigure(0, weight=1)
    unapprFrame.rowconfigure(0, weight=1)

    # get access request data from db
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      query = "SELECT proj.name, accreq.datetime, accreq.environment, accreq.safe_name, appid.name, cybracct.name, cybracct.db_name "	\
		"FROM projects proj, accessrequests accreq, appidentities appid, cybraccounts cybracct "	\
		"WHERE NOT accreq.approved AND accreq.project_id = proj.id AND appid.accreq_id = accreq.id "	\
		"AND appid.project_id = proj.id AND cybracct.project_id = proj.id"
      cursor.execute(query)
      accessRequests = cursor.fetchall()
    except Error as e:
      print("Error while connecting to MySQL", e)

    # cols correspond to SELECT fields
    cols=('Project','DateTime','Environment','Safe','AppId','Account','Target')

    # get max column widths for each column
    maxwidths = [0,0,0,0,0,0,0]
    for req in accessRequests:
      for i in range(len(cols)):
        if len(str(req[i])) > maxwidths[i]:
          maxwidths[i] = len(str(req[i]))

    unapprTree = ttk.Treeview(unapprFrame, height=5, columns=cols)
    unapprTree['show'] = 'headings'
    unapprTree.column('#0',minwidth=0,width=0)	# minimize width of 'ghost' column
    for c in range(len(cols)):
      unapprTree.column(cols[c], width=maxwidths[c], anchor='center')
      unapprTree.heading(cols[c],text=cols[c])

    for req in accessRequests:
      unapprTree.insert('','end',value=req)
    unapprTree.grid(column=0, row=0, sticky=(N, W, E, S),columnspan=3)

    #################################
    # Approved/Unprovisioned access requests
    #################################
    unprovFrame = ttk.LabelFrame(parent, text='Approved/Unprovisioned', padding="3 3 12 12")
    unprovFrame.grid(column=0, row=3, sticky=(N, W, E, S), columnspan=3)
    unprovFrame.columnconfigure(0, weight=1)
    unprovFrame.rowconfigure(0, weight=1)

    # get access request data from db
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      query = "SELECT proj.name, accreq.datetime, accreq.environment, accreq.safe_name, appid.name, cybracct.name, cybracct.db_name "	\
		"FROM projects proj, accessrequests accreq, appidentities appid, cybraccounts cybracct "	\
		"WHERE accreq.approved AND NOT accreq.provisioned "						\
		"AND accreq.project_id = proj.id AND appid.accreq_id = accreq.id "				\
		"AND appid.project_id = proj.id AND cybracct.project_id = proj.id"
      cursor.execute(query)
      accessRequests = cursor.fetchall()
    except Error as e:
      print("Error while connecting to MySQL", e)

    # cols correspond to SELECT fields
    cols=('Project','DateTime','Environment','Safe','AppId','Account','Target')

    # get max column widths for each column
    maxwidths = [0,0,0,0,0,0,0]
    for req in accessRequests:
      for i in range(len(cols)):
        if len(str(req[i])) > maxwidths[i]:
          maxwidths[i] = len(str(req[i]))

    unprovTree = ttk.Treeview(unprovFrame, height=5, columns=cols)
    unprovTree['show'] = 'headings'
    unprovTree.column('#0',minwidth=0,width=0)	# minimize width of 'ghost' column
    for c in range(len(cols)):
      unprovTree.column(cols[c], width=maxwidths[c], anchor='center')
      unprovTree.heading(cols[c],text=cols[c])

    for req in accessRequests:
      unprovTree.insert('','end',value=req)
    unprovTree.grid(column=0, row=1, sticky=(N, W, E, S),columnspan=3)

    #################################
    # Provisioned access requests
    #################################
    provFrame = ttk.LabelFrame(parent, text='Provisioned', padding="3 3 12 12")
    provFrame.grid(column=0, row=4, sticky=(N, W, E, S), columnspan=3)
    provFrame.columnconfigure(0, weight=1)
    provFrame.rowconfigure(0, weight=1)

    # get access request data from db
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      query = "SELECT proj.name, accreq.datetime, accreq.environment, accreq.safe_name, appid.name, cybracct.name, cybracct.db_name "	\
		"FROM projects proj, accessrequests accreq, appidentities appid, cybraccounts cybracct "	\
		"WHERE accreq.provisioned AND accreq.project_id = proj.id AND appid.accreq_id = accreq.id "	\
		"AND appid.project_id = proj.id AND cybracct.project_id = proj.id"
      cursor.execute(query)
      accessRequests = cursor.fetchall()
    except Error as e:
      print("Error while connecting to MySQL", e)

    # cols correspond to SELECT fields
    cols=('Project','DateTime','Environment','Safe','AppId','Account','Target')

    # get max column widths for each column
    maxwidths = [0,0,0,0,0,0,0]
    for req in accessRequests:
      for i in range(len(cols)):
        if len(str(req[i])) > maxwidths[i]:
          maxwidths[i] = len(str(req[i]))

    provTree = ttk.Treeview(provFrame, height=5, columns=cols)
    provTree['show'] = 'headings'
    provTree.column('#0',minwidth=0,width=0)	# minimize width of 'ghost' column
    for c in range(len(cols)):
      provTree.column(cols[c], width=maxwidths[c], anchor='center')
      provTree.heading(cols[c],text=cols[c])

    for req in accessRequests:
      provTree.insert('','end',value=req)
    provTree.grid(column=0, row=1, sticky=(N, W, E, S),columnspan=3)


    for child in parent.winfo_children():
        child.grid_configure(padx=10, pady=10)

######################################
  def exit(self, *args):
    DBLayer.dbClose()
    sys.exit(0)
