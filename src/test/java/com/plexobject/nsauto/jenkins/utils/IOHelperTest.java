package com.plexobject.nsauto.jenkins.utils;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.plexobject.nsauto.jenkins.utils.IOHelper;

public class IOHelperTest {
    private static final String GROUP = "aaaa";
    private static final String API = "eyJhbGciOi";
    private static final String file = "apkpure_app_887.apk";

    // @Test
    public void testUpload() throws Exception {
        String json = IOHelper.upload("https://lab-api.nowsecure.com/build/?group=" + GROUP, API, file);
        Assert.assertNotNull(json);
    }

    // @Test
    public void testGet() throws Exception {
        String json = IOHelper.get("https://lab-api.nowsecure.com/app/android/pkg/assessment/task/results", API);
        Assert.assertNotNull(json);
    }

    @Test
    public void testFind() throws Exception {
        new File("/tmp/test.out").createNewFile();
        File file = IOHelper.find(new File("/tmp"), "test.out");
        Assert.assertNotNull(file);
    }

}
