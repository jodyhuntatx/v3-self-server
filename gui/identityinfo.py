from tkinter import *
from tkinter import ttk

class IdentityInfo:

  def __init__(self, parent):
    identityFrame = ttk.LabelFrame(parent, text='Identity Info',padding="3 3 12 12")
    identityFrame.grid(column=0, row=0, sticky=(N, W, E, S))
    identityFrame.columnconfigure(0, weight=1)
    identityFrame.rowconfigure(0, weight=1)

    self.identity = StringVar()
    identity_entry = ttk.Entry(identityFrame, width=20, textvariable=self.identity)
    identity_entry.grid(column=1, row=1, sticky=(W, E))
    ttk.Label(identityFrame, text="Conjur Identity (host) Name").grid(column=0, row=1, sticky=W)

  def print(self, *args):
    try:
      print("\"identities\": [")
      print("  {\"identity\": \"!host "+self.identity.get()+"\"},")
      print("]")
    except ValueError:
      pass

