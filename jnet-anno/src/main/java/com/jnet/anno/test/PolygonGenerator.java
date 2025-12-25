package com.jnet.anno.test;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author mugw
 * @version 1.0
 * @description 生成多边形
 * @date 2024/8/6 16:11:07
 */
public class PolygonGenerator {

    public static void main(String[] args) {
        int numberOfPoints = 2000; // Number of points for the polygon

        // Generate the points
        //List<Coordinate> coordinates = generatePoints(numberOfPoints);

        // Create the polygon
        //Polygon polygon = createPolygon(coordinates);

        // Print the WKT representation of the polygon
        //System.out.println(polygon.toText());


    }

    /**
     * 生成随机多边形(相交)
     * @param numberOfVertices
     * @param maxX
     * @param maxY
     * @return
     */
    public static List<Coordinate> generateRandomPolygon(int numberOfVertices, int maxX, int maxY) {
        List<Coordinate> coordinates = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numberOfVertices; i++) {
            int x = random.nextInt(maxX);
            int y = random.nextInt(maxY);
            Coordinate coordinate = new Coordinate(x, y);
            coordinates.add(coordinate);
        }
        // 添加第一个点作为最后一个点，形成闭合的多边形
        coordinates.add(coordinates.get(0));
        return coordinates;
    }

    public static int generateRandomNumber(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    public static List<Coordinate> generatePoints(){
        return generatePoints(generateRandomNumber(400, 600),generateRandomNumber(0, 2000),generateRandomNumber(0, 2000));
    }

    public static List<Coordinate> generatePoints(int numberOfPoints, int offsetX, int offsetY) {
        List<Coordinate> coordinates = new ArrayList<>();

        double step = 360.0 / numberOfPoints;

        for (int i = 0; i < numberOfPoints; i++) {
            double angle = Math.toRadians(i * step);
            double x = convertTo3(50 * Math.cos(angle))+offsetX;
            double y = convertTo3(50 * Math.sin(angle))+offsetY;
            if (x!=0){
                coordinates.add(new Coordinate(x, y));
            }
        }

        // Add the first coordinate again to close the polygon
        coordinates.add(coordinates.get(0));

        return coordinates;
    }

    public static double convertTo3(double value) {
        DecimalFormat df = new DecimalFormat("#0.000");
        String formattedValue = df.format(value);
        return Double.parseDouble(formattedValue);
    }

    public static Polygon createPolygon(List<Coordinate> coordinates) {
        GeometryFactory factory = new GeometryFactory();
        LinearRing shell = factory.createLinearRing(coordinates.toArray(new Coordinate[0]));
        Polygon polygon = factory.createPolygon(shell, null); // No holes in this example

        return polygon;
    }
}
