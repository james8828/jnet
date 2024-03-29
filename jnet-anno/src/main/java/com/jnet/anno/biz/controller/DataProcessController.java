package com.jnet.anno.biz.controller;


import org.springframework.web.bind.annotation.*;

@CrossOrigin
/*
@Api(tags = "tools")
*/
@RestController
@RequestMapping("/biz/data-process")
public class DataProcessController {

    /*@PostMapping("/dxfToGeoJSON")
    public ApiResult<Object> dxfToGeoJSON(@RequestParam(value = "file", required = true) MultipartFile file) throws IOException, InterruptedException {
        Path fileStorageLocation = Paths.get(fileProperties.getStoreFileUrl()).toAbsolutePath().normalize();
        Path fileOutLocation = Paths.get(fileProperties.getOutFileUrl()).toAbsolutePath().normalize();
        String name =  GISFileUtils.storeFile(file, fileStorageLocation);
        String inPath = fileStorageLocation.resolve(name).toString();
        String outPath = fileOutLocation.resolve( BaseUtil.trimFileSuffix(name)+ ".json").toString();
        GDALUtils.geoTransfer(inPath,outPath,"GeoJSON");
        return ResultUtils.Success(JSONObject.parse(GISFileUtils.readFileContent(outPath)));
    }
*/
    /*@PostMapping("/shpToGeoJSON")
    public ApiResult<Object> shpToGeoJSON(@RequestParam(value = "file", required = true) MultipartFile file) throws Exception {
        if(!BaseUtil.getFileSuffix(file.getOriginalFilename()).equals("zip")) return ResultUtils.Success("上传格式错误，清上传zip");
        Path fileOutLocation = Paths.get(fileProperties.getOutFileUrl()).toAbsolutePath().normalize();
        Path fileStorageLocation = Paths.get(fileProperties.getStoreFileUrl()).toAbsolutePath().normalize();
        String name =  GISFileUtils.storeFile(file, fileStorageLocation);
        String zipPath = fileStorageLocation.resolve(name).toString();
        String shpName = ZipUtils.getShpZipContentName(zipPath);
        ZipUtils.ZipUncompress(zipPath,fileStorageLocation.toString());
        String outPath = fileOutLocation.resolve( BaseUtil.trimFileSuffix(name)+ ".json").toString();
        GDALUtils.geoTransfer(fileStorageLocation.resolve(shpName + ".shp").toString(),outPath,"GeoJSON");
        String res  = GISFileUtils.readFileContent(outPath);
        return ResultUtils.Success(JSONObject.parse(res));
    }*/

}
