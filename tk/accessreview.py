import os
import sys
import time
import datetime
import subprocess
from tkinter import *
from tkinter import ttk
import tkinter as tk

import mysql.connector
from mysql.connector import Error
from dblayer import *

import requests

class AccessReview:

######################################
  def __init__(self, parent):
    mainframe = ttk.Frame(parent, width=1000, padding="12 12 12 12")
    mainframe.grid(column=0, row=1, sticky=(N, W, E, S), columnspan=3)

    accreqLabel = ttk.Label(parent,text='Access Requests',font=('Helvetica bold',14),anchor='center')
    accreqLabel.grid(column=0, row=1, sticky=(N, W, E, S), columnspan=3)

    # cols must correspond to fields in SELECT statements for all treeviews
    self.cols=('Project','AppId','Safe','Environment','DateTime','RequestId')
    self.unapprTree = None
    self.unprovTree = None
    self.provTree = None

    #################################
    # Unapproved access requests
    #################################
    self.unapprFrame = ttk.LabelFrame(parent, text='Unapproved', padding="3 3 12 12")
    self.unapprFrame.grid(column=0, row=2, sticky=(N, W, E, S), columnspan=3)
    self.unapprFrame.columnconfigure(0, weight=1)
    self.unapprFrame.rowconfigure(0, weight=1)
    self.buildUnapprovedTree(self.unapprFrame)

    #################################
    # Approved/Not Provisioned access requests
    #################################
    self.unprovFrame = ttk.LabelFrame(parent, text='Approved/Provisioning Incomplete', padding="3 3 12 12")
    self.unprovFrame.grid(column=0, row=3, sticky=(N, W, E, S), columnspan=3)
    self.unprovFrame.columnconfigure(0, weight=1)
    self.unprovFrame.rowconfigure(0, weight=1)
    self.buildUnprovisionedTree(self.unprovFrame)

    #################################
    # Provisioned access requests
    #################################
    self.provFrame = ttk.LabelFrame(parent, text='Provisioned', padding="3 3 12 12")
    self.provFrame.grid(column=0, row=4, sticky=(N, W, E, S), columnspan=3)
    self.provFrame.columnconfigure(0, weight=1)
    self.provFrame.rowconfigure(0, weight=1)
    self.buildProvisionedTree(self.provFrame)

    # Refresh button at bottom right of main window
    ttk.Button(parent, text="Refresh", command=self.refresh, style='Cybr.TButton').grid(column=3, row=1, sticky=E)

    # Exit button at bottom right of main window
    ttk.Button(parent, text="Exit", command=self.exit, style='Cybr.TButton').grid(column=3, row=5, sticky=E)
    parent.bind("<Return>", self.exit)

    for child in parent.winfo_children():
        child.grid_configure(padx=10, pady=10)

######################################
  # Build Unapproved access request treeview
  def buildUnapprovedTree(self,parent):
    # get access request data from db
    try:
      DBLayer.dbConnect()	# refresh DB connection to avoid timeouts
      cursor = DBLayer.dbConn.cursor(buffered=True)
      query = "SELECT proj.name, appid.name, accreq.safe_name, accreq.environment, accreq.datetime, accreq.id " \
	      "FROM " \
		"projects proj, " \
		"accessrequests accreq, " \
		"appidentities appid " \
	      "WHERE " \
		"NOT accreq.approved " \
		"AND NOT accreq.rejected " \
		"AND accreq.project_id = proj.id " \
		"AND appid.accreq_id = accreq.id "
      cursor.execute(query)
      accessRequests = cursor.fetchall()
      DBLayer.dbConn.commit()
      cursor.close()
    except Error as e:
      print("buildUnapprovedTree: Error selecting unapproved access requests.", e)

    # get max column widths for each column
    maxwidths = [0,0,0,0,0,0]
    for req in accessRequests:
      for i in range(len(self.cols)):
        if len(str(req[i])) > maxwidths[i]:
          maxwidths[i] = len(str(req[i]))

    if self.unapprTree is not None:
      self.unapprTree.destroy()

    self.unapprTree = ttk.Treeview(parent, height=5, columns=self.cols)
    self.unapprTree['show'] = 'headings'

    for c in range(len(self.cols)):
      self.unapprTree.column(self.cols[c], width=maxwidths[c], anchor='center')
      self.unapprTree.heading(self.cols[c],text=self.cols[c])

    for req in accessRequests:
      self.unapprTree.insert('','end',value=req)

    #++++++++++++++++++++++++++++++++++++++
    # setup popup menu for approvals
    self.unapprTree.popup_menu = tk.Menu(self.unapprTree, tearoff=0)
    self.unapprTree.popup_menu.add_command(label="Approve", command=self.approve)
    self.unapprTree.popup_menu.add_command(label="Reject", command=self.rejectSubmitted)
    self.unapprTree.popup_menu.add_separator()
    def do_popup(event):
      # display the popup menu
      try:
        self.unapprTree.popup_menu.selection = self.unapprTree.set(self.unapprTree.identify_row(event.y))
        self.unapprTree.popup_menu.post(event.x_root, event.y_root)
      finally:
        # make sure to release the grab (Tk 8.0a1 only)
        self.unapprTree.popup_menu.grab_release()
    self.unapprTree.bind("<Button-2>", do_popup)

    self.unapprTree.grid(column=0, row=0, sticky=(N, W, E, S),columnspan=3)

######################################
  def approve(self):
    try:
      approvedReqId = self.unapprTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # update approval status of selected access request 
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      cursor.execute("""UPDATE accessrequests SET approved=1 WHERE id=%s""", (approvedReqId,))
      DBLayer.dbConn.commit()
      cursor.close()
    except Error as e:
      print("approve: Error updating accessrequest", e)

    self.buildUnapprovedTree(self.unapprFrame)		# rebuild unapproved tree w/o approved access request
    self.buildUnprovisionedTree(self.unprovFrame)	# rebuild unprovisioned tree w/ approved access request

######################################
  def rejectSubmitted(self):
    try:
      approvedReqId = self.unapprTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # update rejected status of selected access request
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      cursor.execute("""UPDATE accessrequests SET rejected=1 WHERE id=%s""", (approvedReqId,))
      DBLayer.dbConn.commit()
      cursor.close()
    except Error as e:
      print("reject: Error updating accessrequest", e)

    self.buildUnapprovedTree(self.unapprFrame)          # rebuild unapproved tree w/o rejected access request

######################################
  def popup(self, event):
      try:
          self.popup_menu.tk_popup(event.x_root, event.y_root, 0)
      finally:
          self.popup_menu.grab_release()

######################################
  def buildUnprovisionedTree(self,parent):
    # get access request data from db
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      query = "SELECT proj.name, appid.name, accreq.safe_name, accreq.environment, accreq.datetime, accreq.id " \
		"FROM "					\
		" projects proj, "			\
		" accessrequests accreq, "		\
		" appidentities appid "			\
		"WHERE "				\
		" accreq.approved "			\
		" AND NOT accreq.rejected"		\
		" AND NOT accreq.provisioned "		\
		" AND NOT accreq.revoked"		\
		" AND accreq.project_id = proj.id " 	\
		" AND appid.accreq_id = accreq.id "
      cursor.execute(query)
      accessRequests = cursor.fetchall()
      DBLayer.dbConn.commit()
      cursor.close()
    except Error as e:
      print("buildUnprovisionedTree: Error selecting approved access requests", e)

    # get max column widths for each column
    maxwidths = [0,0,0,0,0,0]
    for req in accessRequests:
      for i in range(len(self.cols)):
        if len(str(req[i])) > maxwidths[i]:
          maxwidths[i] = len(str(req[i]))

    if self.unprovTree is not None:
      self.unprovTree.destroy()

    self.unprovTree = ttk.Treeview(parent, height=5, columns=self.cols)
    self.unprovTree['show'] = 'headings'
    self.unprovTree.column('#0',minwidth=0,width=0)	# minimize width of 'ghost' column
    for c in range(len(self.cols)):
      self.unprovTree.column(self.cols[c], width=maxwidths[c], anchor='center')
      self.unprovTree.heading(self.cols[c],text=self.cols[c])

    for req in accessRequests:
      self.unprovTree.insert('','end',value=req)

    #++++++++++++++++++++++++++++++++++++++
    # setup popup menu for provisioning
    self.unprovTree.popup_menu = tk.Menu(self.unprovTree, tearoff=0)
    self.unprovTree.popup_menu.add_command(label="Provision", command=self.provision)
    self.unprovTree.popup_menu.add_command(label="Reject", command=self.rejectApproved)
    self.unprovTree.popup_menu.add_separator()
    def do_popup(event):
      # display the popup menu
      try:
        self.unprovTree.popup_menu.selection = self.unprovTree.set(self.unprovTree.identify_row(event.y))
        self.unprovTree.popup_menu.post(event.x_root, event.y_root)
      finally:
        # make sure to release the grab (Tk 8.0a1 only)
        self.unprovTree.popup_menu.grab_release()
    self.unprovTree.bind("<Button-2>", do_popup)

    self.unprovTree.grid(column=0, row=1, sticky=(N, W, E, S),columnspan=3)

######################################
  def provision(self):
    try:
      provisionReqId = self.unprovTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # make REST call to provision access request from DB
    apiEndpoint = config.cybr["apiEndpoint"]
    pasSessionToken = requests.get(apiEndpoint + '/pas/login',
                                auth=(config.cybr["pasAdminUsername"], config.cybr["pasAdminPassword"]))
    conjurApiKey = requests.get(apiEndpoint + '/conjur/login',
                                auth=(config.cybr["conjurAdminUsername"], config.cybr["conjurAdminPassword"]))
    r = requests.post(url = apiEndpoint + "/provision?accReqId=" + provisionReqId, data = "")

    # update provisioned status of selected access request
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      cursor.execute("""UPDATE accessrequests SET provisioned=1 WHERE id=%s""", (provisionReqId,))
      DBLayer.dbConn.commit()
      cursor.close()
    except Error as e:
      print("provision: Error updating accessrequest provisioned status", e)

    self.buildUnprovisionedTree(self.unprovFrame)       # rebuild unprovisioned tree w/o approved access request
    self.buildProvisionedTree(self.provFrame)           # rebuild provisioned tree w/ provisioned access request

######################################
  def rejectApproved(self):
    try:
      provisionReqId = self.unprovTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # update provisioned status of selected access request 
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      cursor.execute("""UPDATE accessrequests SET rejected=1 WHERE id=%s""", (provisionReqId,))
      DBLayer.dbConn.commit()
      cursor.close()
    except Error as e:
      print("provision: Error updating accessrequest provisioned status", e)

    self.buildUnprovisionedTree(self.unprovFrame)	# rebuild unprovisioned tree w/o rejected access request

######################################
  def buildProvisionedTree(self,parent):
    # get access request data from db
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      query = "SELECT proj.name, appid.name, accreq.safe_name, accreq.environment, accreq.datetime, accreq.id " \
              "FROM " \
                "projects proj, " \
                "accessrequests accreq, " \
                "appidentities appid " \
              "WHERE " \
		"accreq.approved " \
		"AND accreq.provisioned " \
		"AND NOT accreq.revoked " \
		"AND accreq.project_id = proj.id " \
		"AND appid.accreq_id = accreq.id "
      cursor.execute(query)
      accessRequests = cursor.fetchall()
      DBLayer.dbConn.commit()
      cursor.close()
    except Error as e:
      print("Error while connecting to MySQL", e)

    # get max column widths for each column
    maxwidths = [0,0,0,0,0,0]
    for req in accessRequests:
      for i in range(len(self.cols)):
        if len(str(req[i])) > maxwidths[i]:
          maxwidths[i] = len(str(req[i]))

    if self.provTree is not None:
      self.provTree.destroy()

    self.provTree = ttk.Treeview(parent, height=5, columns=self.cols)
    self.provTree['show'] = 'headings'
    self.provTree.column('#0',minwidth=0,width=0)	# minimize width of 'ghost' column
    for c in range(len(self.cols)):
      self.provTree.column(self.cols[c], width=maxwidths[c], anchor='center')
      self.provTree.heading(self.cols[c],text=self.cols[c])

    for req in accessRequests:
      self.provTree.insert('','end',value=req)

    #++++++++++++++++++++++++++++++++++++++
    # setup popup menu for revoking access
    self.provTree.popup_menu = tk.Menu(self.provTree, tearoff=0)
    self.provTree.popup_menu.add_command(label="Revoke Access", command=self.revokeAccess)
    self.provTree.popup_menu.add_separator()
    def do_popup(event):
      # display the popup menu
      try:
        self.provTree.popup_menu.selection = self.provTree.set(self.provTree.identify_row(event.y))
        self.provTree.popup_menu.post(event.x_root, event.y_root)
      finally:
        # make sure to release the grab (Tk 8.0a1 only)
        self.provTree.popup_menu.grab_release()
    self.provTree.bind("<Button-2>", do_popup)

    self.provTree.grid(column=0, row=1, sticky=(N, W, E, S),columnspan=3)

######################################
  def revokeAccess(self):
    try:
      revokeReqId= self.provTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # make REST call to revoke access request 
    apiEndpoint = config.cybr["apiEndpoint"]
    pasSessionToken = requests.get(apiEndpoint + '/pas/login',
				auth=(config.cybr["pasAdminUsername"], config.cybr["pasAdminPassword"]))
    conjurApiKey = requests.get(apiEndpoint + '/conjur/login',
				auth=(config.cybr["conjurAdminUsername"], config.cybr["conjurAdminPassword"]))
    r = requests.delete(url = apiEndpoint + "/provision?accReqId=" + revokeReqId, data = "")
    # update revoked status of selected access request
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      cursor.execute("""UPDATE accessrequests SET revoked=1 WHERE id=%s""", (revokeReqId,))
      DBLayer.dbConn.commit()
      cursor.close()
    except Error as e:
      print("revokeAccess: Error updating accessrequest revoked status", e)

    self.buildProvisionedTree(self.provFrame)           # rebuild provisioned tree w/o revoked access request

######################################
  def refresh(self, *args):
    self.buildUnapprovedTree(self.unapprFrame)
    self.buildUnprovisionedTree(self.unprovFrame)
    self.buildProvisionedTree(self.provFrame) 

######################################
  def exit(self, *args):
    DBLayer.dbClose()
    sys.exit(0)
