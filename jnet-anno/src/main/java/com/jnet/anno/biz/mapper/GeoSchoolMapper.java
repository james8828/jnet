package com.jnet.anno.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.anno.biz.entity.GeoSchool;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Magic1412
 * @since 2021-01-11
 */
public interface GeoSchoolMapper extends BaseMapper<GeoSchool> {
    List<Map> getGeoById(Long fid);
    List<Map> getGeoContainsPoint(Point point);
    List<Map> getGeoStatistic();

}
