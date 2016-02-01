package e.debug;

public interface HungAwtExitMBean {
    public int getExtantFrameCount();
    public String[] getDisplayableFrames();
    public String[] getSwingTimers();
}
