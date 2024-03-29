package com.jnet.anno.utils;

import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Driver;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;

import java.io.IOException;
/**
 * @author: Magic1412
 * @description: GDAL工具
 * @date: 2021/1/11 12:35
 */
public class GDALUtils {

    /**
     * @description: 转换工具
     * @param inPath 输入路径
     * @param outPath 输出路径
     * @param outFormat 输出格式
     * @return null
     */
    public static void geoTransfer(String inPath,String outPath,String outFormat) throws IOException, InterruptedException {

        ogr.RegisterAll();
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8","YES");
        gdal.SetConfigOption("SHAPE_ENCODING","");
        //输入
        DataSource ds = ogr.Open(inPath,0);
        if (ds == null)
        {
            System.out.println("打开文件失败！" );
            return;
        }
        System.out.println("打开文件成功！" );
        Layer oLayer = ds.GetLayerByIndex(0);
        if(oLayer == null){
            System.out.println("获取失败");
            return;
        }
        //输出
        Driver dv;
        switch(outFormat){
            case "SHP":
                dv = ogr.GetDriverByName("ESRI Shapefile");
                break;
            default:
                dv = ogr.GetDriverByName("GeoJSON");
        }
        DataSource dataSource = dv.CreateDataSource(outPath);
        dataSource.CopyLayer(oLayer,"out");
        dataSource.delete();
    }

}
