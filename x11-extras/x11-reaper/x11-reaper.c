#include    <stdio.h>
#include    <stdlib.h>
#include    <signal.h>
#include    <errno.h>

#include    <unistd.h>
#include    <sys/types.h>
#include    <sys/wait.h>

#include    <X11/X.h>
#include    <X11/Xos.h>
#include    <X11/Xlib.h>
#include    <X11/Xutil.h>
#include    <X11/Xatom.h>

/* ARGSUSED */
static void
reap(int s) {
    (void) s;
    signal(SIGCHLD, reap);
    wait(0);
}

int
main(int argc, char *argv[])
{
    Display    *dpy;
    Window    root;
    Atom    enh_terminator;
    int        terminate;
    XEvent    ev;

    signal(SIGCHLD, reap);

    /*
     * See what we're expected to do. We can either terminate a session or
     * wait for one to be terminated.
     */
    (void) argv;
    switch (argc) {
    case 1:    /* Terminate the current session. */
        terminate = True;
        break;
    case 2:    /* Wait for signal of termination. */
        terminate = False;
        break;
    default:
        fprintf(stderr, "syntax: %s [daemon]\n", argv[0]);
        return EXIT_FAILURE;
    }

    /*
     * Open a connection to the X server.
     */
    dpy = XOpenDisplay("");
    if (dpy == 0) {
        fprintf(stderr, "%s: can't connect to X server.\n", argv[0]);
        return EXIT_FAILURE;
    }

    /*
     * Find the root window.
     */
    root = DefaultRootWindow(dpy);

    /*
     * Internalize the _ENH_TERMINATOR atom. If we intend terminating a
     * session, then we want to fail if the server doesn't know about the
     * atom (because that means there cannot be a session).
     */
    enh_terminator = XInternAtom(dpy, "_ENH_TERMINATOR", terminate);
    if (enh_terminator == None) {
        fprintf(stderr, "%s: can't internalize atom.\n", argv[0]);
        return EXIT_FAILURE;
    }

    if (terminate) {
        /*
         * If we're here to terminate a session, set the _ENH_TERMINATOR
         * property and exit.
         */
        XChangeProperty(dpy, root, enh_terminator, XA_STRING, 8, PropModeReplace, (unsigned char *) "ttfn", strlen("ttfn"));
    } else {
        /*
         * If we're here to wait for termination, wait for termination... ;-)
         */

        XSelectInput(dpy, root, PropertyChangeMask);

        while (!terminate) {
            XNextEvent(dpy, &ev);
            if (ev.type == PropertyNotify && ev.xproperty.window == root &&
                ev.xproperty.atom == enh_terminator &&
                ev.xproperty.state == PropertyNewValue ) {
                /*
                 * See what the value is. Actually, we don't bother: it's
                 * not important so long as we don't implement authentication.
                 */
                terminate = True;

                /*
                 * Remove the property, in case we aren't being used to
                 * terminate the X server.
                 */
                XDeleteProperty(dpy, root, enh_terminator);
            }
        }
    }

    XCloseDisplay(dpy);
    return EXIT_SUCCESS;
}
