package devsummit2015.advancedtopics;
import java.util.Arrays;

// Each ClusterableLocation passed in via the "ClusterableLocations" array should be unique.
// If duplicates are passed in the returned polygon might not be a convex hull.
// Credits: http://www.algorithmist.com/index.php/Monotone_Chain_Convex_Hull.java
public class ConvexHullGenerator {
  public static ClusterableLocation[] generateHull(ClusterableLocation[] ClusterableLocations) {
    int n = ClusterableLocations.length;
    Arrays.sort(ClusterableLocations);
    ClusterableLocation[] ans = new ClusterableLocation[2 * n]; // In between we
                                                                // may have a 2n
                                                                // ClusterableLocations
    int k = 0;
    int start = 0; // start is the first insertion ClusterableLocation

    for (int i = 0; i < n; i++) // Finding lower layer of hull
    {
      ClusterableLocation p = ClusterableLocations[i];
      while (k - start >= 2 && p.sub(ans[k - 1]).cross(p.sub(ans[k - 2])) > 0)
        k--;
      ans[k++] = p;
    }

    k--; // drop off last ClusterableLocation from lower layer
    start = k;

    for (int i = n - 1; i >= 0; i--) // Finding top layer from hull
    {
      ClusterableLocation p = ClusterableLocations[i];
      while (k - start >= 2 && p.sub(ans[k - 1]).cross(p.sub(ans[k - 2])) > 0)
        k--;
      ans[k++] = p;
    }
    k--; // drop off last ClusterableLocation from top layer

    return Arrays.copyOf(ans, k); // convex hull is of size k
  }
}
