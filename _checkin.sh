#!/bin/bash
if [[ "$#" != 1 ]]; then
  echo "Usage: $0 <branch>"
  exit -1
fi
echo "Size before cleaning:" $(du -sh .)
./ant.sh clean		# delete built binaries & caches
echo "Size after cleaning:" $(du -sh .)
git add .
git commit -m "checkpoint commit"
git push origin $1
./ant.sh publish	# rebuild after cleaning
