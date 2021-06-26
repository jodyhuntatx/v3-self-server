#!/bin/bash
echo "Size before cleaning:" $(du -sh .)
./ant.sh clean
echo "Size after cleaning:" $(du -sh .)
git add .
git commit -m "checkpoint commit"
git push origin main
./ant.sh publish
