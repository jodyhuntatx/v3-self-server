#!/bin/bash
echo "Size before cleaning:" $(du -sh .)
pushd plugin
./ant.sh clean
popd
echo "Size after cleaning:" $(du -sh .)
git add .
git commit -m "checkpoint commit"
git push origin main
