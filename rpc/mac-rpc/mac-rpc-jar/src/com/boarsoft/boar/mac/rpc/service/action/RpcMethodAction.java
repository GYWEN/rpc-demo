package com.boarsoft.boar.mac.rpc.service.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boarsoft.bean.ReplyInfo;
import com.boarsoft.common.dao.PagedResult;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcReferenceConfig;
import com.boarsoft.rpc.bean.RpcRegistry;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.core.RpcContext;

@Component
@Scope("prototype")
@RestController
@RequestMapping("/method")
public class RpcMethodAction {
	@Autowired
	protected RpcContext rpcContext;

	protected String id;

	protected String key;
	
	protected String addr;
	
	protected int type = 0; // provider / consumer

	@RequestMapping("/list")
	// @Authorized(code = "rpc.method.list")
	public ReplyInfo<Object> list(int pageNo, int pageSize) {
		RpcRegistry rr = rpcContext.getRegistry(addr);
		RpcServiceConfig sc = rr.getServiceMap().get(key);
		RpcReferenceConfig rc = rr.getReferenceMap().get(key);
		List<Map<String, Object>> rmLt = new ArrayList<Map<String, Object>>();
		Map<String, RpcMethodConfig> mcMap = //
				(type == 0? sc.getMethodConfigMap(): rc.getMethodConfigMap());
		for (RpcMethodConfig mc: mcMap.values()) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("callback", mc.getCallback());
			m.put("failover", mc.getFailover());
			m.put("key", mc.getKey());
			m.put("mocker", mc.getMocker());
			m.put("protocol", mc.getProtocol());
			m.put("relativeId", mc.getRelativeId());
			m.put("sign", mc.getSign());
			m.put("timeout", mc.getTimeout());
			m.put("type", mc.getType());
			rmLt.add(m);
		}
		PagedResult<Map<String, Object>> pr = //
				new PagedResult<Map<String, Object>>(rmLt.size(), rmLt, pageNo, pageSize);
		return new ReplyInfo<Object>(true, pr);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
