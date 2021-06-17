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
    mainframe = ttk.Frame(parent, width=500, padding="12 12 12 12")
    mainframe.grid(column=0, row=1, sticky=(N, W, E, S), columnspan=3)

    ttk.Button(parent, text="Submit", command=self.submit, style='Cybr.TButton').grid(column=3, row=4, sticky=E)
    parent.bind("<Return>", self.submit)

    accreqFrame = ttk.LabelFrame(parent, text='', padding="3 3 12 12")
    accreqFrame.grid(column=0, row=3, sticky=(N, W, E, S), columnspan=3)
    accreqFrame.columnconfigure(0, weight=1)
    accreqFrame.rowconfigure(0, weight=1)

    # get access request data from db
    try:
      cursor = DBLayer.dbConn.cursor(buffered=True)
      query = "SELECT * from projects"
      cursor.execute(query)
      accessRequests = cursor.fetchall()
    except Error as e:
      print("Error while connecting to MySQL", e)

    # get max column widths for each column
    maxIdWidth = 3
    maxNameWidth = 0
    maxAdminWidth = 0
    for req in accessRequests:
      if len(req[1]) > maxNameWidth:
          maxNameWidth = len(req[1])
      if len(req[2]) > maxAdminWidth:
          maxAdminWidth = len(req[2])

    cols=('Id','Project','Admin')
    widths=(maxIdWidth,maxNameWidth,maxAdminWidth)
    self.tview = ttk.Treeview(accreqFrame, height=10, columns=cols)
    self.tview['show'] = 'headings'
    self.tview.column('#0',minwidth=0,width=0)	# minimize width of 'ghost' column
    for c in range(len(cols)):
      self.tview.column(cols[c], width=widths[c], anchor='center')
      self.tview.heading(cols[c],text=cols[c])

    for req in accessRequests:
      self.tview.insert('','end',value=req)
    self.tview.grid(column=0, row=0, sticky=(N, W, E, S),columnspan=3)

    for child in parent.winfo_children():
        child.grid_configure(padx=10, pady=10)

######################################
  def submit(self, *args):
    DBLayer.dbClose()
    sys.exit(0)
