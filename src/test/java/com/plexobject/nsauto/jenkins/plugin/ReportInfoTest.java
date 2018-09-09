package com.plexobject.nsauto.jenkins.plugin;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class ReportInfoTest {

    @Test
    public void testParse() throws URISyntaxException, IOException, ParseException {
        Path path = Paths.get(getClass().getClassLoader().getResource("upload.json").toURI());
        byte[] fileBytes = Files.readAllBytes(path);
        String json = new String(fileBytes);
        ReportInfo info = ReportInfo.fromJson(json);
        Assert.assertEquals("d2fc75a0-b2d8-48f5-a70d-eded118f3065", info.getAccount());
    }

}
