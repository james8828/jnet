package com.jnet.anno.biz.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnet.anno.biz.domain.Anno;
import com.jnet.anno.biz.service.AnnoService;
import com.jnet.anno.biz.mapper.AnnoMapper;
import com.jnet.anno.common.datatype.jts.parsers.PolygonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
* @author 86186
* @description 针对表【anno】的数据库操作Service实现
* @createDate 2024-02-01 16:36:51
*/
@Slf4j
@Service
public class AnnoServiceImpl extends ServiceImpl<AnnoMapper, Anno>
    implements AnnoService{

    private static Snowflake snowflake = IdUtil.getSnowflake();

    @Override
    public void uploadAnnoFile(MultipartFile jsonFile) throws Exception {
        /*InputStream inputStream = jsonFile.getInputStream();
        //创建文件夹
        File dir = new File(".."+File.separator+"temp");
        if(!dir.exists()) {
            dir.mkdirs();
        }
        File file = FileUtil.file(dir+File.separator+snowflake.nextIdStr(),snowflake.nextIdStr());
        log.info("anno json file:{}",file);*/
        File file = FileUtil.file("D:\\work\\dict\\jnet\\schemas\\ST_AsGeoJSON_contour40000__text2.json");
        //FileOutputStream out = new FileOutputStream(file);
        try {
            /*byte[] bytes = IOUtils.toByteArray(inputStream);
            out.write(bytes);*/
            //List<String> annoList = FileUtils.readLines(file);
            List<Anno> annoList = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(file);
            GeometryFactory geometryFactory = new GeometryFactory();
            PolygonParser polygonParser = new PolygonParser(geometryFactory);
            for(int i=0;i<node.size();i++){
                JsonNode jsonNode = node.get(i);
                String annoStr = jsonNode.get("st_asgeojson").asText();
                Polygon polygon = polygonParser.geometryFromJson(objectMapper.readTree(annoStr));
                Anno anno = Anno.builder().userId("anno").name("anno").geometry(polygon).build();
                annoList.add(anno);
            }
            saveBatch(annoList);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //IOUtils.close(out);
        }

    }
}




