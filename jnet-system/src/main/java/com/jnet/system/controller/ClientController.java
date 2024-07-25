package com.jnet.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.R;
import com.jnet.api.system.domain.Client;
import com.jnet.system.service.ClientService;
import com.jnet.api.system.vo.ClientQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/24 14:09:02
 */
@Slf4j
@RestController()
@RequestMapping("/v1/client")
public class ClientController {
    
    @Resource
    private ClientService clientService;
    @PostMapping("/addOrUpdateClient")
    public R addOrUpdateClient(@RequestBody Client params) throws Exception{
        return R.success(clientService.saveOrUpdate(params));
    }

    @DeleteMapping("/deleteClientById")
    public R deleteClientById(@RequestParam("id") String clientId) throws Exception{
        boolean result = clientService.updateById(Client.builder().clientRegistrationId(clientId).enabled(false).build());
        return R.success(result);
    }

    @GetMapping("/getClientById")
    public R getClientById(@RequestParam("id") String  clientId) throws Exception{
        Client client = clientService.getById(clientId);
        return R.success(client);
    }

    @PostMapping("/pageClient")
    public R<Page<Client>> pageClient(@RequestBody ClientQuery<Client> query) throws Exception{
        LambdaQueryWrapper<Client> queryWrapper = Wrappers.lambdaQuery(query.getClient());
        Page<Client> page = clientService.page(query,queryWrapper);
        return R.success(page);
    }

    @PostMapping("/queryClient")
    public R<List<Client>> queryClient(@RequestBody ClientQuery<Client> query) throws Exception{
        LambdaQueryWrapper<Client> queryWrapper = Wrappers.lambdaQuery(query.getClient());
        List<Client> clients = clientService.list(queryWrapper);
        return R.success(clients);
    }
    
}
