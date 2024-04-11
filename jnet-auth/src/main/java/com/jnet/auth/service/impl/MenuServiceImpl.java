package com.jnet.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.auth.domain.Menu;
import com.jnet.auth.mapper.MenuMapper;
import com.jnet.auth.service.MenuService;
import org.springframework.stereotype.Service;

/**
* @author 86186
* @description 针对表【sys_menu】的数据库操作Service实现
* @createDate 2024-04-09 15:44:43
*/
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu>
    implements MenuService {

}




