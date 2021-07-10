#!/bin/bash
source ~/.bashrc
export PATH="/usr/local/opt/ant@1.9/bin:$PATH"
ant "$@"
