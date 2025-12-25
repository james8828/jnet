package com.jnet.anno.service.impl;

import com.jnet.anno.constant.Constant;
import com.jnet.anno.domain.Measure;
import com.jnet.anno.mapper.MeasureMapper;
import com.jnet.anno.netty.websocket.NioWebSocketHandler;
import com.jnet.anno.service.MeasureService;
import com.jnet.anno.utils.MessageSource;
import com.jnet.anno.utils.measure.MeasureMessageGenerator;
import com.jnet.anno.vo.measure.MeasureVo;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.api.R;
import com.jnet.api.system.domain.User;
import com.jnet.common.core.utils.SecurityUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 测量标注管理
 * @author mugw
 * @version 1.0
 * @since 2025/5/21 14:33:43
 */
@Service
public class MeasureServiceImpl extends ServiceImpl<MeasureMapper, Measure>
    implements MeasureService{

    @Resource
    private NioWebSocketHandler webSocketHandler;
    @Resource
    private HttpServletResponse response;
//    @Resource
//    private RemoteUserService remoteUserService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public R<Measure> addMeasure(Measure req) throws Exception {

        if (req == null||req.getGeometry()==null||!req.getGeometry().isSimple()){
            throw new Exception(MessageSource.M("ARGUMENT_INVALID"));
        }

        req.setCreateBy(SecurityUtils.getUserId());
        req.setAnnotationType("Measure");
        long number = 1L;
        Measure measure = getOne(Wrappers.<Measure>lambdaQuery().eq(Measure::getSlideId, req.getSlideId()).eq(Measure::getMeasureName, req.getMeasureName())
                .orderBy(false,false,Measure::getMeasureId).last("limit 1"));
        if (measure != null && measure.getNumber() != null) {
            number += measure.getNumber();
        }
        String measureFullName = req.getMeasureName() + number;
        req.setMeasureFullName(measureFullName);
        req.setNumber(number);
        baseMapper.insert(req);
        webSocketHandler.sendMessage(MeasureMessageGenerator.generateAnnotationMessage(req, Constant.ANNO_ACTION_ADD));
        return R.success(req);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public R delete(Long measureId) throws Exception {
        if (!Optional.ofNullable(measureId).isPresent()) {
            throw new Exception(MessageSource.M("ARGUMENT_INVALID"));
        }
        Measure measure = baseMapper.selectById(measureId);
        if (!Optional.ofNullable(measure).isPresent()) {
            throw new Exception(MessageSource.M("NO_ANNOTATION_DATA"));
        }
        baseMapper.deleteById(measureId);
        webSocketHandler.sendMessage(MeasureMessageGenerator.generateAnnotationMessage(measure, Constant.ANNO_ACTION_ADD));
        return R.success(null, MessageSource.M("OPERATE_SUCCEED"));
    }

    @Override
    public void export(Long slideId) throws Exception {
        List<Measure> measureList = list(Wrappers.<Measure>lambdaQuery().eq(Measure::getSlideId, slideId)
                .ne(Measure::getLocationType, Geometry.TYPENAME_POINT));
        List<MeasureVo> measureVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(measureList)){
            measureVoList = measureList.stream().map(MeasureVo::convert).map(this::renderUser).collect(Collectors.toList());
        }
        long points = count(Wrappers.<Measure>lambdaQuery().eq(Measure::getSlideId, slideId)
                .eq(Measure::getLocationType, Geometry.TYPENAME_POINT));
        if (points > 0){
            measureVoList.add(MeasureVo.builder().pointCount(points).measureFullName("P").build());
        }
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String exportName = URLEncoder.encode(MessageSource.M("EXCEL_TITLE"), "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + exportName + ".xlsx");
        // 使用EasyExcel写入数据
        EasyExcel.write(response.getOutputStream(), MeasureVo.class).sheet(exportName).doWrite(measureVoList);
    }

    private MeasureVo renderUser(MeasureVo measureVo){
        if (measureVo == null){
            return null;
        }
        /*R<List<User>> r = remoteUserService.query(new User());
        List<User> users = r == null ? null : r.getData();
        Map<Long, User> userMap = users == null ? new HashMap<>() : users.stream()
                .filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(
                        User::getUserId,
                        user -> user,
                        (existing, replacement) -> existing // 遇到重复 key 保留第一个
                ));*/

        Map<Long, User> userMap = new HashMap<>();
        Long createBy = measureVo.getCreateBy();
        if (createBy == null){
            return measureVo;
        }
        if (userMap == null){
            return measureVo;
        }
        String userName = userMap.get(createBy).getNickName();
        measureVo.setCreateUserName(userName);
        return measureVo;
    }
}




