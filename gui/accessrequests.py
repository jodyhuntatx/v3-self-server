import os
import sys
import time
import datetime
import subprocess
from tkinter import *
from tkinter import ttk
import tkinter as tk

import requests
import config
import json

class AccessRequests:

######################################
  def __init__(self, parent):
    mainframe = ttk.Frame(parent, width=1000, padding="12 12 12 12")
    mainframe.grid(column=0, row=1, sticky=(N, W, E, S), columnspan=3)

    accreqLabel = ttk.Label(parent,text='Access Requests',font=('Helvetica bold',32),anchor='center')
    accreqLabel.grid(column=0, row=1, sticky=(N, W, E, S), columnspan=3)

    # cols must correspond to fields returned from calls to AppGovDB.get()
    self.cols=('Project','AppId','Safe','Environment','DateTimeSubmitted','RequestId')
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
    # get unapproved access request data 
    apiEndpoint = config.cybr["apiEndpoint"]
    accReqJson = json.loads(requests.get(apiEndpoint + '/appgovdb?filter=unapproved').content)
    unapprList = accReqJson["unapproved"]
    unapprValues = []
    if unapprList:
      reqKeyList = tuple(unapprList[0])   # get list of keys in json record
      for req in unapprList:              # for each key in each unapproved access request
        reqValues = []
        for key in reqKeyList:
          reqValues.append(req[key])      # append just values
        unapprValues.append(reqValues)

    # get max column widths for each column
    maxwidths = [20,20,20,20,20,20]

    if self.unapprTree is not None:
      self.unapprTree.destroy()

    self.unapprTree = ttk.Treeview(parent, height=5, columns=self.cols)
#    self.unapprScrollbar = Scrollbar(self.unapprTree, orient=VERTICAL, command=self.unapprTree.yview)
#    self.unapprTree.configure(yscrollcommand=self.unapprScrollbar.set)
#    self.unapprScrollbar.grid(row=1, column=7, sticky='ns')

    self.unapprTree['show'] = 'headings'
    for c in range(len(self.cols)):
      self.unapprTree.column(self.cols[c], width=maxwidths[c], anchor='center')
      self.unapprTree.heading(self.cols[c],text=self.cols[c])

    for req in unapprValues:
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
      selectedReqId = self.unapprTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # update approval status of selected access request 
    apiEndpoint = config.cybr["apiEndpoint"] + '/appgovdb?accReqId=' + selectedReqId + '&status=approved'
    accStatusChangeResponse = requests.put(apiEndpoint).content

    self.buildUnapprovedTree(self.unapprFrame)		# rebuild unapproved tree w/o approved access request
    self.buildUnprovisionedTree(self.unprovFrame)	# rebuild unprovisioned tree w/ approved access request

######################################
  def rejectSubmitted(self):
    try:
      selectedReqId = self.unapprTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # update rejected status of selected access request
    apiEndpoint = config.cybr["apiEndpoint"] + '/appgovdb?accReqId=' + selectedReqId + '&status=rejected'
    accStatusChangeResponse = requests.put(apiEndpoint).content

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
    apiEndpoint = config.cybr["apiEndpoint"]
    accReqJson = json.loads(requests.get(apiEndpoint + '/appgovdb?filter=unprovisioned').content)
    unprovList = accReqJson["unprovisioned"]
    unprovValues = []
    if unprovList:
      reqKeyList = tuple(unprovList[0])	# get list of keys in json record
      for req in unprovList:		# for each key in each unprovisioned access request
        reqValues = []
        for key in reqKeyList:
          reqValues.append(req[key])	# append just values
        unprovValues.append(reqValues)

    # get max column widths for each column
    maxwidths = [20,20,20,20,20,20]

    if self.unprovTree is not None:
      self.unprovTree.destroy()

    self.unprovTree = ttk.Treeview(parent, height=5, columns=self.cols)
    self.unprovTree['show'] = 'headings'
    self.unprovTree.column('#0',minwidth=0,width=0)	# minimize width of 'ghost' column
    for c in range(len(self.cols)):
      self.unprovTree.column(self.cols[c], width=maxwidths[c], anchor='center')
      self.unprovTree.heading(self.cols[c],text=self.cols[c])

    for req in unprovValues:
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
      selectedReqId = self.unprovTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # make REST call to provision access request from DB
    r = requests.post(url = config.cybr["apiEndpoint"] + "/provision?accReqId=" + selectedReqId, data = "")

    self.buildUnprovisionedTree(self.unprovFrame)       # rebuild unprovisioned tree w/o approved access request
    self.buildProvisionedTree(self.provFrame)           # rebuild provisioned tree w/ provisioned access request

######################################
  def rejectApproved(self):
    try:
      selectedReqId = self.unprovTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # update rejected status of selected access request 
    apiEndpoint = config.cybr["apiEndpoint"] + '/appgovdb?accReqId=' + selectedReqId + '&status=rejected'
    accStatusChangeResponse = requests.patch(apiEndpoint).content

    self.buildUnprovisionedTree(self.unprovFrame)	# rebuild unprovisioned tree w/o rejected access request

######################################
  def buildProvisionedTree(self,parent):
    # get access request data from db
    apiEndpoint = config.cybr["apiEndpoint"]
    accReqJson = json.loads(requests.get(apiEndpoint + '/appgovdb?filter=provisioned').content)
    provList = accReqJson["provisioned"]
    provValues = []
    if provList:
      reqKeyList = tuple(provList[0])   # get list of keys in json record
      for req in provList:              # for each key in each provisioned access request
        reqValues = []
        for key in reqKeyList:
          reqValues.append(req[key])      # append just values
        provValues.append(reqValues)

    # get max column widths for each column
    maxwidths = [20,20,20,20,20,20]

    if self.provTree is not None:
      self.provTree.destroy()

    self.provTree = ttk.Treeview(parent, height=5, columns=self.cols)
    self.provTree['show'] = 'headings'
    self.provTree.column('#0',minwidth=0,width=0)	# minimize width of 'ghost' column
    for c in range(len(self.cols)):
      self.provTree.column(self.cols[c], width=maxwidths[c], anchor='center')
      self.provTree.heading(self.cols[c],text=self.cols[c])

    for req in provValues:
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
      selectedReqId= self.provTree.popup_menu.selection['RequestId']
    except LookupError:
      return

    # revoke access request 
    r = requests.delete(url = config.cybr["apiEndpoint"] + "/provision?accReqId=" + selectedReqId, data = "")

    self.buildProvisionedTree(self.provFrame)           # rebuild provisioned tree w/o revoked access request

######################################
  def refresh(self, *args):
    self.buildUnapprovedTree(self.unapprFrame)
    self.buildUnprovisionedTree(self.unprovFrame)
    self.buildProvisionedTree(self.provFrame) 

######################################
  def exit(self, *args):
    sys.exit(0)
