package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.api.system.domain.Client;
import com.jnet.system.service.ClientService;
import com.jnet.system.mapper.ClientMapper;
import org.springframework.stereotype.Service;

/**
* @author 86186
* @description 针对表【sys_client】的数据库操作Service实现
* @createDate 2024-07-24 14:17:01
*/
@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client>
    implements ClientService{

}




