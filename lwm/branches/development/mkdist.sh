#!/bin/sh

DISTFILES="AUTHORS BUGS COPYING ChangeLog INSTALL Imakefile README TODO client.c cursor.c disp.c error.c ewmh.c ewmh.h lwm.c lwm.h lwm.man manage.c mouse.c no_xmkmf_makefile resource.c session.c shape.c"

VERSION=`cat VERSION`
mkdir /tmp/lwm-$VERSION

for f in `echo $DISTFILES`; do
	if [ -f $f.dist ]; then
		cp $f.dist /tmp/lwm-$VERSION/$f
	else
		cp $f /tmp/lwm-$VERSION/$f
	fi
done

(cd /tmp ; tar zcvf /usr/james/ftp/lwm-$VERSION.tar.gz lwm-$VERSION)
cp ChangeLog /usr/james/www/jfc/software/lwm-stable-ChangeLog.txt
cp lwm.1x.html /usr/james/www/jfc/software/lwm-stable.1x.html

rm -rf /tmp/lwm-$VERSION
