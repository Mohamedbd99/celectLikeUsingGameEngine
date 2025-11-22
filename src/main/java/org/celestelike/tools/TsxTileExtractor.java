package org.celestelike.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TsxTileExtractor {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java TsxTileExtractor <path-to-tsx> [output-dir]");
            return;
        }

        Path tsxPath = Paths.get(args[0]);

        Path outputDir;
        if (args.length >= 2) {
            outputDir = Paths.get(args[1]);
        } else {
            outputDir = tsxPath.getParent().resolve("tiles_out");
        }
        Files.createDirectories(outputDir);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(tsxPath.toFile());

        Element root = document.getDocumentElement();

        int tileWidth = Integer.parseInt(root.getAttribute("tilewidth"));
        int tileHeight = Integer.parseInt(root.getAttribute("tileheight"));

        String tileCountAttr = root.getAttribute("tilecount");
        String columnsAttr = root.getAttribute("columns");

        NodeList images = root.getElementsByTagName("image");
        if (images.getLength() == 0) {
            System.err.println("No <image> tag found in TSX file.");
            return;
        }

        Element imageElement = (Element) images.item(0);
        String imageSource = imageElement.getAttribute("source");

        Path imagePath = tsxPath.getParent().resolve(imageSource).normalize();

        System.out.println("TSX file:   " + tsxPath.toAbsolutePath());
        System.out.println("Image file: " + imagePath.toAbsolutePath());

        BufferedImage spriteSheet = ImageIO.read(imagePath.toFile());
        if (spriteSheet == null) {
            System.err.println("Failed to load image: " + imagePath);
            return;
        }

        int imageWidth = spriteSheet.getWidth();
        int imageHeight = spriteSheet.getHeight();

        int columns;
        if (!columnsAttr.isEmpty()) {
            columns = Integer.parseInt(columnsAttr);
        } else {
            columns = imageWidth / tileWidth;
        }

        int tileCount;
        if (!tileCountAttr.isEmpty()) {
            tileCount = Integer.parseInt(tileCountAttr);
        } else {
            int rows = imageHeight / tileHeight;
            tileCount = columns * rows;
        }

        System.out.println("tilewidth:  " + tileWidth);
        System.out.println("tileheight: " + tileHeight);
        System.out.println("columns:    " + columns);
        System.out.println("tilecount:  " + tileCount);
        System.out.println("Output dir: " + outputDir.toAbsolutePath());

        for (int i = 0; i < tileCount; i++) {
            int col = i % columns;
            int row = i / columns;

            int x = col * tileWidth;
            int y = row * tileHeight;

            if (x + tileWidth > imageWidth || y + tileHeight > imageHeight) {
                System.out.println("Stopping at tile index " + i + " (out of bounds).");
                break;
            }

            BufferedImage tile = spriteSheet.getSubimage(x, y, tileWidth, tileHeight);
            String fileName = String.format("tile_%04d.png", i);
            File outFile = outputDir.resolve(fileName).toFile();

            ImageIO.write(tile, "png", outFile);
        }

        System.out.println("Done.");
    }
}
