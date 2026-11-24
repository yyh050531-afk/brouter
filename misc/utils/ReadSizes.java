import java.io.*;
import java.net.*;

public class ReadSizes {

private static final int degrees = 5;
  private static final int row = 360/degrees;
  private static final int column =180/degrees;
  private static final int offset = 180;
  private static final int latOffset = 90;
  private static final int invalidTile = -1;
  private static int[] tileSizes = new int[row * column];
  protected static String baseNameForTile(int tileIndex) {
    int lon = (tileIndex % row) * degrees - offset;
    int lat = (tileIndex / row) * degrees - latOffset;
    String slon = lon < 0 ? "W" + (-lon) : "E" + lon;
    String slat = lat < 0 ? "S" + (-lat) : "N" + lat;
    return slon + "_" + slat;
  }


  private static int tileForBaseName(String basename) {
    String uname = basename.toUpperCase();
    int idx = uname.indexOf("_");
    if (idx < 0) return invalidTile;
    String slon = uname.substring(0, idx);
    String slat = uname.substring(idx + 1);
    int ilon = slon.charAt(0) == 'W' ? -Integer.valueOf(slon.substring(1)) :
      (slon.charAt(0) == 'E' ? Integer.valueOf(slon.substring(1)) : -1);
    int ilat = slat.charAt(0) == 'S' ? -Integer.valueOf(slat.substring(1)) :
      (slat.charAt(0) == 'N' ? Integer.valueOf(slat.substring(1)) : -1);
    if (ilon < -offset || ilon >= offset || ilon % degrees != 0) return invalidTile;
    if (ilat < -latOffset || ilat >= latOffset || ilat % degrees != 0) return invalidTile;
    return (ilon + offset) / degrees + row * ((ilat + latOffset) /degrees);
  }



  private static void scanExistingFiles(File dir) {
    String[] fileNames = dir.list();
    if (fileNames == null) return;
    String suffix = ".rd5";
    for (String fileName : fileNames) {
      if (fileName.endsWith(suffix)) {
        String basename = fileName.substring(0, fileName.length() - suffix.length());
        int tidx = tileForBaseName(basename);
        tileSizes[tidx] = (int) new File(dir, fileName).length();
      }
    }
  }

  // Extract segment information from directory listing on https://brouter.de/brouter/segments4/
  private static void scanTilesIndex(String tilesUrl) {
    try {
      URL url = new URL(tilesUrl);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");
      BufferedReader in = new BufferedReader(
        new InputStreamReader(con.getInputStream()));
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        parseAndUpdateTileSize(inputLine);
      }
      in.close();
    } catch (MalformedURLException e) {
      System.out.println("Invalid URL");
    } catch (ProtocolException e) {
      System.out.println("Unable to download segment index");
    } catch (IOException e) {
      System.out.println("Unable to download segment index");
    }
  }

  // Extract filename and size from each line in directory listing
  // Example (stripped multiple spaces): "<a href="E0_N10.rd5">E0_N10.rd5</a> 17-Oct-2021 01:03 9648604"
  private static void parseAndUpdateTileSize(String indexLine) {
    String suffix = ".rd5";

    if (!indexLine.contains(suffix)) {
      return;
    }

    String fileName = indexLine.substring(indexLine.indexOf('"') + 1, indexLine.lastIndexOf('"'));
    int fileSize = Integer.parseInt(indexLine.substring(indexLine.lastIndexOf(" ") + 1));

    String basename = fileName.substring(0, fileName.length() - suffix.length());
    int tidx = tileForBaseName(basename);
    tileSizes[tidx] = fileSize;
  }

  public static void main(String[] args) {
    if (args[0].startsWith("http")) {
      scanTilesIndex(args[0]);
    } else {
      scanExistingFiles(new File(args[0]));
    }
    StringBuilder sb = new StringBuilder();
    for (int tidx = 0; tidx < tileSizes.length; tidx++) {
      if ((tidx % 12) == 0) sb.append("\n        ");
      sb.append(tileSizes[tidx]).append(',');
    }
    System.out.println(sb);
  }

} 
