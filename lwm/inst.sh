#!/bin/sh

dir=`dirname $0`

cd $dir
dir=`/bin/pwd`
cd $HOME/bin				# at home
if [ "X$cputype" != "X" ]; then
	cd $cputype 2> /dev/null	# at work
fi
mv lwm lwm_o
cp $dir/lwm .
strip lwm

