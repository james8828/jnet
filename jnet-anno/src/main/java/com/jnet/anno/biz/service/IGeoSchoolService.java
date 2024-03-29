package com.jnet.anno.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.anno.biz.dto.GeoSchoolSearchParModel;
import com.jnet.anno.biz.entity.GeoSchool;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Magic1412
 * @since 2021-01-11
 */
public interface IGeoSchoolService extends IService<GeoSchool> {
    IPage<GeoSchool> getGeoSchoolByParams(GeoSchoolSearchParModel params);
    List<Map> getGeoById(Long fid);
    List<Map> getGeoStatistic();
    List<Map> getGeoContainsPoint(Point point);
}
