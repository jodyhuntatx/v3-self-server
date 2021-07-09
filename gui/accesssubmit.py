import os
import sys
import time
import datetime
import subprocess
from tkinter import *
from tkinter import ttk
import requests

from projectinfo import *
from identityinfo import *
from accountinfo import *
import config

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

    self.projectInfo.project_entry.focus()

    for child in parent.winfo_children():
        child.grid_configure(padx=10, pady=10)

  ######################################
  def submit(self, *args):
    self.write_to_db()
    sys.exit(0)

  ##############################
  # Writes form input variables to MySQL database
  def write_to_db(self, *args):
    approved = 0
    if self.projectInfo.env.get() == 'dev':	# auto-approve requests for dev environments
      approved = 1
    accReqParms= "{" \
                + "\"projectName\":\"" + self.projectInfo.project.get() + "\"," \
                + "\"requestor\":\"" + self.projectInfo.requestor.get() + "\"," \
                + "\"approved\":" + str(approved) + "," \
                + "\"environment\":\""  + self.projectInfo.env.get() + "\"," \
                + "\"pasVaultName\":\"" + config.cybr["pasVaultName"] + "\"," \
                + "\"pasSafeName\":\"" + self.projectInfo.safe.get() + "\"," \
                + "\"pasCpmName\":\"" + config.cybr["pasCpmName"] + "\"," \
                + "\"pasLobName\":\"" + config.cybr["pasLobName"] + "\"," \
                + "\"appIdName\":\"" + self.identityInfo.identity.get() + "\"," \
                + "\"appAuthnMethod\":\"authn-k8s\"" \
                + "}"
    r = requests.post(url = config.cybr["apiEndpoint"]+"/appgovdb", data = accReqParms)
