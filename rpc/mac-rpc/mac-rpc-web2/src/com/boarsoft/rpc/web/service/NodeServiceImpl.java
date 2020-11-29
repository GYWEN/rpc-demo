package com.boarsoft.rpc.web.service;

import org.springframework.stereotype.Component;

import com.boarsoft.bean.ReplyInfo;

@Component("nodeService")
public class NodeServiceImpl implements NodeService {
	@Override
	public ReplyInfo<String> list(){
		return new ReplyInfo<String>(true, "hello");
	}
}
