Note that `lock` relies on passwords being stored in /etc/passwd, which hasn't
been true since the 1990s (when it was written). To be useful today, it would
need to be rewritten to use PAM.
