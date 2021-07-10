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

class Governance:

######################################
  def __init__(self, parent):
    mainframe = ttk.Frame(parent, width=1000, padding="12 12 12 12")
    mainframe.grid(column=0, row=1, sticky=(N, W, E, S), columnspan=3)

    accreqLabel = ttk.Label(parent,text='Identity Access Review',font=('Helvetica bold',32),anchor='center')
    accreqLabel.grid(column=0, row=1, sticky=(N, W, E, S), columnspan=3)

    # cols must correspond to fields returned from calls to Governance.get()
    self.cols=('Project','AppId','ResourceType','ResourceName','Username','SafeName')
    self.governTree = None
    self.unprovTree = None
    self.provTree = None

    #################################
    # Identity Access
    #################################
    self.governFrame = ttk.LabelFrame(parent, text='', padding="3 3 12 12")
    self.governFrame.grid(column=0, row=2, sticky=(N, W, E, S), columnspan=3)
    self.governFrame.columnconfigure(0, weight=1)
    self.governFrame.rowconfigure(0, weight=1)
    self.buildIdentityAccessTree(self.governFrame)

    # Refresh button at middle right of main window
    ttk.Button(parent, text="Refresh", command=self.refresh, style='Cybr.TButton').grid(column=3, row=1, sticky=E)

    # Exit button at bottom right of main window
    ttk.Button(parent, text="Exit", command=self.exit, style='Cybr.TButton').grid(column=3, row=2, sticky=E)
    parent.bind("<Return>", self.exit)

    for child in parent.winfo_children():
        child.grid_configure(padx=10, pady=10)

######################################
  # Build identity access treeview
  def buildIdentityAccessTree(self,parent):
    # get identity access json structure, convert to list of lists of values for treeview
    apiEndpoint = config.cybr["apiEndpoint"]
    projectJson = json.loads(requests.get(apiEndpoint + '/governance').content)
    projectDict = projectJson["projects"]	# get dict of projects
    projectsValues = []
    if projectDict:
      for pr in projectDict:              # for each project
        prValues = []
        prValues.append(pr["projectName"])
        idList = pr["identities"]	  # get dict of ids
        idKeyList = tuple(idList[0])   # get list of keys in json record
        for id in idList:		  # for each id
          idValues = [""]		  # initialize with blank value for first column
          for key in idKeyList:		  # get the values for each key
            idValues.append(id[key])      # append just values
          prValues.append(idValues)
        projectsValues.append(prValues)

    # get max column widths for each column
    maxwidths = [0,15,15,15,15,15,15]

    if self.governTree is not None:
      self.governTree.destroy()

    self.governTree = ttk.Treeview(parent, height=10, columns=self.cols)
    self.governTree.column("#0", minwidth=0, width=10, stretch=NO)

    self.governTree['show'] = 'tree headings'
    for c in range(len(self.cols)):
      self.governTree.column(self.cols[c], width=maxwidths[c], anchor='center')
      self.governTree.heading(self.cols[c],text=self.cols[c])

    for pr in projectsValues:
      self.governTree.insert('','end',pr[0],value=pr[0])
      for i in range(1,len(pr)):
        self.governTree.insert(pr[0],'end',value=pr[i])

    self.governTree.grid(column=0, row=0, sticky=(N, W, E, S),columnspan=3)

######################################
  def refresh(self, *args):
    self.buildIdentityAccessTree(self.governFrame)

######################################
  def exit(self, *args):
    sys.exit(0)
