package e.toys.world;

public class Place {
    String name;
    double latitude;
    double longitude;
    
    public final double distanceFrom(double lat, double lon) {
        double dx = lon - this.longitude;
        double dy = lat - this.latitude;
        return Math.sqrt(dx*dx + dy*dy);
    }
}
