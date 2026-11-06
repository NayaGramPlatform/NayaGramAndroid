package org.nayagram.platform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class NayaArchiveInspectorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File zipWith(String fileName, String... entryNames) throws IOException {
        File file = new File(tmp.getRoot(), fileName);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            for (String entry : entryNames) {
                out.putNextEntry(new ZipEntry(entry));
                out.write("x".getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return file;
    }

    private File rawFile(String fileName, byte[] bytes) throws IOException {
        File file = new File(tmp.getRoot(), fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
        return file;
    }

    @Test
    public void renamedApkWithManifestAndDexIsDetected() throws IOException {
        File renamed = zipWith("invoice.docx", "AndroidManifest.xml", "classes.dex", "META-INF/CERT.RSA");
        assertTrue(NayaArchiveInspector.containsAndroidPayload(renamed));
        assertTrue(NayaArchiveInspector.shouldBlockIncomingPayload(true, false, renamed));
    }

    @Test
    public void multidexPayloadIsDetected() throws IOException {
        File apk = zipWith("photo.pdf", "classes2.dex");
        assertTrue(NayaArchiveInspector.containsAndroidPayload(apk));
    }

    @Test
    public void docxIsNotBlocked() throws IOException {
        File docx = zipWith("report.docx", "[Content_Types].xml", "_rels/.rels", "word/document.xml");
        assertFalse(NayaArchiveInspector.containsAndroidPayload(docx));
        assertFalse(NayaArchiveInspector.shouldBlockIncomingPayload(true, false, docx));
    }

    @Test
    public void xlsxIsNotBlocked() throws IOException {
        File xlsx = zipWith("sheet.xlsx", "[Content_Types].xml", "xl/workbook.xml", "xl/worksheets/sheet1.xml");
        assertFalse(NayaArchiveInspector.containsAndroidPayload(xlsx));
        assertFalse(NayaArchiveInspector.shouldBlockIncomingPayload(true, false, xlsx));
    }

    @Test
    public void plainZipOfPhotosIsNotPayload() throws IOException {
        File photos = zipWith("holiday.jpg.zip", "a.jpg", "b.png");
        assertFalse(NayaArchiveInspector.containsAndroidPayload(photos));
    }

    @Test
    public void manifestInsideASubfolderIsNotTheRootPayload() throws IOException {
        File nested = zipWith("project.zip", "docs/AndroidManifest.xml");
        assertFalse(NayaArchiveInspector.containsAndroidPayload(nested));
    }

    @Test
    public void plainTextNamedAsDexIsNotBlocked() throws IOException {
        File text = rawFile("classes.dex", "hello, this is only text".getBytes(StandardCharsets.UTF_8));
        assertFalse(NayaArchiveInspector.containsAndroidPayload(text));
    }

    @Test
    public void corruptZipWithSignatureDoesNotThrow() throws IOException {
        File broken = rawFile("broken.apk", new byte[]{'P', 'K', 3, 4, 'g', 'a', 'r', 'b'});
        assertFalse(NayaArchiveInspector.containsAndroidPayload(broken));
    }

    @Test
    public void missingOrEmptyFileIsNotBlocked() throws IOException {
        File missing = new File(tmp.getRoot(), "missing.apk");
        File empty = rawFile("empty.apk", new byte[0]);
        assertFalse(NayaArchiveInspector.containsAndroidPayload(missing));
        assertFalse(NayaArchiveInspector.containsAndroidPayload(empty));
        assertFalse(NayaArchiveInspector.containsAndroidPayload(null));
    }

    @Test
    public void shieldOffByDefaultLetsApkThrough() throws IOException {
        File apk = zipWith("app.apk", "AndroidManifest.xml", "classes.dex");
        assertFalse(NayaArchiveInspector.shouldBlockIncomingPayload(false, false, apk));
    }

    @Test
    public void ownFileIsNeverBlockedEvenWhenShieldIsOn() throws IOException {
        File apk = zipWith("mine.apk", "AndroidManifest.xml", "classes.dex");
        assertFalse(NayaArchiveInspector.shouldBlockIncomingPayload(true, true, apk));
    }
}
