#!/bin/bash
# usage: make-new-project.rb <name>

svn_host=software@jessies.org
projects_dir=~/Projects

name=$1
if [ -d $projects_dir/$name ]; then
    echo "Project '$name' already exists!"
    exit 1
fi
if [ ! -f $projects_dir/edit/COPYING ]; then
    echo "Couldn't find a copy of the GPL!"
    exit 1
fi

echo "Creating new project '$name'..."

mkdir -p /tmp/$$
cd /tmp/$$

echo "Creating directories..."
mkdir -p $name
mkdir -p $name/src

echo "Creating Makefile..."
cat > $name/Makefile <<EOF
include ../salma-hayek/universal.make
EOF

echo "Adding GPL..."
cp $projects_dir/edit/COPYING $name/COPYING

echo "Creating a new Subversion repository..."
ssh $svn_host svnadmin create /home/software/svnroot/$name
ssh $svn_host chmod -R g+w /home/software/svnroot/$name/db
echo "Making the initial import..."
svn import $name svn+ssh://$svn_host/home/software/svnroot/$name -m 'New project, $name.'
echo "Checking back out..."
svn co svn+ssh://$svn_host/home/software/svnroot/$name $projects_dir/$name
cd $projects_dir/$name

echo "Telling Subversion to ignore generated files..."
svn propset svn:ignore "ChangeLog
ChangeLog.html
classes
.generated
$name.jar" .
echo "Getting Subversion to commit that change..."
svn update
echo "Prompting you to manually commit that change..."
checkintool

echo "Done!"
exit 0
