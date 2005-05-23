package e.toys.world;

import java.util.*;
import e.util.*;

public class Gazetteer {
    private static Gazetteer instance;
    static {
        instance = new Gazetteer();
    }
    
    private ArrayList places;
    
    private Gazetteer() {
        readPlaceInformation();
    }
    
    public static Gazetteer getInstance() {
        return instance;
    }
    
    public Place get(int i) {
        return (Place) places.get(i);
    }
    
    public int size() {
        return places.size();
    }
    
    private void readPlaceInformation() {
        long startTime = System.currentTimeMillis();
        final String filename = "~/Desktop/popdata/towns.txt";
        if (FileUtilities.exists(filename)) {
            ArrayList<Place> result = new ArrayList<Place>();
            String[] lines = StringUtilities.readLinesFromFile(filename);
            for (int i = 1; i < lines.length; ++i) {
                String line = lines[i];
                // Basel   Basel-Stadt     Switzerland     ch      R       164700  47.57   7.58
                String[] fields = line.split("\t");
                
                // Filter out some of the dodgy data.
                if (fields.length != 8 || line.contains("&deg;")) {
                    System.err.println(line);
                    continue;
                }
                
                // Only the supposed country capitals and regional capitals.
                char type = fields[4].charAt(0);
                if (type == 'N') {
                    continue;
                }
                
                int population = Integer.parseInt(fields[5]);
                Place place = new Place();
                place.name = fields[0] + ", " + fields[2];
                place.latitude = Double.parseDouble(fields[6]);
                place.longitude = Double.parseDouble(fields[7]);
                result.add(place);
            }
            this.places = result;
            System.out.println("Places: " + places.size());
            System.out.println("Took: " + (System.currentTimeMillis() - startTime) + "ms");
        } else {
            System.err.println("Download population data from http://www.world-gazetteer.com/st/popdata.zip");
        }
    }
}
