package com.jnet.anno.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.anno.biz.dto.GeoSchoolSearchParModel;
import com.jnet.anno.biz.entity.GeoSchool;
import com.jnet.anno.biz.mapper.GeoSchoolMapper;
import com.jnet.anno.biz.service.IGeoSchoolService;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Magic1412
 * @since 2021-01-11
 */
@Service
public class GeoSchoolServiceImpl extends ServiceImpl<GeoSchoolMapper, GeoSchool> implements IGeoSchoolService {

        @Autowired
        private GeoSchoolMapper mapper;

        @Override
        public IPage<GeoSchool> getGeoSchoolByParams(GeoSchoolSearchParModel params) {
            QueryWrapper<GeoSchool> queryWrapper = new QueryWrapper<GeoSchool>();
            Page<GeoSchool> pageObj = new Page<>(params.getPage(), params.getSize());
            IPage<GeoSchool> iPage = mapper.selectPage(pageObj,queryWrapper);
            return iPage;
        }

    @Override
    public List<Map> getGeoById(Long fid) {
        return mapper.getGeoById(fid);
        //return null;
    }

    @Override
    public List<Map> getGeoStatistic() {
        return mapper.getGeoStatistic();
        //return null;
    }

    @Override
    public List<Map> getGeoContainsPoint(Point point) {
        return mapper.getGeoContainsPoint(point);
        //return null;
    }
}
