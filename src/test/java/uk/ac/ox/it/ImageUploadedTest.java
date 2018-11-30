package uk.ac.ox.it;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.TreeSet;

public class ImageUploadedTest {

    @Test
    public void testSorting() {
        ImageUploader.KeyFirstComparator comparator = new ImageUploader.KeyFirstComparator();
        Set<String> tree = new TreeSet<>(comparator);
        tree.add("a");
        tree.add("b");
        tree.add("key");
        tree.add("c");
        tree.add("z");
        // Key should always be at the start
        Assert.assertThat(tree, CoreMatchers.hasItems("key", "a", "b", "c", "z"));
    }

}
