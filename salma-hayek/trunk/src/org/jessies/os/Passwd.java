package org.jessies.os;

/**
 * Access to the POSIX user database.
 * http://www.opengroup.org/onlinepubs/000095399/basedefs/pwd.h.html
 */
public class Passwd {
    private String pw_name;
    private int pw_uid;
    private int pw_gid;
    private String pw_dir;
    private String pw_shell;
    
    private Passwd(String pw_name, int pw_uid, int pw_gid, String pw_dir, String pw_shell) {
        this.pw_name = pw_name;
        this.pw_uid = pw_uid;
        this.pw_gid = pw_gid;
        this.pw_dir = pw_dir;
        this.pw_shell = pw_shell;
    }
    
    /** User's login name. */
    public String pw_name() { return pw_name; }
    
    /** Numerical user id. */
    public int pw_uid() { return pw_uid; }
    
    /** Numerical group id. */
    public int pw_gid() { return pw_gid; }
    
    /** Initial working directory. */
    public String pw_dir() { return pw_dir; }
    
    /** Program to use as shell. */
    public String pw_shell() { return pw_shell; }
    
    /**
     * Returns a string representation of this object for debugging purposes.
     */
    public String toString() {
        return "Passwd[pw_name=" + pw_name + ",pw_uid=" + pw_uid + ",pw_gid=" + pw_gid + ",pw_dir=" + pw_dir + ",pw_shell=" + pw_shell + "]";
    }
    
    /**
     * Looks up the user with the login name 'name'.
     * Returns a Passwd object on success, null on failure.
     */
    public static Passwd fromName(String name) {
        return PosixJNI.getpwnam(name);
    }
    
    /**
     * Looks up the user with the user id 'uid'.
     * Returns a Passwd object on success, null on failure.
     */
    public static Passwd fromUid(int uid) {
        return PosixJNI.getpwuid(uid);
    }
}
